package com.library.controller;

import com.library.repository.UserRepository;
import com.library.service.BookService;
import com.library.service.BorrowRecordService;
import com.library.service.RecommendService;
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
@RequestMapping("/reader")
public class ReaderController {

    private final BookService bookService;
    private final ReservationService reservationService;
    private final RecommendService recommendService;
    private final UserRepository userRepository;
    private final BorrowRecordService borrowRecordService;

    // Trang chủ reader
    @GetMapping("/home")
    public String home(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Long userId = getUserId(userDetails);
        model.addAttribute("recommendations", recommendService.recommendBooks(userId));
        model.addAttribute("username", userDetails.getUsername());
        return "reader/home";
    }

    // Danh sách sách + tìm kiếm
    @GetMapping("/books")
    public String books(
            @RequestParam(required = false) String keyword,
            Model model) {

        if (keyword != null && !keyword.isEmpty()) {
            model.addAttribute("books", bookService.searchBooks(keyword));
            model.addAttribute("keyword", keyword);
        } else {
            model.addAttribute("books", bookService.getAllBooks());
        }
        return "reader/books";
    }

    // Chi tiết sách
    @GetMapping("/books/{id}")
    public String bookDetail(@PathVariable Long id, Model model) {
        model.addAttribute("book", bookService.getBookById(id));
        return "reader/book-detail";
    }

    // Lịch sử mượn
    @GetMapping("/history")
    public String borrowHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        Long userId = getUserId(userDetails);
        model.addAttribute("records", borrowRecordService.getUserBorrowHistory(userId));
        model.addAttribute("username", userDetails.getUsername());
        return "reader/history";
    }

    // Danh sách đặt giữ
    @GetMapping("/reservations")
    public String reservations(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        Long userId = getUserId(userDetails);
        model.addAttribute("reservations",
            reservationService.getUserReservations(userId));
        return "reader/reservations";
    }

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new EntityNotFoundException("User không tồn tại"))
            .getId();
    }
}