package com.example.conferencebooking.service;

import com.example.conferencebooking.entity.User;
import com.example.conferencebooking.entity.Booking;
import java.util.List;
import java.util.Optional;

public interface UserService {
    User createUser(User user);
    User getUser(String id);
    User getUserByEmail(String email);
    List<User> getAllUsers();
    void deleteUser(String id);
    Optional<User> findByEmail(String email);
    User validateLogin(String email, String password);
    User updateUser(User user);
    List<Booking> getUserBookings(String userId);
} 