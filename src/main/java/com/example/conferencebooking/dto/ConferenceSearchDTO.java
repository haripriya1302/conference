package com.example.conferencebooking.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConferenceSearchDTO {
    private String userId;
    private String name;
    private String location;
    private List<String> topics;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String timeframe; // "all", "upcoming", "past"
} 