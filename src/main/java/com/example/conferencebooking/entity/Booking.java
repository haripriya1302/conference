package com.example.conferencebooking.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String userId;
    private String conferenceId;
    private LocalDateTime bookingTime;
    private BookingStatus status;
    
    @Builder.Default
    private transient Conference conference = null;
} 