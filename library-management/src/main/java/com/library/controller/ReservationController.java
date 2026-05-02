package com.library.controller;

import com.library.model.Reservation;
import com.library.model.User;
import com.library.repository.UserRepository;
import com.library.service.ReservationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final UserRepository userRepository;

    @PostMapping("/create")
    public String createReservation(
            @RequestParam Long bookId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        reservationService.createReservation(userId, bookId);
        return "redirect:/reader/books";
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
            .orElseThrow(() -> new EntityNotFoundException("User không tồn tại"))
            .getId();
    }
}