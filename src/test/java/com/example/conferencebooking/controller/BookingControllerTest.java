package com.example.conferencebooking.controller;

import com.example.conferencebooking.entity.Booking;
import com.example.conferencebooking.entity.Conference;
import com.example.conferencebooking.entity.User;
import com.example.conferencebooking.entity.BookingStatus;
import com.example.conferencebooking.service.BookingService;
import com.example.conferencebooking.service.ConferenceService;
import com.example.conferencebooking.exception.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private BookingService bookingService;

    @Mock
    private ConferenceService conferenceService;

    private BookingController bookingController;
    private User testUser;
    private Conference testConference;
    private Booking testBooking;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        bookingController = new BookingController(bookingService, conferenceService);
        mockMvc = MockMvcBuilders.standaloneSetup(bookingController).build();
        objectMapper = new ObjectMapper();
        session = new MockHttpSession();

        testUser = User.builder()
                .id(UUID.randomUUID().toString())
                .email("test@example.com")
                .build();

        testConference = Conference.builder()
                .id(UUID.randomUUID().toString())
                .name("Test Conference")
                .startTimestamp(LocalDateTime.now().plusDays(1))
                .endTimestamp(LocalDateTime.now().plusDays(2))
                .build();

        testBooking = Booking.builder()
                .id(UUID.randomUUID().toString())
                .userId(testUser.getId())
                .conferenceId(testConference.getId())
                .status(BookingStatus.CONFIRMED)
                .build();

        session.setAttribute("user", testUser);
    }

    @Test
    void listBookings_WhenUserLoggedIn_ShouldShowBookings() throws Exception {
        when(bookingService.getUserBookings(testUser.getId()))
                .thenReturn(Arrays.asList(testBooking));

        mockMvc.perform(get("/bookings").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("bookings/list"))
                .andExpect(model().attributeExists("userBookings"));
    }

    @Test
    void listBookings_WhenUserNotLoggedIn_ShouldRedirect() throws Exception {
        mockMvc.perform(get("/bookings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void createBooking_WhenValidRequest_ShouldCreateBooking() throws Exception {
        when(conferenceService.getConference(testConference.getId())).thenReturn(testConference);
        when(bookingService.createBooking(testUser.getId(), testConference.getId())).thenReturn(testBooking);

        mockMvc.perform(post("/api/bookings")
                .session(session)
                .param("conferenceId", testConference.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testBooking.getId()));
    }

    @Test
    void createBooking_WhenConferenceEnded_ShouldReturnError() throws Exception {
        testConference.setEndTimestamp(LocalDateTime.now().minusDays(1));
        when(conferenceService.getConference(testConference.getId())).thenReturn(testConference);

        mockMvc.perform(post("/api/bookings")
                .session(session)
                .param("conferenceId", testConference.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot book - Conference has ended"));
    }

    @Test
    void createBooking_WhenUserNotLoggedIn_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/bookings")
                .param("conferenceId", testConference.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getBooking_WhenAuthorized_ShouldReturnBooking() throws Exception {
        when(bookingService.getBooking(testBooking.getId())).thenReturn(testBooking);

        mockMvc.perform(get("/api/bookings/{id}", testBooking.getId())
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testBooking.getId()));
    }

    @Test
    void getBooking_WhenUnauthorized_ShouldReturnForbidden() throws Exception {
        Booking otherUserBooking = Booking.builder()
                .id(UUID.randomUUID().toString())
                .userId("other-user-id")
                .build();
        when(bookingService.getBooking(otherUserBooking.getId())).thenReturn(otherUserBooking);

        mockMvc.perform(get("/api/bookings/{id}", otherUserBooking.getId())
                .session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelBooking_WhenValidRequest_ShouldCancelBooking() throws Exception {
        when(bookingService.getBooking(testBooking.getId())).thenReturn(testBooking);
        when(conferenceService.getConference(testConference.getId())).thenReturn(testConference);
        doNothing().when(bookingService).cancelBooking(testBooking.getId());

        mockMvc.perform(delete("/api/bookings/cancel/{id}", testBooking.getId())
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void cancelBooking_WhenConferenceStarted_ShouldReturnError() throws Exception {
        testConference.setStartTimestamp(LocalDateTime.now().minusDays(1));
        when(bookingService.getBooking(testBooking.getId())).thenReturn(testBooking);
        when(conferenceService.getConference(testConference.getId())).thenReturn(testConference);

        mockMvc.perform(delete("/api/bookings/cancel/{id}", testBooking.getId())
                .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot cancel booking after conference has started"));
    }

    @Test
    void confirmBooking_WhenValidRequest_ShouldConfirmBooking() throws Exception {
        when(bookingService.getBooking(testBooking.getId())).thenReturn(testBooking);
        when(bookingService.confirmWaitlistBooking(testBooking.getId())).thenReturn(testBooking);

        mockMvc.perform(post("/api/bookings/{id}/confirm", testBooking.getId())
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void confirmBooking_WhenValidationFails_ShouldReturnError() throws Exception {
        when(bookingService.getBooking(testBooking.getId())).thenReturn(testBooking);
        when(bookingService.confirmWaitlistBooking(testBooking.getId()))
                .thenThrow(new ValidationException("Confirmation time expired"));

        mockMvc.perform(post("/api/bookings/{id}/confirm", testBooking.getId())
                .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Confirmation time expired"));
    }

    @Test
    void getBookingStatus_WhenValidRequest_ShouldReturnStatus() throws Exception {
        when(bookingService.getUserBookings(testUser.getId()))
                .thenReturn(Arrays.asList(testBooking));

        mockMvc.perform(get("/api/bookings/status/{conferenceId}", testConference.getId())
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(testBooking.getStatus().toString()));
    }

    @Test
    void getBookingStatus_WhenUserNotLoggedIn_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/bookings/status/{conferenceId}", testConference.getId()))
                .andExpect(status().isUnauthorized());
    }
}
