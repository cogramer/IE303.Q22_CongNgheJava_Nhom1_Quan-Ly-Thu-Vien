package com.library.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.library.dto.BorrowRecordDTO;
import com.library.model.BorrowRecord;
import com.library.model.Reservation;
import com.library.repository.UserRepository;
import com.library.service.BookService;
import com.library.service.BorrowRecordService;
import com.library.service.RecommendService;
import com.library.service.ReservationService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

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
        model.addAttribute("featuredBooks", recommendService.recommendBooks(userId)); // dùng AI làm featured
        model.addAttribute("featuredBooks", bookService.getFeaturedBooks());
        model.addAttribute("newBooks", bookService.getNewBooks());
        model.addAttribute("username", userDetails.getUsername());
        return "home";
    }

    // Danh sách sách + tìm kiếm
    @GetMapping("/books")
    public String books(
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        Long userId = getUserId(userDetails);

        // Lấy danh sách bookId đã đặt giữ PENDING
        List<Long> pendingBookIds = reservationService
            .getUserReservations(userId).stream()
            .filter(r -> r.getStatus() == Reservation.Status.PENDING)
            .map(r -> r.getBook().getId())
            .collect(Collectors.toList());

        if (keyword != null && !keyword.isEmpty()) {
            model.addAttribute("books", bookService.searchBooks(keyword));
            model.addAttribute("keyword", keyword);
        } else {
            model.addAttribute("books", bookService.getAllBooks());
        }

        model.addAttribute("featuredBooks", bookService.getFeaturedBooks());
        model.addAttribute("pendingBookIds", pendingBookIds);
        return "reader/books";
    }

    // Chi tiết sách
    @GetMapping("/books/{id}")
    public String bookDetail(@PathVariable Long id, Model model) {
        model.addAttribute("book", bookService.getBookById(id));
        return "reader/book-detail";
    }

    @GetMapping("/borrow")
    public String borrow(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        Long userId = getUserId(userDetails);
        // Lọc chỉ lấy phiếu đang mượn của user này
        List<BorrowRecordDTO> activeLoans = borrowRecordService
            .getUserBorrowHistory(userId).stream()
            .filter(r -> r.getStatus() == BorrowRecord.Status.BORROWING
                    || r.getStatus() == BorrowRecord.Status.OVERDUE)
            .collect(Collectors.toList());
        model.addAttribute("loans", activeLoans);
        model.addAttribute("username", userDetails.getUsername());
        return "reader/borrow";
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

    // Trang gợi ý sách
    @GetMapping("/recommendations")
    public String recommendations(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        Long userId = getUserId(userDetails);
        model.addAttribute("books", recommendService.recommendBooks(userId));
        model.addAttribute("username", userDetails.getUsername());
        return "reader/recommendations";
    }
}
