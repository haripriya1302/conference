package com.example.conferencebooking.repository.impl;

import com.example.conferencebooking.entity.Booking;
import com.example.conferencebooking.entity.BookingStatus;
import com.example.conferencebooking.entity.User;
import com.example.conferencebooking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserPostgresImpl implements UserRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    private final RowMapper<User> userRowMapper = (rs, rowNum) -> User.builder()
            .id(rs.getString("id"))
            .name(rs.getString("name"))
            .email(rs.getString("email"))
            .phoneNumber(rs.getString("phone_number"))
            .build();

    @Autowired
    public UserPostgresImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public User save(User user) {
        String sql = "INSERT INTO users (id, name, email, phone_number) " +
                    "VALUES (?, ?, ?, ?) " +
                    "ON CONFLICT (id) DO UPDATE SET " +
                    "name = EXCLUDED.name, " +
                    "email = EXCLUDED.email, " +
                    "phone_number = EXCLUDED.phone_number";
        
        jdbcTemplate.update(sql,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber());
        
        return user;
    }

    @Override
    public Optional<User> findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, id);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, email);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM users WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public List<Booking> findUserBookings(String userId) {
        String sql = "SELECT * FROM bookings WHERE user_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> Booking.builder()
                .id(rs.getString("id"))
                .userId(rs.getString("user_id"))
                .conferenceId(rs.getString("conference_id"))
                .status(BookingStatus.valueOf(rs.getString("status")))
                .bookingTime(rs.getTimestamp("booking_time").toLocalDateTime())
                .build(), userId);
    }
} 