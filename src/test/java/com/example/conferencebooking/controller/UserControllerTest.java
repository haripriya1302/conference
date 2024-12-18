package com.example.conferencebooking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.conferencebooking.dto.UserRequestDTO;
import com.example.conferencebooking.dto.UserResponseDTO;
import com.example.conferencebooking.entity.User;
import com.example.conferencebooking.exception.GlobalExceptionHandler;
import com.example.conferencebooking.mapper.UserMapper;
import com.example.conferencebooking.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private MockHttpSession session;
    private User testUser;
    private UserResponseDTO testUserResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        session = new MockHttpSession();
        
        testUser = User.builder()
                .id(UUID.randomUUID().toString())
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("1234567890")
                .build();

        testUserResponse = UserResponseDTO.builder()
                .id(testUser.getId())
                .name(testUser.getName())
                .email(testUser.getEmail())
                .phoneNumber(testUser.getPhoneNumber())
                .build();
    }

    @Test
    void createUserApi_WhenValidRequest_ShouldCreateUser() throws Exception {
        // Given
        UserRequestDTO requestDTO = UserRequestDTO.builder()
                .name(testUser.getName())
                .email(testUser.getEmail())
                .phoneNumber(testUser.getPhoneNumber())
                .build();

        when(userMapper.toEntity(any(UserRequestDTO.class))).thenReturn(testUser);
        when(userService.createUser(any(User.class))).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(testUserResponse);

        // When
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserResponse.getId()))
                .andExpect(jsonPath("$.name").value(testUserResponse.getName()))
                .andExpect(jsonPath("$.email").value(testUserResponse.getEmail()))
                .andExpect(jsonPath("$.phoneNumber").value(testUserResponse.getPhoneNumber()));

        // Then
        verify(userMapper).toEntity(any(UserRequestDTO.class));
        verify(userService).createUser(any(User.class));
        verify(userMapper).toDto(testUser);
    }

    @Test
    void getUser_WhenUserExists_ShouldReturnUser() throws Exception {
        when(userService.getUser(testUser.getId())).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(testUserResponse);

        mockMvc.perform(get("/api/users/{id}", testUser.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));
    }

    @Test
    void getUserByEmail_WhenUserExists_ShouldReturnUser() throws Exception {
        when(userService.getUserByEmail(testUser.getEmail())).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(testUserResponse);

        mockMvc.perform(get("/api/users/email/{email}", testUser.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserResponse.getId()))
                .andExpect(jsonPath("$.name").value(testUserResponse.getName()))
                .andExpect(jsonPath("$.email").value(testUserResponse.getEmail()))
                .andExpect(jsonPath("$.phoneNumber").value(testUserResponse.getPhoneNumber()));
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() throws Exception {
        when(userService.getAllUsers()).thenReturn(Arrays.asList(testUser));
        when(userMapper.toDto(testUser)).thenReturn(testUserResponse);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testUserResponse.getId()))
                .andExpect(jsonPath("$[0].name").value(testUserResponse.getName()))
                .andExpect(jsonPath("$[0].email").value(testUserResponse.getEmail()))
                .andExpect(jsonPath("$[0].phoneNumber").value(testUserResponse.getPhoneNumber()));
    }

    @Test
    void deleteUser_ShouldDeleteUser() throws Exception {
        doNothing().when(userService).deleteUser(testUser.getId());

        mockMvc.perform(delete("/api/users/{id}", testUser.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void login_WhenValidCredentials_ShouldLoginSuccessfully() throws Exception {
        when(userService.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/users/login")
                .param("email", testUser.getEmail()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    void login_WhenUserNotFound_ShouldRedirectWithError() throws Exception {
        when(userService.findByEmail(testUser.getEmail())).thenReturn(Optional.empty());

        mockMvc.perform(post("/users/login")
                .param("email", testUser.getEmail()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("registerEmail"));
    }

    @Test
    void register_WhenValidUser_ShouldRegisterSuccessfully() throws Exception {
        when(userService.createUser(any(User.class))).thenReturn(testUser);

        mockMvc.perform(post("/users/register")
                .param("name", testUser.getName())
                .param("email", testUser.getEmail())
                .param("phoneNumber", testUser.getPhoneNumber()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    void register_WhenValidationFails_ShouldRedirectWithError() throws Exception {
        when(userService.createUser(any(User.class))).thenThrow(new RuntimeException("Validation failed"));

        mockMvc.perform(post("/users/register")
                .param("name", testUser.getName())
                .param("email", testUser.getEmail())
                .param("phoneNumber", testUser.getPhoneNumber()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void logout_ShouldClearSessionAndRedirect() throws Exception {
        session.setAttribute("user", testUser);

        mockMvc.perform(post("/users/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("message"));
    }
} 