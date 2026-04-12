package com.library.controller;

import com.library.dto.BookDTO;
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

    // Quản lý mượn/trả
    @GetMapping("/loans")
    public String loans(Model model) {
        model.addAttribute("overdueList", borrowService.getOverdueRecords());
        return "librarian/loans";
    }

    @PostMapping("/loans/return/{id}")
    public String returnBook(@PathVariable Long id) {
        borrowService.returnBook(id);
        return "redirect:/librarian/loans";
    }

    // Quản lý đặt giữ
    @GetMapping("/reservations")
    public String reservations(Model model) {
        model.addAttribute("reservations",
            reservationService.getPendingReservations());
        return "librarian/reservations";
    }

    // Báo cáo
    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("topCategories",
            categoryService.getCategoryHotStats());
        model.addAttribute("overdueList",
            borrowService.getOverdueRecords());
        return "librarian/reports";
    }
}