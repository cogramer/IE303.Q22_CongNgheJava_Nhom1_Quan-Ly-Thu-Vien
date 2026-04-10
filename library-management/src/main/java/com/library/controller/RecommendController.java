package com.library.controller;

import com.library.dto.BookDTO;
import com.library.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.library.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;

@Controller
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;
    private final UserRepository userRepository;

    @GetMapping("/recommendations")
    public String getRecommendations(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        // Lấy userId từ username đang đăng nhập
        Long userId = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User không tồn tại"))
                .getId();

        model.addAttribute("books", recommendService.recommendBooks(userId));
        model.addAttribute("username", userDetails.getUsername());

        return "recommendations";
    }
}