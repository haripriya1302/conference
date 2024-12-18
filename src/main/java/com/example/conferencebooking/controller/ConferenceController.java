package com.example.conferencebooking.controller;

import com.example.conferencebooking.dto.ConferenceRequestDTO;
import com.example.conferencebooking.dto.ConferenceSearchDTO;
import com.example.conferencebooking.entity.Conference;
import com.example.conferencebooking.entity.User;
import com.example.conferencebooking.entity.Booking;
import com.example.conferencebooking.entity.BookingStatus;
import com.example.conferencebooking.exception.ValidationException;
import com.example.conferencebooking.mapper.ConferenceMapper;
import com.example.conferencebooking.service.ConferenceService;
import com.example.conferencebooking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import java.util.List;



@Controller
@RequiredArgsConstructor
@Slf4j
public class ConferenceController {
    private final ConferenceService conferenceService;
    private final ConferenceMapper conferenceMapper;
    private final BookingService bookingService;

    // View endpoints
    @GetMapping("/conferences")
    public String listConferences(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        List<Conference> conferences = conferenceService.getAllConferences();
        
        if (user != null) {
            List<Booking> userBookings = bookingService.getUserBookings(user.getId());
            // Add hasActiveBooking flag to each conference
            conferences.forEach(conference -> {
                boolean hasActiveBooking = userBookings.stream()
                    .anyMatch(booking -> 
                        booking.getConferenceId().equals(conference.getId()) &&
                        (booking.getStatus() == BookingStatus.CONFIRMED || 
                         booking.getStatus() == BookingStatus.WAITLISTED)
                    );
                conference.setHasActiveBooking(hasActiveBooking);
            });
        }
        
        model.addAttribute("availableConferences", conferences);
        model.addAttribute("user", user);
        return "conferences/list";
    }

    @GetMapping("/conferences/create")
    public String showCreateForm(Model model) {
        model.addAttribute("conferenceRequest", new ConferenceRequestDTO());
        return "conferences/create";
    }

    @PostMapping("/conferences/create")
    public String createConferenceView(@ModelAttribute ConferenceRequestDTO conferenceRequest, 
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        try {
            Conference conference = conferenceMapper.toEntity(conferenceRequest);
            conferenceService.createConference(conference);
            redirectAttributes.addFlashAttribute("message", "Conference created successfully!");
            return "redirect:/conferences";
        } catch (ValidationException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("validationErrors", e.getErrors());
            model.addAttribute("conferenceRequest", conferenceRequest);
            return "conferences/create";
        } catch (Exception e) {
            model.addAttribute("error", "An unexpected error occurred: " + e.getMessage());
            model.addAttribute("conferenceRequest", conferenceRequest);
            return "conferences/create";
        }
    }

    // REST API endpoints
    @PostMapping("/api/conferences")
    @ResponseBody
    public ResponseEntity<Conference> createConference(@RequestBody ConferenceRequestDTO conferenceDTO) {
        Conference conference = conferenceMapper.toEntity(conferenceDTO);
        return ResponseEntity.ok(conferenceService.createConference(conference));
    }

    @GetMapping("/api/conferences/{id}")
    @ResponseBody
    public ResponseEntity<Conference> getConference(@PathVariable String id) {
        return ResponseEntity.ok(conferenceService.getConference(id));
    }

    @GetMapping("/api/conferences")
    @ResponseBody
    public ResponseEntity<List<Conference>> getAllConferences() {
        return ResponseEntity.ok(conferenceService.getAllConferences());
    }

    @DeleteMapping("/api/conferences/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteConference(@PathVariable String id) {
        conferenceService.deleteConference(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/conferences/search")
    @ResponseBody
    public ResponseEntity<List<Conference>> searchConferences(@RequestBody ConferenceSearchDTO searchDTO, HttpSession session) {
        try {
            User user = (User) session.getAttribute("user");
            if (user != null) {
                searchDTO.setUserId(user.getId());
            }
            
            List<Conference> results = conferenceService.searchConferences(searchDTO);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error searching conferences: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/conferences/suggested")
    @ResponseBody
    public ResponseEntity<List<Conference>> getSuggestedConferences(HttpSession session) {
        try {
            User user = (User) session.getAttribute("user");
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            List<Conference> suggestions = conferenceService.getSuggestedConferences(user.getId());
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            log.error("Error getting suggested conferences: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
} 