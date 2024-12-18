package com.example.conferencebooking.service.impl;

import com.example.conferencebooking.entity.Booking;
import com.example.conferencebooking.entity.User;
import com.example.conferencebooking.exception.UserNotFoundException;
import com.example.conferencebooking.repository.UserRepository;
import com.example.conferencebooking.service.UserService;
import com.example.conferencebooking.service.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserValidator userValidator;

    @Override
    public User createUser(User user) {
        userValidator.validateUser(user);
        
        if (user.getId() == null) {
            user = User.builder()
                    .id(UUID.randomUUID().toString())
                    .name(user.getName())
                    .email(user.getEmail())
                    .phoneNumber(user.getPhoneNumber())
                    .build();
        }
        return userRepository.save(user);
    }

    @Override
    public User getUser(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User validateLogin(String email, String password) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Invalid email or password"));
    }

    @Override
    public User updateUser(User user) {
        // Validate the user exists
        getUser(user.getId());
        
        // Validate the updated user data
        userValidator.validateUser(user);
        
        // Save and return the updated user
        return userRepository.save(user);
    }

    @Override
    public List<Booking> getUserBookings(String userId) {
        return userRepository.findUserBookings(userId);
    }
} 