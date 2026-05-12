package com.library.controller;

import com.library.repository.UserRepository;
import com.library.service.ReservationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final UserRepository userRepository;

    @PostMapping("/create")
    public ResponseEntity<?> createReservation(
            @RequestParam Long bookId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = getUserId(userDetails);
            reservationService.createReservation(userId, bookId);
            return ResponseEntity.ok(Map.of("message", "Dat giu thanh cong!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/cancel/{id}")
    public String cancelReservation(@PathVariable Long id) {
        reservationService.cancelReservation(id);
        return "redirect:/reader/reservations";
    }

    @PostMapping("/fulfill/{id}")
    public String fulfillReservation(@PathVariable Long id) {
        reservationService.fulfillReservation(id);
        return "redirect:/librarian/reservations";
    }

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new EntityNotFoundException("User khong ton tai"))
            .getId();
    }
}
