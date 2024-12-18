package com.example.conferencebooking.repository;

import com.example.conferencebooking.entity.User;
import com.example.conferencebooking.entity.Booking;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(String id);
    Optional<User> findByEmail(String email);
    List<User> findAll();
    void deleteById(String id);
    List<Booking> findUserBookings(String userId);
} 