package com.example.conferencebooking.controller;

import com.example.conferencebooking.dto.UserRequestDTO;
import com.example.conferencebooking.dto.UserResponseDTO;
import com.example.conferencebooking.entity.User;
import com.example.conferencebooking.mapper.UserMapper;
import com.example.conferencebooking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; 
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping("/api/users")
    @ResponseBody
    public ResponseEntity<UserResponseDTO> createUserApi(@RequestBody UserRequestDTO userDTO) {
        User user = userMapper.toEntity(userDTO);
        user = userService.createUser(user);
        return ResponseEntity.ok(userMapper.toDto(user));
    }

    @GetMapping("/api/users/{id}")
    public ResponseEntity<UserResponseDTO> getUser(@PathVariable String id) {
        User user = userService.getUser(id);
        return ResponseEntity.ok(userMapper.toDto(user));
    }

    @GetMapping("/api/users/email/{email}")
    public ResponseEntity<UserResponseDTO> getUserByEmail(@PathVariable String email) {
        User user = userService.getUserByEmail(email);
        return ResponseEntity.ok(userMapper.toDto(user));
    }

    @GetMapping("/api/users")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        List<UserResponseDTO> users = userService.getAllUsers()
                .stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/api/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/login")
    public String login(@RequestParam String email, 
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        try {
            Optional<User> userOpt = userService.findByEmail(email);
            
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("registerEmail", email);
                redirectAttributes.addFlashAttribute("message", 
                    "Email not found. Please register first.");
                return "redirect:/";
            }
            
            User user = userOpt.get();
            session.setAttribute("user", user);
            redirectAttributes.addFlashAttribute("message", "Welcome back!");
            return "redirect:/";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }

    @PostMapping("/users/register")
    public String register(@ModelAttribute User user,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        try {
            user.setId(UUID.randomUUID().toString());
            User savedUser = userService.createUser(user);
            session.setAttribute("user", savedUser);
            redirectAttributes.addFlashAttribute("message", "Registration successful!");
            return "redirect:/";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }

    @PostMapping("/users/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.removeAttribute("user");
        redirectAttributes.addFlashAttribute("message", "Logged out successfully");
        return "redirect:/";
    }
} 