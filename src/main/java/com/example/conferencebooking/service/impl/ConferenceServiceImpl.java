package com.example.conferencebooking.service.impl;

import com.example.conferencebooking.entity.Conference;
import com.example.conferencebooking.dto.ConferenceSearchDTO;
import com.example.conferencebooking.exception.ConferenceNotFoundException;
import com.example.conferencebooking.repository.ConferenceRepository;
import com.example.conferencebooking.service.ConferenceService;
import com.example.conferencebooking.service.BookingService;
import com.example.conferencebooking.entity.Booking;
import com.example.conferencebooking.service.validator.ConferenceValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.*;

import com.example.conferencebooking.entity.BookingStatus;
@Service
public class ConferenceServiceImpl implements ConferenceService {
    private final ConferenceRepository conferenceRepository;
    private final ConferenceValidator conferenceValidator;
    private final BookingService bookingService;

    @Autowired
    public ConferenceServiceImpl(
            ConferenceRepository conferenceRepository,
            ConferenceValidator conferenceValidator,
            @Lazy BookingService bookingService) {
        this.conferenceRepository = conferenceRepository;
        this.conferenceValidator = conferenceValidator;
        this.bookingService = bookingService;
    }

    @Override
    public Conference createConference(Conference conference) {
        System.out.println("Creating conference: " + conference);
        if (conference.getId() == null) {
            conference = Conference.builder()
                    .id(UUID.randomUUID().toString())
                    .name(conference.getName())
                    .location(conference.getLocation())
                    .topics(conference.getTopics())
                    .startTimestamp(conference.getStartTimestamp())
                    .endTimestamp(conference.getEndTimestamp())
                    .totalSlots(conference.getTotalSlots())
                    .availableSlots(conference.getTotalSlots())
                    .build();
        }
        
        conferenceValidator.validate(conference);
        return conferenceRepository.save(conference);
    }

    @Override
    public Conference getConference(String id) {
        return conferenceRepository.findById(id)
                .orElseThrow(() -> new ConferenceNotFoundException("Conference not found with id: " + id));
    }

    @Override
    public List<Conference> getAllConferences() {
        return conferenceRepository.findAll();
    }

    @Override
    public void deleteConference(String id) {
        conferenceRepository.deleteById(id);
    }

    @Override
    public List<Conference> searchConferences(ConferenceSearchDTO searchDTO) {
        List<Conference> allConferences = conferenceRepository.findAll();
        
        // Get user's bookings to check for already booked conferences
        List<Booking> userBookings = bookingService.getUserBookings(searchDTO.getUserId());
        
        // Create a map of conference IDs to their booking status
        Map<String, BookingStatus> conferenceBookingStatus = userBookings.stream()
            .collect(Collectors.toMap(
                Booking::getConferenceId,
                Booking::getStatus,
                (existing, replacement) -> existing
            ));
        
        // Filter and process conferences
        List<Conference> results = allConferences.stream()
            .filter(conference -> matchesSearchCriteria(conference, searchDTO))
            .peek(conference -> {
                BookingStatus status = conferenceBookingStatus.get(conference.getId());
                conference.setHasActiveBooking(status != null && status != BookingStatus.CANCELLED);
                // Add the specific booking status to the conference object
                if (status != null) {
                    conference.setBookingStatus(status);
                }
            })
            .sorted(
                // First sort by booking status (not booked first)
                Comparator.comparing((Conference c) -> conferenceBookingStatus.containsKey(c.getId()))
                // Then by the specified timeframe criteria
                .thenComparing(getConferenceComparator(searchDTO.getTimeframe()))
            )
            .collect(Collectors.toList());
        
        return results;
    }

    @Override
    public List<Conference> getSuggestedConferences(String userId) {
        // Get user's booking history
        List<Booking> userBookings = bookingService.getUserBookings(userId);
        
        // Extract topics from user's previous bookings
        Set<String> userInterests = userBookings.stream()
            .map(booking -> conferenceRepository.findById(booking.getConferenceId()).orElse(null))
            .filter(Objects::nonNull)
            .map(Conference::getName)
            .collect(Collectors.toSet());
        
        // Get all upcoming conferences
        List<Conference> upcomingConferences = conferenceRepository.findAll().stream()
            .filter(conference -> conference.getStartTimestamp().isAfter(LocalDateTime.now()))
            .collect(Collectors.toList());
        
        // Score and rank conferences based on user interests
        return upcomingConferences.stream()
            .map(conference -> new ConferenceScore(conference, calculateScore(conference, userInterests)))
            .sorted(Comparator.comparing(ConferenceScore::getScore).reversed())
            .limit(10)
            .map(ConferenceScore::getConference)
            .collect(Collectors.toList());
    }

    @Override
    public List<Conference> getUpcomingConferences() {
        return conferenceRepository.findAll().stream()
                .filter(conference -> conference.getStartTimestamp().isAfter(LocalDateTime.now()))
                .sorted(Comparator.comparing(Conference::getStartTimestamp))
                .collect(Collectors.toList());
    }

    private boolean matchesSearchCriteria(Conference conference, ConferenceSearchDTO searchDTO) {
        // Match name
        if (searchDTO.getName() != null && !searchDTO.getName().isEmpty() &&
            !conference.getName().toLowerCase().contains(searchDTO.getName().toLowerCase())) {
            return false;
        }

        // Match location
        if (searchDTO.getLocation() != null && !searchDTO.getLocation().isEmpty() &&
            !conference.getLocation().toLowerCase().contains(searchDTO.getLocation().toLowerCase())) {
            return false;
        }

        // Match topics
        if (searchDTO.getTopics() != null && !searchDTO.getTopics().isEmpty() &&
            !conference.getTopics().stream().anyMatch(topic -> 
                searchDTO.getTopics().stream().anyMatch(searchTopic -> 
                    topic.toLowerCase().contains(searchTopic.toLowerCase())))) {
            return false;
        }

        // Match date range
        if (searchDTO.getStartDate() != null && conference.getStartTimestamp().isBefore(searchDTO.getStartDate())) {
            return false;
        }
        if (searchDTO.getEndDate() != null && conference.getEndTimestamp().isAfter(searchDTO.getEndDate())) {
            return false;
        }

        // Match timeframe
        LocalDateTime now = LocalDateTime.now();
        return switch (searchDTO.getTimeframe()) {
            case "upcoming" -> conference.getStartTimestamp().isAfter(now);
            case "past" -> conference.getEndTimestamp().isBefore(now);
            default -> true;
        };
    }

    private Comparator<Conference> getConferenceComparator(String timeframe) {
        return switch (timeframe) {
            case "upcoming" -> Comparator.comparing(Conference::getStartTimestamp);
            case "past" -> Comparator.comparing(Conference::getEndTimestamp).reversed();
            default -> Comparator.comparing(Conference::getStartTimestamp);
        };
    }

    private double calculateScore(Conference conference, Set<String> userInterests) {
        if (userInterests.isEmpty()) {
            // If no user interests, score based on time and availability
            return scoreByTimeAndAvailability(conference);
        }

        double topicScore = calculateTopicScore(conference, userInterests);
        double timeScore = scoreByTimeAndAvailability(conference);
        
        // Weighted combination of scores
        return (0.7 * topicScore) + (0.3 * timeScore);
    }

    private double calculateTopicScore(Conference conference, Set<String> userInterests) {
        long matchingTopics = conference.getTopics().stream()
            .filter(topic -> userInterests.stream()
                .anyMatch(interest -> topic.toLowerCase().contains(interest.toLowerCase())))
            .count();
        
        return (double) matchingTopics / conference.getTopics().size();
    }

    private double scoreByTimeAndAvailability(Conference conference) {
        LocalDateTime now = LocalDateTime.now();
        long daysUntilStart = java.time.Duration.between(now, conference.getStartTimestamp()).toDays();
        
        // Score conferences starting soon higher, but not too soon
        double timeScore = 1.0 / (1.0 + Math.abs(daysUntilStart - 14)); // Optimal time is 2 weeks away
        
        // Factor in available slots
        double availabilityScore = (double) conference.getAvailableSlots() / conference.getTotalSlots();
        
        return (timeScore + availabilityScore) / 2;
    }

    private static class ConferenceScore {
        private final Conference conference;
        private final double score;

        public ConferenceScore(Conference conference, double score) {
            this.conference = conference;
            this.score = score;
        }

        public Conference getConference() {
            return conference;
        }

        public double getScore() {
            return score;
        }
    }
} 