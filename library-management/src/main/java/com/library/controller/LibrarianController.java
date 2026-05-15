package com.library.controller;

import com.library.dto.BookDTO;
import com.library.dto.UserDTO;
import com.library.service.BookService;
import com.library.service.BorrowRecordService;
import com.library.service.CategoryService;
import com.library.service.ReservationService;
import com.library.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/librarian")
public class LibrarianController {

    private final BookService bookService;
    private final CategoryService categoryService;
    private final BorrowRecordService borrowService;
    private final ReservationService reservationService;
    private final UserService userService;

    // Dashboard
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalBooks", bookService.getAllBooks().size());
        model.addAttribute("totalUsers", userService.getAllUsers().size());
        model.addAttribute("overdueList", borrowService.getOverdueRecords());
        model.addAttribute("pendingReservations",
            reservationService.getPendingReservations());
        model.addAttribute("recentActivity", borrowService.getRecentActivity());
        model.addAttribute("topBorrowedBooks", borrowService.getTopBorrowedBooks());
        model.addAttribute("newBooks", bookService.getNewBooks());
        model.addAttribute("borrowByMonth", borrowService.getBorrowCountByMonth(2026));
        model.addAttribute("borrowingRate", bookService.getBorrowingRate());
        model.addAttribute("topCategories", categoryService.getCategoryHotStats());
        return "librarian/dashboard";
    }

    // Quản lý sách
    @GetMapping("/books")
    public String books(Model model) {
        model.addAttribute("books", bookService.getAllBooks());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "librarian/books";
    }

    @PostMapping("/books/save")
    public String saveBook(@ModelAttribute BookDTO bookDTO) {
        bookService.saveBook(bookDTO);
        return "redirect:/librarian/books";
    }

    @PostMapping("/books/delete/{id}")
    public String deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return "redirect:/librarian/books";
    }

    // UI librarian/users cần search theo tên và danh sách toàn bộ user.
    @GetMapping("/users")
    public String users(
            @RequestParam(required = false) String keyword,
            Model model) {
        if (keyword != null && !keyword.isBlank()) {
            model.addAttribute("users", userService.searchUsersByName(keyword));
            model.addAttribute("keyword", keyword);
        } else {
            model.addAttribute("users", userService.getAllUsers());
        }
        return "librarian/users";
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/librarian/users";
    }

    // UI librarian/users dùng modal sửa user và submit về endpoint này.
    @PostMapping("/users/update/{id}")
    public String updateUser(@PathVariable Long id, @ModelAttribute UserDTO userDTO) {
        userService.updateUser(id, userDTO);
        return "redirect:/librarian/users";
    }

    // UI librarian/loans cần tất cả phiếu, gồm BORROWING, OVERDUE và RETURNED.
    // Search/filter trạng thái/ngày hiện xử lý phía frontend bằng librarian-loans.js.
    @GetMapping("/loans")
    public String loans(
            @RequestParam(required = false) String keyword,
            Model model) {
        model.addAttribute("loans", borrowService.getAllBorrowRecords());
        model.addAttribute("keyword", keyword);
        model.addAttribute("overdueList", borrowService.getOverdueRecords());
        return "librarian/loans";
    }

    @PostMapping("/loans/return/{id}")
    public String returnBook(@PathVariable Long id) {
        borrowService.returnBook(id);
        return "redirect:/librarian/loans";
    }

    // UI librarian/reservations cần xem cả PENDING, FULFILLED, CANCELLED, NOTIFIED.
    @GetMapping("/reservations")
    public String reservations(Model model) {
        model.addAttribute("reservations",
            reservationService.getAllReservations());
        return "librarian/reservations";
    }

    // UI librarian/reports cần dữ liệu tổng hợp riêng, không dùng chung model dashboard.
    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("totalBooks", bookService.getAllBooks().size());
        model.addAttribute("loans", borrowService.getAllBorrowRecords());
        model.addAttribute("topBorrowedBooks", borrowService.getTopBorrowedBooks());
        model.addAttribute("borrowByMonth", borrowService.getBorrowCountByMonth(2026));
        model.addAttribute("topCategories",
            categoryService.getCategoryHotStats());
        model.addAttribute("overdueList",
            borrowService.getOverdueRecords());
        return "librarian/reports";
    }
}
