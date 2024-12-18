package com.example.conferencebooking.service;

import com.example.conferencebooking.entity.Booking;
import java.util.List;
import com.example.conferencebooking.dto.BookingStatusDTO;

public interface BookingService {
    Booking createBooking(String userId, String conferenceId);
    Booking getBooking(String id);
    List<Booking> getUserBookings(String userId);
    void cancelBooking(String id);
    List<Booking> getConferenceBookings(String conferenceId);
    Booking confirmWaitlistBooking(String bookingId);
    BookingStatusDTO getBookingStatus(String bookingId);
} 