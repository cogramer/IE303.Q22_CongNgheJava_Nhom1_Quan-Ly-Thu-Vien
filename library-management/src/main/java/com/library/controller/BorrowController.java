package com.library.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import com.library.dto.BookDTO;
import com.library.dto.BorrowRecordDTO;
import com.library.dto.UserDTO;
import com.library.service.BookService;
import com.library.service.BorrowRecordService;
import com.library.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BorrowController {

    private final BookService bookService;
    private final UserService userService;
    private final BorrowRecordService borrowRecordService;

    // Điều hướng route /borrow sang trang mượn sách của reader.
    @GetMapping("/borrow")
    public String borrow() {
        return "redirect:/reader/borrow";
    }

    // Frontend borrow/history cần biết reader hiện tại để lấy lịch sử đúng user.
    @GetMapping("api/users/me")
    public ResponseEntity<UserDTO> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDTO user = userService.getUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }

    // UI borrow dùng API này để render danh sách sách có thể đặt giữ.
    @GetMapping("api/books/available")
    public ResponseEntity<List<BookDTO>> getAvailableBooks() {
        return ResponseEntity.ok(bookService.getAvailableBooks());
    }

    // Frontend borrow/history dùng API này để hiển thị sách đang mượn và lịch sử mượn.
    @GetMapping("api/users/{userId}/borrow-history")
    public ResponseEntity<?> getBorrowHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userId
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập"));
        }

        UserDTO currentUser = userService.getUserByUsername(userDetails.getUsername());
        if (!currentUser.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Không có quyền xem lịch sử của người khác"));
        }

        List<BorrowRecordDTO> history = borrowRecordService.getUserBorrowHistory(userId);
        return ResponseEntity.ok(history);
    }

    // API trả sách cũ; librarian UI hiện dùng POST /librarian/loans/return/{id}.
    // Giữ tạm để không phá client cũ nếu còn dùng.
    @PutMapping("api/borrow/return/{id}")
    public ResponseEntity<?> returnBook(@PathVariable Long id) {
        try {
            BorrowRecordDTO result = borrowRecordService.returnBook(id);
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
        }
    }
}
