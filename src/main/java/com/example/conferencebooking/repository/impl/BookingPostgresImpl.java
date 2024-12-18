package com.example.conferencebooking.repository.impl;

import com.example.conferencebooking.entity.Booking;
import com.example.conferencebooking.entity.BookingStatus;
import com.example.conferencebooking.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class BookingPostgresImpl implements BookingRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    private final RowMapper<Booking> bookingRowMapper = (rs, rowNum) -> Booking.builder()
            .id(rs.getString("id"))
            .userId(rs.getString("user_id"))
            .conferenceId(rs.getString("conference_id"))
            .bookingTime(rs.getTimestamp("booking_time").toLocalDateTime())
            .status(BookingStatus.valueOf(rs.getString("status")))
            .build();

    @Autowired
    public BookingPostgresImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Booking save(Booking booking) {
        String sql = "INSERT INTO bookings (id, user_id, conference_id, booking_time, status) " +
                    "VALUES (?, ?, ?, ?, ?) " +
                    "ON CONFLICT (id) DO UPDATE SET " +
                    "user_id = EXCLUDED.user_id, " +
                    "conference_id = EXCLUDED.conference_id, " +
                    "booking_time = EXCLUDED.booking_time, " +
                    "status = EXCLUDED.status";
        
        jdbcTemplate.update(sql,
                booking.getId(),
                booking.getUserId(),
                booking.getConferenceId(),
                booking.getBookingTime(),
                booking.getStatus().toString());
        
        return booking;
    }

    @Override
    public Optional<Booking> findById(String id) {
        String sql = """
            SELECT b.*, u.email as user_email 
            FROM bookings b 
            JOIN users u ON b.user_id = u.id 
            WHERE b.id = ?
        """;
        
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> Booking.builder()
                    .id(rs.getString("id"))
                    .userId(rs.getString("user_id"))
                    .conferenceId(rs.getString("conference_id"))
                    .bookingTime(rs.getTimestamp("booking_time").toLocalDateTime())
                    .status(BookingStatus.valueOf(rs.getString("status")))
                    .build(),
                id
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Booking> findByConferenceId(String conferenceId) {
        String sql = "SELECT * FROM bookings WHERE conference_id = ?";
        return jdbcTemplate.query(sql, bookingRowMapper, conferenceId);
    }

    @Override
    public List<Booking> findByUserId(String userId) {
        String sql = """
            SELECT b.*, u.email as user_email 
            FROM bookings b 
            JOIN users u ON b.user_id = u.id 
            WHERE b.user_id = ?
        """;
        
        return jdbcTemplate.query(
            sql, 
            (rs, rowNum) -> Booking.builder()
                .id(rs.getString("id"))
                .userId(rs.getString("user_id"))
                .conferenceId(rs.getString("conference_id"))
                .bookingTime(rs.getTimestamp("booking_time").toLocalDateTime())
                .status(BookingStatus.valueOf(rs.getString("status")))
                .build(),
            userId
        );
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM bookings WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public List<Booking> findByConferenceIdAndStatus(String conferenceId, BookingStatus status) {
        String sql = "SELECT * FROM bookings WHERE conference_id = ? AND status = ?::booking_status";
        return jdbcTemplate.query(sql, bookingRowMapper, conferenceId, status.name());
    }

    @Override
    public List<Booking> findByStatus(BookingStatus status) {
        String sql = "SELECT * FROM bookings WHERE status = ?::booking_status";
        return jdbcTemplate.query(sql, bookingRowMapper, status.name());
    }
} 