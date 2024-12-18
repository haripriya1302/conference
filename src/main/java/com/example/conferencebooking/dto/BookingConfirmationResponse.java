package com.example.conferencebooking.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import com.example.conferencebooking.entity.BookingStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingConfirmationResponse {
    private boolean success;
    private String message;
    private BookingStatus status;
    private LocalDateTime confirmUntil;
    private String bookingId;
    private String reason;
} 