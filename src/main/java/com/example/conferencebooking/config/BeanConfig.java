package com.example.conferencebooking.config;

import com.example.conferencebooking.repository.BookingRepository;
import com.example.conferencebooking.repository.ConferenceRepository;
import com.example.conferencebooking.repository.UserRepository;
import com.example.conferencebooking.repository.impl.BookingPostgresImpl;
import com.example.conferencebooking.repository.impl.ConferencePostgresImpl;
import com.example.conferencebooking.repository.impl.UserPostgresImpl;
import com.example.conferencebooking.service.BookingService;
import com.example.conferencebooking.service.ConferenceService;
import com.example.conferencebooking.service.UserService;
import com.example.conferencebooking.service.impl.BookingServiceImpl;
import com.example.conferencebooking.service.impl.ConferenceServiceImpl;
import com.example.conferencebooking.service.impl.UserServiceImpl;
import com.example.conferencebooking.service.validator.BookingValidator;
import com.example.conferencebooking.service.validator.ConferenceValidator;
import com.example.conferencebooking.service.validator.UserValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Lazy;

@Configuration
public class BeanConfig {


    @Bean
    public ConferenceRepository conferenceRepository(JdbcTemplate jdbcTemplate) {
        return new ConferencePostgresImpl(jdbcTemplate);
    }

    @Bean
    public BookingRepository bookingRepository(JdbcTemplate jdbcTemplate) {
        return new BookingPostgresImpl(jdbcTemplate);
    }

    @Bean
    public UserRepository userRepository(JdbcTemplate jdbcTemplate) {
        return new UserPostgresImpl(jdbcTemplate);
    }

    @Bean
    public ConferenceService conferenceService(
            ConferenceRepository conferenceRepository, 
            ConferenceValidator conferenceValidator,
            @Lazy BookingService bookingService) {
        return new ConferenceServiceImpl(conferenceRepository, conferenceValidator, bookingService);
    }

    @Bean
    public BookingService bookingService(
            BookingRepository bookingRepository,
            ConferenceService conferenceService,
            UserService userService,
            BookingValidator bookingValidator,
            RabbitTemplate rabbitTemplate) {
        return new BookingServiceImpl(
            bookingRepository,
            conferenceService,
            userService,
            bookingValidator,
            rabbitTemplate
        );
    }

    @Bean
    public UserService userService(UserRepository userRepository,
                                 UserValidator userValidator) {
        return new UserServiceImpl(userRepository, userValidator);
    }
} 