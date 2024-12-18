package com.example.conferencebooking.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpSession;
import com.example.conferencebooking.entity.User;
import com.example.conferencebooking.service.ConferenceService;
import com.example.conferencebooking.service.UserService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HomeController {
    private final ConferenceService conferenceService;
    private final UserService userService;

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("user", user);
            model.addAttribute("userBookings", userService.getUserBookings(user.getId()));
        }
        model.addAttribute("upcomingConferences", conferenceService.getUpcomingConferences());
        return "index";
    }
} 