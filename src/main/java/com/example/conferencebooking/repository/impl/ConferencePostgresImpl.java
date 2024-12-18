package com.example.conferencebooking.repository.impl;

import com.example.conferencebooking.entity.Conference;
import com.example.conferencebooking.repository.ConferenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.util.*;

@Repository
public class ConferencePostgresImpl implements ConferenceRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    private final RowMapper<Conference> conferenceRowMapper = (rs, rowNum) -> {
        Array topicsArray = rs.getArray("topics");
        List<String> topics = new ArrayList<>();
        if (topicsArray != null) {
            topics.addAll(Arrays.asList((String[]) topicsArray.getArray()));
        }
        
        return Conference.builder()
                .id(rs.getString("id"))
                .name(rs.getString("name"))
                .location(rs.getString("location"))
                .topics(topics)
                .startTimestamp(rs.getTimestamp("start_timestamp").toLocalDateTime())
                .endTimestamp(rs.getTimestamp("end_timestamp").toLocalDateTime())
                .totalSlots(rs.getInt("total_slots"))
                .availableSlots(rs.getInt("available_slots"))
                .fullyBooked(rs.getBoolean("fully_booked"))
                .waitlistQueueId(rs.getString("waitlist_queue_id"))
                .build();
    };

    @Autowired
    public ConferencePostgresImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Conference save(Conference conference) {
        String sql = """
            INSERT INTO conferences (id, name, location, start_timestamp, end_timestamp, 
                total_slots, available_slots, waitlist_queue_id, fully_booked, topics) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::text[]) 
            ON CONFLICT (id) DO UPDATE SET 
                name = EXCLUDED.name, 
                location = EXCLUDED.location,
                start_timestamp = EXCLUDED.start_timestamp,
                end_timestamp = EXCLUDED.end_timestamp,
                total_slots = EXCLUDED.total_slots,
                available_slots = EXCLUDED.available_slots,
                waitlist_queue_id = EXCLUDED.waitlist_queue_id,
                fully_booked = EXCLUDED.fully_booked,
                topics = EXCLUDED.topics
        """;

        jdbcTemplate.update(sql,
            conference.getId(),
            conference.getName(),
            conference.getLocation(),
            conference.getStartTimestamp(),
            conference.getEndTimestamp(),
            conference.getTotalSlots(),
            conference.getAvailableSlots(),
            conference.getWaitlistQueueId(),
            conference.isFullyBooked(),
            conference.getTopics().toArray(new String[0])
        );

        return conference;
    }

    @Override
    public Optional<Conference> findById(String id) {
        String sql = "SELECT * FROM conferences WHERE id = ?";
        List<Conference> conferences = jdbcTemplate.query(sql, conferenceRowMapper, id);
        return conferences.isEmpty() ? Optional.empty() : Optional.of(conferences.get(0));
    }

    @Override
    public List<Conference> findAll() {
        String sql = "SELECT * FROM conferences";
        return jdbcTemplate.query(sql, conferenceRowMapper);
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM conferences WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    private String toJson(Set<String> topics) {
        if (topics == null || topics.isEmpty()) {
            return "[]";
        }
        return "[" + String.join(",", topics.stream()
            .map(topic -> "\"" + topic.replace("\"", "\\\"") + "\"")
            .toList()) + "]";
    }
} 