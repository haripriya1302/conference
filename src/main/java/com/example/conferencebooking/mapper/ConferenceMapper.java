package com.example.conferencebooking.mapper;

import com.example.conferencebooking.dto.ConferenceRequestDTO;
import com.example.conferencebooking.entity.Conference;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.UUID;

@Component
public class ConferenceMapper {
    public Conference toEntity(ConferenceRequestDTO dto) {
        return Conference.builder()
                .id(UUID.randomUUID().toString())
                .name(dto.getName())
                .location(dto.getLocation())
                .topics(new ArrayList<>(dto.getTopics()))
                .startTimestamp(dto.getStartTimestamp())
                .endTimestamp(dto.getEndTimestamp())
                .totalSlots(dto.getTotalSlots())
                .availableSlots(dto.getTotalSlots())
                .build();
    }
} 