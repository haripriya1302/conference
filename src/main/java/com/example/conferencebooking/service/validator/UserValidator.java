package com.example.conferencebooking.service.validator;

import com.example.conferencebooking.entity.User;
import com.example.conferencebooking.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class UserValidator {
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("^\\+?[1-9][0-9]{7,14}$");

    public void validateUser(User user) {
        List<String> errors = new ArrayList<>();

        if (user.getName() == null || user.getName().trim().isEmpty()) {
            errors.add("User name is required");
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            errors.add("Email is required");
        } else if (!EMAIL_PATTERN.matcher(user.getEmail()).matches()) {
            errors.add("Invalid email format");
        }

        if (user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty() 
            && !PHONE_PATTERN.matcher(user.getPhoneNumber()).matches()) {
            errors.add("Invalid phone number format");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("User validation failed", errors);
        }
    }
} 