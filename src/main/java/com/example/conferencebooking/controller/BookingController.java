package com.example.conferencebooking.controller;

import com.example.conferencebooking.entity.Booking;
import com.example.conferencebooking.service.BookingService;
import com.example.conferencebooking.service.ConferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import com.example.conferencebooking.entity.User;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import com.example.conferencebooking.exception.ValidationException;
import com.example.conferencebooking.dto.BookingConfirmationResponse;
import com.example.conferencebooking.entity.Conference;
import java.time.LocalDateTime;
import com.example.conferencebooking.exception.BookingNotFoundException;
import com.example.conferencebooking.entity.BookingStatus;

@Slf4j
@Controller
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;
    private final ConferenceService conferenceService;

    // View endpoints
    @GetMapping("/bookings")
    public String listBookings(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/";
        }
        
        List<Booking> userBookings = bookingService.getUserBookings(user.getId());
        model.addAttribute("userBookings", userBookings);
        return "bookings/list";
    }

    // REST API endpoints
    @PostMapping("/api/bookings")
    @ResponseBody
    public ResponseEntity<?> createBooking(@RequestParam String conferenceId, HttpSession session) {
        try {
            User user = (User) session.getAttribute("user");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Please login to book a conference"));
            }
            
            Conference conference = conferenceService.getConference(conferenceId);
            if (LocalDateTime.now().isAfter(conference.getEndTimestamp())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Cannot book - Conference has ended"));
            }
            
            log.info("Attempting to create booking for user {} and conference {}", user.getId(), conferenceId);
            Booking booking = bookingService.createBooking(user.getId(), conferenceId);
            log.info("Successfully created booking: {}", booking);
            return ResponseEntity.ok(booking);
        } catch (ValidationException e) {
            log.error("Validation error during booking: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create booking for conference {}: {}", conferenceId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create booking: " + e.getMessage()));
        }
    }

    @GetMapping("/api/bookings/{id}")
    @ResponseBody
    public ResponseEntity<Booking> getBooking(@PathVariable String id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Booking booking = bookingService.getBooking(id);
        // Only return if booking belongs to user
        if (!booking.getUserId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(booking);
    }

    @DeleteMapping("/api/bookings/cancel/{id}")
    @ResponseBody
    public ResponseEntity<?> cancelBooking(@PathVariable String id, HttpSession session) {
        try {
            User user = (User) session.getAttribute("user");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Please login to cancel booking"));
            }
            
            Booking booking = bookingService.getBooking(id);
            Conference conference = conferenceService.getConference(booking.getConferenceId());

            if (LocalDateTime.now().isAfter(conference.getStartTimestamp())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Cannot cancel booking after conference has started"));
            }

            if (booking.getStatus() == BookingStatus.CANCELLED) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Booking is already cancelled"));
            }

            bookingService.cancelBooking(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Booking cancelled successfully"
            ));

        } catch (BookingNotFoundException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error cancelling booking {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/bookings/{id}/confirm")
    @ResponseBody
    public ResponseEntity<BookingConfirmationResponse> confirmBooking(@PathVariable String id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BookingConfirmationResponse.builder()
                    .success(false)
                    .message("Please login to confirm booking")
                    .bookingId(id)
                    .build());
        }

        try {
            Booking booking = bookingService.getBooking(id);
            if (!booking.getUserId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BookingConfirmationResponse.builder()
                        .success(false)
                        .message("Not authorized to confirm this booking")
                        .bookingId(id)
                        .build());
            }

            Booking confirmedBooking = bookingService.confirmWaitlistBooking(id);
            return ResponseEntity.ok(BookingConfirmationResponse.builder()
                .success(true)
                .message("Booking confirmed successfully")
                .status(confirmedBooking.getStatus())
                .bookingId(confirmedBooking.getId())
                .build());

        } catch (ValidationException e) {
            return ResponseEntity.badRequest()
                .body(BookingConfirmationResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .status(bookingService.getBooking(id).getStatus())
                    .bookingId(id)
                    .reason("Validation failed")
                    .build());
        } catch (Exception e) {
            log.error("Error confirming booking {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(BookingConfirmationResponse.builder()
                    .success(false)
                    .message("Failed to confirm booking")
                    .bookingId(id)
                    .build());
        }
    }

    @GetMapping("/api/bookings/status/{conferenceId}")
    @ResponseBody
    public ResponseEntity<?> getBookingStatusForConference(@PathVariable String conferenceId, HttpSession session) {
        try {
            User user = (User) session.getAttribute("user");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Please login to check booking status"));
            }
            
            List<Booking> userBookings = bookingService.getUserBookings(user.getId());
            BookingStatus status = userBookings.stream()
                .filter(booking -> booking.getConferenceId().equals(conferenceId))
                .map(Booking::getStatus)
                .filter(s -> s == BookingStatus.WAITLISTED || s == BookingStatus.PROCESSING || s == BookingStatus.CONFIRMED)
                .findFirst()
                .orElse(null);
            
            return ResponseEntity.ok(Map.of("status", status));
        } catch (Exception e) {
            log.error("Error checking booking status for conference {}: {}", conferenceId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to check booking status"));
        }
    }
} 