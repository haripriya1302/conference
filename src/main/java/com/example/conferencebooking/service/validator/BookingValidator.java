package com.example.conferencebooking.service.validator;

import com.example.conferencebooking.entity.Booking;
import com.example.conferencebooking.entity.Conference;
import com.example.conferencebooking.entity.User;
import com.example.conferencebooking.entity.BookingStatus;
import com.example.conferencebooking.exception.ValidationException;
import com.example.conferencebooking.repository.BookingRepository;
import com.example.conferencebooking.service.ConferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BookingValidator {
    private final BookingRepository bookingRepository;
    private final ConferenceService conferenceService;

    public void validateBooking(User user, Conference conference) {
        List<String> errors = new ArrayList<>();

        if (conference.getStartTimestamp().isBefore(LocalDateTime.now())) {
            errors.add("Cannot book a conference that has already started");
        }

        // Check if user already has an active booking for this conference
        List<Booking> existingBookings = bookingRepository.findByUserId(user.getId())
                .stream()
                .filter(b -> b.getConferenceId().equals(conference.getId()))
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || 
                            b.getStatus() == BookingStatus.WAITLISTED)
                .toList();

        if (!existingBookings.isEmpty()) {
            errors.add("You already have an active booking for this conference");
        }

        // Check for booking conflicts
        for (Booking existingBooking : existingBookings) {
            Conference existingConference = conferenceService.getConference(existingBooking.getConferenceId());
            
            if (hasTimeOverlap(conference, existingConference)) {
                errors.add("This booking conflicts with another conference you've booked");
                break;
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Booking validation failed", errors);
        }
    }

    private boolean hasTimeOverlap(Conference conf1, Conference conf2) {
        return !conf1.getEndTimestamp().isBefore(conf2.getStartTimestamp()) &&
               !conf2.getEndTimestamp().isBefore(conf1.getStartTimestamp());
    }
} 