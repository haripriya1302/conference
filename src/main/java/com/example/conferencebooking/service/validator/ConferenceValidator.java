package com.example.conferencebooking.service.validator;

import com.example.conferencebooking.entity.Conference;
import com.example.conferencebooking.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ConferenceValidator implements Validator<Conference> {
    
    // private static final long MAX_DURATION_HOURS = 12;
    // private static final int MAX_TOPICS = 10;
    // private static final String TOPIC_PATTERN = "^[a-zA-Z0-9\\s]+$";
    
    @Override
    public void validate(Conference conference) {
        List<String> errors = new ArrayList<>();

        if (conference.getName() == null || conference.getName().trim().isEmpty()) {
            errors.add("Conference name is required");
        }

        if (conference.getLocation() == null || conference.getLocation().trim().isEmpty()) {
            errors.add("Conference location is required");
        }

        if (conference.getStartTimestamp() == null) {
            errors.add("Start time is required");
        }

        if (conference.getEndTimestamp() == null) {
            errors.add("End time is required");
        }

        // if (conference.getStartTimestamp() != null && conference.getEndTimestamp() != null) {
        //     if (conference.getStartTimestamp().isAfter(conference.getEndTimestamp())) {
        //         errors.add("Start time must be before end time");
        //     }
            
        //     if (conference.getStartTimestamp().isBefore(LocalDateTime.now())) {
        //         errors.add("Conference cannot be scheduled in the past");
        //     }

        //     long durationHours = java.time.Duration.between(
        //         conference.getStartTimestamp(), 
        //         conference.getEndTimestamp()
        //     ).toHours();

        //     if (durationHours > MAX_DURATION_HOURS) {
        //         errors.add("Conference duration cannot exceed " + MAX_DURATION_HOURS + " hours");
        //     }
        // }

        if (conference.getTotalSlots() <= 0) {
            errors.add("Total slots must be greater than 0");
        }

        // Validate topics
        // if (conference.getTopics() != null) {
        //     if (conference.getTopics().size() > MAX_TOPICS) {
        //         errors.add("Maximum " + MAX_TOPICS + " topics are allowed");
        //     }

        //     for (String topic : conference.getTopics()) {
        //         if (topic == null || topic.trim().isEmpty()) {
        //             errors.add("Empty topics are not allowed");
        //             break;
        //         }
        //         if (!topic.matches(TOPIC_PATTERN)) {
        //             errors.add("Topic '" + topic + "' contains invalid characters. Only alphanumeric characters and spaces are allowed");
        //             break;
        //         }
        //     }
        // }

        if (!errors.isEmpty()) {
            throw new ValidationException("Conference validation failed", errors);
        }
    }
} 