package com.example.conferencebooking.controller;

import com.example.conferencebooking.entity.User;
import com.example.conferencebooking.service.ConferenceService;
import com.example.conferencebooking.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.hamcrest.Matchers;

class HomeControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private ConferenceService conferenceService;

    @Mock
    private Model model;

    @InjectMocks
    private HomeController homeController;
    private MockMvc mockMvc;
    private MockHttpSession session;
    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(homeController).build();
        session = new MockHttpSession();
        
        testUser = User.builder()
                .id(UUID.randomUUID().toString())
                .email("test@example.com")
                .build();
    }

    @Test
    void home_whenUserNotLoggedIn_shouldShowLoginPage() throws Exception {
        // Given
        when(conferenceService.getUpcomingConferences()).thenReturn(Collections.emptyList());

        // When/Then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeDoesNotExist("user"))
                .andExpect(model().attributeExists("upcomingConferences"));

        verify(conferenceService).getUpcomingConferences();
        verify(userService, never()).getUserBookings(any());
    }

    @Test
    void home_whenUserLoggedIn_shouldShowHomePageWithUserInfo() throws Exception {
        // Given
        session.setAttribute("user", testUser);

        // When/Then
        mockMvc.perform(get("/")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user", testUser));
    }

    @Test
    void home_shouldShowUpcomingConferences() throws Exception {
        // Given
        when(conferenceService.getUpcomingConferences()).thenReturn(Collections.emptyList());

        // When/Then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("upcomingConferences"));

        verify(conferenceService).getUpcomingConferences();
    }

    @Test
    void home_whenUserLoggedIn_shouldShowUserBookings() throws Exception {
        // Given
        session.setAttribute("user", testUser);
        when(userService.getUserBookings(testUser.getId())).thenReturn(Collections.emptyList());

        // When/Then
        mockMvc.perform(get("/")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("userBookings"));

        verify(userService).getUserBookings(testUser.getId());
    }
}