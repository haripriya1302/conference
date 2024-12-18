package com.example.conferencebooking.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class UserResponseDTO {
    private String id;
    private String name;
    private String email;
    private String phoneNumber;
} 