package com.example.conferencebooking.mapper;

import com.example.conferencebooking.dto.UserRequestDTO;
import com.example.conferencebooking.dto.UserResponseDTO;
import com.example.conferencebooking.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public User toEntity(UserRequestDTO dto) {
        return User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .build();
    }

    public UserResponseDTO toDto(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }
} 