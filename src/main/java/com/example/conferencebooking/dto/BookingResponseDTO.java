package com.example.conferencebooking.dto;

import com.example.conferencebooking.entity.BookingStatus;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
public class BookingResponseDTO {
    private String id;
    private String userId;
    private String conferenceId;
    private LocalDateTime bookingTime;
    private BookingStatus status;
} 