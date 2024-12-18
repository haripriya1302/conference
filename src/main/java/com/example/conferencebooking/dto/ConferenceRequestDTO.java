package com.example.conferencebooking.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConferenceRequestDTO {
    private String name;
    private String location;
    private List<String> topics;
    private LocalDateTime startTimestamp;
    private LocalDateTime endTimestamp;
    private Integer totalSlots;
} 