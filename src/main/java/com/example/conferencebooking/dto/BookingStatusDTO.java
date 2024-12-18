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
public class BookingStatusDTO {
    private BookingStatus status;
    private boolean canConfirm;
    private LocalDateTime confirmUntil;
} 