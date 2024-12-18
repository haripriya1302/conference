package com.example.conferencebooking.mapper;

import com.example.conferencebooking.dto.BookingResponseDTO;
import com.example.conferencebooking.entity.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {
    public BookingResponseDTO toDto(Booking booking) {
        return BookingResponseDTO.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .conferenceId(booking.getConferenceId())
                .bookingTime(booking.getBookingTime())
                .status(booking.getStatus())
                .build();
    }
} 