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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;
import org.springframework.web.servlet.view.RedirectView;

import com.example.conferencebooking.dto.ConferenceRequestDTO;
import com.example.conferencebooking.dto.ConferenceSearchDTO;
import com.example.conferencebooking.entity.Conference;
import com.example.conferencebooking.entity.User;
import com.example.conferencebooking.exception.ValidationException;
import com.example.conferencebooking.mapper.ConferenceMapper;
import com.example.conferencebooking.service.BookingService;
import com.example.conferencebooking.service.ConferenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class ConferenceControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ConferenceService conferenceService;

    @Mock
    private ConferenceMapper conferenceMapper;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private ConferenceController conferenceController;

    private Conference testConference;
    private ConferenceRequestDTO testConferenceRequest;
    private User testUser;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(conferenceController)
                .setViewResolvers((viewName, locale) -> {
                    if (viewName.startsWith("redirect:")) {
                        return new RedirectView(viewName.substring(9));
                    }
                    return new MappingJackson2JsonView();
                })
                .build();
        
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        session = new MockHttpSession();

        testUser = User.builder()
                .id(UUID.randomUUID().toString())
                .email("test@example.com")
                .build();

        testConference = Conference.builder()
                .id(UUID.randomUUID().toString())
                .name("Test Conference")
                .location("Test Location")
                .topics(Arrays.asList("Java", "Spring"))
                .startTimestamp(LocalDateTime.now().plusDays(1))
                .endTimestamp(LocalDateTime.now().plusDays(2))
                .totalSlots(100)
                .availableSlots(50)
                .build();

        testConferenceRequest = ConferenceRequestDTO.builder()
                .name(testConference.getName())
                .location(testConference.getLocation())
                .topics(new ArrayList<>(Arrays.asList("Java", "Spring")))
                .startTimestamp(testConference.getStartTimestamp())
                .endTimestamp(testConference.getEndTimestamp())
                .totalSlots(testConference.getTotalSlots())
                .build();

        session.setAttribute("user", testUser);
    }

    @Test
    void listConferences_WhenUserLoggedIn_ShouldShowConferencesWithBookingStatus() throws Exception {
        when(conferenceService.getAllConferences()).thenReturn(Arrays.asList(testConference));
        when(bookingService.getUserBookings(testUser.getId())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/conferences").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("conferences/list"))
                .andExpect(model().attributeExists("availableConferences", "user"));
    }

    @Test
    void listConferences_WhenUserNotLoggedIn_ShouldShowConferences() throws Exception {
        when(conferenceService.getAllConferences()).thenReturn(Arrays.asList(testConference));

        mockMvc.perform(get("/conferences"))
                .andExpect(status().isOk())
                .andExpect(view().name("conferences/list"))
                .andExpect(model().attributeExists("availableConferences"));
    }

    @Test
    void showCreateForm_ShouldShowForm() throws Exception {
        mockMvc.perform(get("/conferences/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("conferences/create"))
                .andExpect(model().attributeExists("conferenceRequest"));
    }

    @Test
    void createConferenceView_WhenValidRequest_ShouldCreateConference() throws Exception {
        when(conferenceMapper.toEntity(any(ConferenceRequestDTO.class))).thenReturn(testConference);
        when(conferenceService.createConference(any(Conference.class))).thenReturn(testConference);

        mockMvc.perform(post("/conferences/create")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", testConference.getName())
                .param("location", testConference.getLocation())
                .param("topics", "Java")
                .param("topics", "Spring")
                .param("startTimestamp", testConference.getStartTimestamp().toString())
                .param("endTimestamp", testConference.getEndTimestamp().toString())
                .param("totalSlots", String.valueOf(testConference.getTotalSlots())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/conferences"))
                .andExpect(flash().attributeExists("message"));

        verify(conferenceMapper).toEntity(any(ConferenceRequestDTO.class));
        verify(conferenceService).createConference(any(Conference.class));
    }

    @Test
    void createConferenceView_WhenValidationFails_ShouldShowError() throws Exception {
        when(conferenceMapper.toEntity(any(ConferenceRequestDTO.class))).thenReturn(testConference);
        when(conferenceService.createConference(any(Conference.class)))
                .thenThrow(new ValidationException("Invalid conference data"));

        mockMvc.perform(post("/conferences/create")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", testConference.getName())
                .param("location", testConference.getLocation())
                .param("topics", String.join(",", testConference.getTopics()))
                .param("startTimestamp", testConference.getStartTimestamp().toString())
                .param("endTimestamp", testConference.getEndTimestamp().toString())
                .param("totalSlots", String.valueOf(testConference.getTotalSlots())))
                .andExpect(status().isOk())
                .andExpect(view().name("conferences/create"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void createConference_WhenValidRequest_ShouldCreateConference() throws Exception {
        when(conferenceMapper.toEntity(any(ConferenceRequestDTO.class))).thenReturn(testConference);
        when(conferenceService.createConference(any(Conference.class))).thenReturn(testConference);

        mockMvc.perform(post("/api/conferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testConferenceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testConference.getId()));
    }

    @Test
    void getConference_ShouldReturnConference() throws Exception {
        when(conferenceService.getConference(testConference.getId())).thenReturn(testConference);

        mockMvc.perform(get("/api/conferences/{id}", testConference.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testConference.getId()));
    }

    @Test
    void getAllConferences_ShouldReturnAllConferences() throws Exception {
        when(conferenceService.getAllConferences()).thenReturn(Arrays.asList(testConference));

        mockMvc.perform(get("/api/conferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testConference.getId()));
    }

    @Test
    void deleteConference_ShouldDeleteConference() throws Exception {
        doNothing().when(conferenceService).deleteConference(testConference.getId());

        mockMvc.perform(delete("/api/conferences/{id}", testConference.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void searchConferences_WhenUserLoggedIn_ShouldSearchWithUserId() throws Exception {
        ConferenceSearchDTO searchDTO = new ConferenceSearchDTO();
        when(conferenceService.searchConferences(any(ConferenceSearchDTO.class)))
                .thenReturn(Arrays.asList(testConference));

        mockMvc.perform(post("/api/conferences/search")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testConference.getId()));
    }

    @Test
    void getSuggestedConferences_WhenUserLoggedIn_ShouldReturnSuggestions() throws Exception {
        when(conferenceService.getSuggestedConferences(testUser.getId()))
                .thenReturn(Arrays.asList(testConference));

        mockMvc.perform(get("/api/conferences/suggested")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testConference.getId()));
    }

    @Test
    void getSuggestedConferences_WhenUserNotLoggedIn_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/conferences/suggested"))
                .andExpect(status().isUnauthorized());
    }
}
