package com.example.conferencebooking.repository;

import com.example.conferencebooking.entity.Conference;
import java.util.List;
import java.util.Optional;

// Using locks here can prevent duplicates. In the interest of time,
// skipping this since I already used transactional in the service layer.
//Versioning is another choice.
public interface ConferenceRepository {
    Conference save(Conference conference);
    Optional<Conference> findById(String id);
    List<Conference> findAll();
    void deleteById(String id);
} 