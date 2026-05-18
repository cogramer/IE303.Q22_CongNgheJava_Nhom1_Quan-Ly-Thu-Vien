package com.library.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.library.dto.FeedbackDTO;
import com.library.dto.UserDTO;
import com.library.model.User;
import com.library.service.FeedbackService;
import com.library.service.UserService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final UserService userService;

    @PostMapping("feedback")
    public ResponseEntity<?> createFeedback(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody FeedbackDTO.CreateRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập"));
        }

        try {
            UserDTO currentUser = userService.getUserByUsername(userDetails.getUsername());
            FeedbackDTO.Response feedback = feedbackService.createFeedback(currentUser.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(feedback);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/feedback/me")
    public ResponseEntity<?> getMyFeedback(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập"));
        }

        try {
            UserDTO currentUser = userService.getUserByUsername(userDetails.getUsername());
            return ResponseEntity.ok(feedbackService.getFeedbackByUserId(currentUser.getId()));
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/books/{bookId}/feedback")
    public ResponseEntity<?> getBookFeedback(@PathVariable Long bookId) {
        try {
            return ResponseEntity.ok(feedbackService.getFeedbackByBookId(bookId));
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/users/{userId}/feedback")
    public ResponseEntity<?> getUserFeedback(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userId) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập"));
        }

        try {
            UserDTO currentUser = userService.getUserByUsername(userDetails.getUsername());
            if (!isStaff(currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Chỉ admin hoặc librarian được xem feedback của user khác"));
            }

            return ResponseEntity.ok(feedbackService.getFeedbackByUserId(userId));
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/feedback/{id}")
    public ResponseEntity<?> updateFeedback(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody FeedbackDTO.UpdateRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập"));
        }

        try {
            UserDTO currentUser = userService.getUserByUsername(userDetails.getUsername());
            FeedbackDTO.Response feedback = feedbackService.updateFeedback(id, currentUser.getId(), isStaff(currentUser), request);
            return ResponseEntity.ok(feedback);
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
        }
    }

    @DeleteMapping("/feedback/{id}")
    public ResponseEntity<?> deleteFeedback(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập"));
        }

        try {
            UserDTO currentUser = userService.getUserByUsername(userDetails.getUsername());
            feedbackService.deleteRatingFeedback(id, currentUser.getId(), isStaff(currentUser));
            return ResponseEntity.ok(Map.of("message", "Xóa feedback thành công"));
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
        }
    }

    private boolean isStaff(UserDTO user) {
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.LIBRARIAN;
    }
}
