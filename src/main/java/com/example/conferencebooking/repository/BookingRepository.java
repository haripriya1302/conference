package com.example.conferencebooking.repository;

import com.example.conferencebooking.entity.Booking;
import com.example.conferencebooking.entity.BookingStatus;
import java.util.List;
import java.util.Optional;

public interface BookingRepository {
    Booking save(Booking booking);
    Optional<Booking> findById(String id);
    List<Booking> findByConferenceId(String conferenceId);
    List<Booking> findByUserId(String userId);
    void deleteById(String id);
    List<Booking> findByConferenceIdAndStatus(String conferenceId, BookingStatus status);
    List<Booking> findByStatus(BookingStatus status);
} 