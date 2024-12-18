package com.example.conferencebooking.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.conferencebooking.dto.BookingStatusDTO;
import com.example.conferencebooking.entity.Booking;
import com.example.conferencebooking.entity.BookingStatus;
import com.example.conferencebooking.entity.Conference;
import com.example.conferencebooking.entity.User;
import com.example.conferencebooking.exception.BookingNotFoundException;
import com.example.conferencebooking.exception.ValidationException;
import com.example.conferencebooking.message.BookingMessage;
import com.example.conferencebooking.repository.BookingRepository;
import com.example.conferencebooking.service.BookingService;
import com.example.conferencebooking.service.ConferenceService;
import com.example.conferencebooking.service.UserService;
import com.example.conferencebooking.service.validator.BookingValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import jakarta.persistence.EntityNotFoundException;
import com.rabbitmq.client.Channel;
import org.springframework.messaging.handler.annotation.Payload;

@Slf4j
@Service
@RequiredArgsConstructor
@RabbitListener(queues = "${rabbitmq.queue.name}")
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final ConferenceService conferenceService;
    private final UserService userService;
    private final BookingValidator bookingValidator;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.queue.name}")
    private String queueName;

    private final Set<String> processedBookings = Collections.synchronizedSet(new HashSet<>());

    @Override
    @Transactional
    public Booking createBooking(String userId, String conferenceId) {
        try {
            User user = userService.getUser(userId);
            Conference conference = conferenceService.getConference(conferenceId);
            
            // Check if conference has started
            if (LocalDateTime.now().isAfter(conference.getStartTimestamp())) {
                throw new ValidationException("Cannot book after conference has started");
            }
            
            // Check for existing active booking
            List<Booking> userBookings = bookingRepository.findByUserId(userId);
            Optional<Booking> existingBooking = userBookings.stream()
                .filter(b -> b.getConferenceId().equals(conferenceId))
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .findFirst();
                
            if (existingBooking.isPresent()) {
                throw new ValidationException("Already have active booking with ID: " + existingBooking.get().getId());
            }
            
            // Validate booking
            bookingValidator.validateBooking(user, conference);
            
            // Create the booking
            Booking booking = Booking.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .conferenceId(conferenceId)
                .bookingTime(LocalDateTime.now())
                .status(conference.getAvailableSlots() > 0 ? BookingStatus.CONFIRMED : BookingStatus.WAITLISTED)
                .build();
            
            // Save to database
            booking = bookingRepository.save(booking);
            
            // If no available slots, add to waitlist
            if (conference.getAvailableSlots() <= 0) {
                log.info("No available slots for conference {}. Adding to waitlist.", conferenceId);
                addToWaitlistQueue(booking, conference);
                return booking;
            }
            
            // Update conference slots for confirmed booking
            conference.setAvailableSlots(conference.getAvailableSlots() - 1);
            conferenceService.createConference(conference);
            
            // Remove from other waitlists if confirmed
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                removeFromOverlappingWaitlists(userId, conference);
            }
            
            log.info("Successfully created booking: {}", booking);
            return booking;
        } catch (Exception e) {
            log.error("Error creating booking", e);
            throw e;
        }
    }

    private void removeFromOverlappingWaitlists(String userId, Conference bookedConference) {
        List<Conference> allConferences = conferenceService.getAllConferences();
        
        allConferences.stream()
            .filter(c -> !c.getId().equals(bookedConference.getId()))
            .filter(c -> isOverlapping(c, bookedConference))
            .forEach(c -> {
                if (c.getWaitlistQueueId() != null) {
                    removeUserFromWaitlist(userId, c);
                }
            });
    }

    private boolean isOverlapping(Conference c1, Conference c2) {
        return !c1.getEndTimestamp().isBefore(c2.getStartTimestamp()) && 
               !c2.getEndTimestamp().isBefore(c1.getStartTimestamp());
    }

    private void removeUserFromWaitlist(String userId, Conference conference) {
        // Get all messages from queue
        List<Message> messages = new ArrayList<>();
        Message message;
        while ((message = rabbitTemplate.receive(conference.getWaitlistQueueId())) != null) {
            try {
                // Extract booking ID from BookingMessage
                BookingMessage bookingMessage = (BookingMessage) rabbitTemplate.getMessageConverter()
                    .fromMessage(message);
                
                // Get the actual booking from database
                Booking booking = bookingRepository.findById(bookingMessage.getBookingId())
                    .orElse(null);
                    
                if (booking != null && !booking.getUserId().equals(userId)) {
                    messages.add(message);
                } else if (booking != null) {
                    // Cancel the booking in database
                    booking.setStatus(BookingStatus.CANCELLED);
                    bookingRepository.save(booking);
                }
            } catch (Exception e) {
                log.error("Error processing message: {}", e.getMessage());
                messages.add(message); // Keep message in queue if processing fails
            }
        }
        
        // Requeue messages that weren't for this user
        messages.forEach(m -> {
            try {
                BookingMessage bookingMessage = (BookingMessage) rabbitTemplate.getMessageConverter()
                    .fromMessage(m);
                rabbitTemplate.convertAndSend(
                    conference.getWaitlistQueueId(),
                    bookingMessage,
                    msg -> {
                        msg.getMessageProperties().setPriority(1);
                        return msg;
                    }
                );
            } catch (Exception e) {
                log.error("Error requeueing message: {}", e.getMessage());
            }
        });
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void autoCancelExpiredWaitlist() {
        List<Conference> startedConferences = conferenceService.getAllConferences().stream()
            .filter(c -> LocalDateTime.now().isAfter(c.getStartTimestamp()))
            .filter(c -> c.getWaitlistQueueId() != null)
            .toList();
            
        for (Conference conference : startedConferences) {
            // Cancel all waitlisted bookings
            List<Booking> waitlistedBookings = bookingRepository.findByConferenceIdAndStatus(
                conference.getId(), BookingStatus.WAITLISTED);
                
            waitlistedBookings.forEach(booking -> {
                booking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);
            });
                
            // Clear the queue
            while (rabbitTemplate.receive(conference.getWaitlistQueueId()) != null) {
                // Keep receiving until queue is empty
            }
                
            // Remove queue ID from conference
            conference.setWaitlistQueueId(null);
            conferenceService.createConference(conference);
        }
    }

    private void addToWaitlistQueue(Booking booking, Conference conference) {
        // Ensure the conference has a queue
        if (conference.getWaitlistQueueId() == null) {
            String queueId = "conference.waitlist." + conference.getId();
            conference.setWaitlistQueueId(queueId);
            conferenceService.createConference(conference);
            
            // Declare queue with FIFO properties
            rabbitTemplate.execute(channel -> {
                Map<String, Object> args = new HashMap<>();
                args.put("x-max-priority", 1);
                channel.queueDeclare(queueId, true, false, false, args);
                return null;
            });
        }

        String idempotencyKey = booking.getId() + "_" + booking.getBookingTime();
        
        // Check if this booking was already processed
        if (!processedBookings.add(idempotencyKey)) {
            log.info("Booking {} already in waitlist queue, skipping", booking.getId());
            return;
        }

        try {
            // Add to queue with idempotency key in message properties
            BookingMessage message = new BookingMessage(booking.getId());
            rabbitTemplate.convertAndSend(
                conference.getWaitlistQueueId(), 
                message,
                m -> {
                    m.getMessageProperties().setMessageId(idempotencyKey);
                    m.getMessageProperties().setTimestamp(
                        java.util.Date.from(booking.getBookingTime()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toInstant())
                    );
                    return m;
                }
            );
            
            log.info("Added booking {} to waitlist queue {} with idempotency key {}", 
                booking.getId(), conference.getWaitlistQueueId(), idempotencyKey);
        } catch (Exception e) {
            // Remove from processed set if sending fails
            processedBookings.remove(idempotencyKey);
            throw e;
        }
    }

    @Override
    @Transactional
    public void cancelBooking(String id) {
        try {
            Booking booking = getBooking(id);
            Conference conference = conferenceService.getConference(booking.getConferenceId());
            
            log.info("Cancelling booking {} for conference {}", id, conference.getId());

            // Only process waitlist if cancelling a confirmed booking
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                // Increment available slots
                conference.setAvailableSlots(conference.getAvailableSlots() + 1);
                conferenceService.createConference(conference);
                
                // Process next waitlisted booking if exists
                if (conference.getWaitlistQueueId() != null) {
                    log.info("Processing next waitlisted booking from queue: {}", conference.getWaitlistQueueId());
                    rabbitTemplate.execute(channel -> {
                        com.rabbitmq.client.GetResponse response = channel.basicGet(conference.getWaitlistQueueId(), false);
                        if (response != null) {
                            String messageBody = new String(response.getBody());
                            String waitlistedBookingId = messageBody.replaceAll("[{}\"]", "").split(":")[1].trim();
                            
                            try {
                                Booking waitlistedBooking = bookingRepository.findById(waitlistedBookingId)
                                    .orElseThrow(() -> new EntityNotFoundException("Waitlisted booking not found"));
                                
                                // Move waitlisted booking to processing state
                                waitlistedBooking.setStatus(BookingStatus.PROCESSING);
                                waitlistedBooking.setBookingTime(LocalDateTime.now());
                                bookingRepository.save(waitlistedBooking);
                                
                                // Acknowledge the message
                                channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
                                
                                log.info("Successfully moved booking {} from waitlist to processing", waitlistedBookingId);
                            } catch (Exception e) {
                                // Requeue the message if processing fails
                                channel.basicNack(response.getEnvelope().getDeliveryTag(), false, true);
                                log.error("Failed to process waitlisted booking: {}", e.getMessage());
                            }
                        } else {
                            log.info("No waitlisted bookings found in queue");
                        }
                        return null;
                    });
                }
            }

            // Cancel the current booking
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            log.info("Successfully cancelled booking {}", id);
            
        } catch (Exception e) {
            log.error("Error cancelling booking {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to cancel booking: " + e.getMessage(), e);
        }
    }

    private void processWaitlistQueue(Conference conference) {
        while (conference.getAvailableSlots() > 0) {
            Message message = rabbitTemplate.receive(conference.getWaitlistQueueId());
            if (message == null) {
                log.info("No more waitlisted bookings");
                break;
            }

            try {
                Booking waitlistedBooking = (Booking) rabbitTemplate.getMessageConverter()
                    .fromMessage(message);
                
                // Move to processing state
                log.info("Processing waitlisted booking: " + waitlistedBooking.getId());
                waitlistedBooking.setStatus(BookingStatus.PROCESSING);
                waitlistedBooking.setBookingTime(LocalDateTime.now());
                bookingRepository.save(waitlistedBooking);
                
                // Remove from queue without re-queueing
                rabbitTemplate.execute(channel -> {
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                    return null;
                });
                
                // Decrease free_slots
                conference.setAvailableSlots(conference.getAvailableSlots() - 1);
                conferenceService.createConference(conference);
                
                log.info("Booking {} moved to processing state. Free slots remaining: {}", 
                    waitlistedBooking.getId(), conference.getAvailableSlots());
            } catch (Exception e) {
                log.error("Error processing waitlist booking", e);
                break;
            }
        }
    }

    @Override
    public BookingStatusDTO getBookingStatus(String bookingId) {
        Booking booking = getBooking(bookingId);
        boolean canConfirm = false;
        LocalDateTime confirmUntil = null;
        
        if (booking.getStatus() == BookingStatus.PROCESSING) {
            canConfirm = true;
            confirmUntil = booking.getBookingTime().plusHours(1);
        }
        
        return BookingStatusDTO.builder()
            .status(booking.getStatus())
            .canConfirm(canConfirm)
            .confirmUntil(confirmUntil)
            .build();
    }

    // private Booking createConfirmedBooking(User user, Conference conference) {
    //     // Decrease available slots
    //     conference.setAvailableSlots(conference.getAvailableSlots() - 1);
    //     conferenceService.createConference(conference);
        
    //     Booking booking = Booking.builder()
    //             .id(UUID.randomUUID().toString())
    //             .userId(user.getId())
    //             .conferenceId(conference.getId())
    //             .bookingTime(LocalDateTime.now())
    //             .status(BookingStatus.CONFIRMED)
    //             .build();
                
    //     // Remove from other waitlists
    //     removeFromOverlappingWaitlists(user.getId(), conference);
                
    //     return bookingRepository.save(booking);
    // }

    @Override
    public Booking getBooking(String id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));
    }

    @Override
    public List<Booking> getUserBookings(String userId) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        // Fetch conference details for each booking
        for (Booking booking : bookings) {
            Conference conference = conferenceService.getConference(booking.getConferenceId());
            booking.setConference(conference);
        }
        return bookings;
    }

    @Override
    public List<Booking> getConferenceBookings(String conferenceId) {
        return bookingRepository.findByConferenceId(conferenceId);
    }

    @Override
    @Transactional
    public Booking confirmWaitlistBooking(String bookingId) {
        Booking booking = getBooking(bookingId);
        Conference conference = conferenceService.getConference(booking.getConferenceId());
        
        // Check if conference has started
        if (LocalDateTime.now().isAfter(conference.getStartTimestamp())) {
            throw new ValidationException("Cannot confirm booking after conference has started");
        }
        
        // Check if booking is in processing state
        if (booking.getStatus() != BookingStatus.PROCESSING) {
            throw new ValidationException("Booking is not in processing state");
        }
        
        // Check if confirmation time has expired
        if (booking.getBookingTime().plusHours(1).isBefore(LocalDateTime.now())) {
            // Move back to waitlist - Update DB first
            booking.setStatus(BookingStatus.WAITLISTED);
            booking.setBookingTime(LocalDateTime.now());
            booking = bookingRepository.save(booking); // Ensure we get the updated booking
            
            // Then add to queue with idempotency
            try {
                addToWaitlistQueue(booking, conference);
            } catch (Exception e) {
                log.error("Failed to add to waitlist queue, but status is updated: {}", e.getMessage());
                // Status is already updated in DB, so we'll still throw the validation exception
            }
            
            log.info("Moved expired booking {} back to waitlist", bookingId);
            throw new ValidationException("Booking moved back to waitlist due to expired confirmation time");
        }
        
        // Check if slots are still available
        if (conference.getAvailableSlots() <= 0) {
            throw new ValidationException("No available slots");
        }
        
        // Confirm booking
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookingTime(LocalDateTime.now());
        bookingRepository.save(booking);
        
        // Update conference slots
        conference.setAvailableSlots(conference.getAvailableSlots() - 1);
        conferenceService.createConference(conference);
        
        // Remove from other overlapping waitlists
        removeFromOverlappingWaitlists(booking.getUserId(), conference);
        
        return booking;
    }

    // Add method to toggle fully booked status
    @Transactional
    public void toggleFullyBooked(String conferenceId, boolean fullyBooked) {
        Conference conference = conferenceService.getConference(conferenceId);
        conference.setFullyBooked(fullyBooked);
        conferenceService.createConference(conference);
        
        // If setting to false and there are available slots, process waitlist
        if (!fullyBooked && conference.getAvailableSlots() > 0 
                && conference.getWaitlistQueueId() != null) {
            // Set free slots to available slots when turning off fully booked
            conference.setAvailableSlots(conference.getAvailableSlots());
            processWaitlistQueue(conference);
        }
    }

    @RabbitHandler
    public void handleWaitlistMessage(Message message, Channel channel, @Payload BookingMessage bookingMessage) {
        try {
            // Get the actual booking from database using the ID from message
            Booking booking = bookingRepository.findById(bookingMessage.getBookingId())
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
            
            // Process the booking
            if (booking.getStatus() == BookingStatus.WAITLISTED) {
                booking.setStatus(BookingStatus.PROCESSING);
                bookingRepository.save(booking);
            }
            
            // Acknowledge the message
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            try {
                // If processing fails, reject the message and requeue it
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
                log.error("Error processing waitlist message: {}", e.getMessage());
            } catch (IOException ioException) {
                log.error("Error handling message acknowledgment: {}", ioException.getMessage());
            }
        }
    }

    // When adding to waitlist
    // private Booking addToWaitlist(User user, Conference conference) {
    //     try {
    //         // Create new booking
    //         Booking booking = Booking.builder()
    //                 .id(UUID.randomUUID().toString())
    //                 .userId(user.getId())
    //                 .conferenceId(conference.getId())
    //                 .bookingTime(LocalDateTime.now())
    //                 .status(BookingStatus.WAITLISTED)
    //                 .build();
            
    //         // Save to database first
    //         booking = bookingRepository.save(booking);
            
    //         // Ensure queue exists
    //         if (conference.getWaitlistQueueId() == null) {
    //             String queueId = "conference.waitlist." + conference.getId();
    //             conference.setWaitlistQueueId(queueId);
    //             conferenceService.createConference(conference);
    //         }
            
    //         // Add to queue with idempotency
    //         addToWaitlistQueue(booking, conference);
            
    //         return booking;
    //     } catch (Exception e) {
    //         log.error("Error adding to waitlist", e);
    //         throw e;
    //     }
    // }

    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void checkProcessingBookingsExpiration() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> processingBookings = bookingRepository.findByStatus(BookingStatus.PROCESSING);
        
        for (Booking booking : processingBookings) {
            if (booking.getBookingTime().plusHours(1).isBefore(now)) {
                Conference conference = conferenceService.getConference(booking.getConferenceId());
                
                // Move back to waitlist
                booking.setStatus(BookingStatus.WAITLISTED);
                booking.setBookingTime(now);
                bookingRepository.save(booking);
                
                // Add to end of queue with idempotency
                addToWaitlistQueue(booking, conference);
                
                log.info("Moved expired processing booking {} back to waitlist", booking.getId());
            }
        }
    }
} 