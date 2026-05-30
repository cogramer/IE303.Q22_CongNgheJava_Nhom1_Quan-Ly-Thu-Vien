package com.library.controller.api;

import com.library.dto.FeedbackDTO;
import com.library.model.User;
import com.library.service.FeedbackService;
import com.library.service.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
public class FeedbackApiController {

  private final FeedbackService feedbackService;
  private final UserService userService;

  // ==========================================
  // CÁC API PUBLIC (AI CŨNG XEM ĐƯỢC)
  // ==========================================

  // Lấy danh sách đánh giá của một cuốn sách cụ thể
  @GetMapping("/books/{bookId}")
  public ResponseEntity<Page<FeedbackDTO.Response>> getFeedbackByBookId(@PathVariable Long bookId, @RequestParam(defaultValue = "0") Long page, @RequestParam(defaultValue = "6") Long size, @RequestParam(defaultValue = "desc") String sortDir) {
    return ResponseEntity.ok(feedbackService.getFeedbackByBookId(bookId, page, size, sortDir));
  }

  // ==========================================
  // CÁC API DÀNH CHO ĐỘC GIẢ (YÊU CẦU ĐĂNG NHẬP)
  // ==========================================

  // 1. Lấy danh sách đánh giá/tương tác của chính mình
  @GetMapping("/me")
  public ResponseEntity<Page<FeedbackDTO.Response>> getMyFeedbacks(Authentication authentication, @RequestParam(defaultValue = "0") Long page, @RequestParam(defaultValue = "10") Long size, @RequestParam(defaultValue = "desc") String sortDir) {
    Long currentUserId = userService.getUserByUsername(authentication.getName()).getId();
    return ResponseEntity.ok(feedbackService.getFeedbackByUserId(currentUserId, page, size, sortDir));
  }

  // 2. Tạo đánh giá (RATING) cho sách
  @PostMapping
  public ResponseEntity<?> createFeedback(@RequestBody FeedbackDTO.CreateRequest request,
      Authentication authentication) {
    try {
      Long currentUserId = userService.getUserByUsername(authentication.getName()).getId();
      FeedbackDTO.Response response = feedbackService.createFeedback(currentUserId, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (IllegalArgumentException | EntityNotFoundException e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  // 3. Sửa đánh giá
  @PutMapping("/{id}")
  public ResponseEntity<?> updateFeedback(
      @PathVariable Long id,
      @RequestBody FeedbackDTO.UpdateRequest request,
      Authentication authentication) {
    try {
      Long currentUserId = userService.getUserByUsername(authentication.getName()).getId();
      boolean isStaff = isStaffMember(authentication);

      FeedbackDTO.Response response = feedbackService.updateFeedback(id, currentUserId, isStaff, request);
      return ResponseEntity.ok(response);
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  // 4. Xóa đánh giá (Admin/Thủ thư có thể xóa của ai cũng được, Độc giả chỉ được
  // xóa của mình)
  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteFeedback(@PathVariable Long id, Authentication authentication) {
    try {
      Long currentUserId = userService.getUserByUsername(authentication.getName()).getId();
      boolean isStaff = isStaffMember(authentication);

      feedbackService.deleteRatingFeedback(id, currentUserId, isStaff);
      return ResponseEntity.noContent().build();
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  @GetMapping("/books/{bookId}/average-score")
  public ResponseEntity<Map<String, Object>> getAverageScoreForBook(@PathVariable Long bookId) {
      double averageScore = feedbackService.getAverageScoreForBook(bookId);

      Map<String, Object> response = new HashMap<>();
      response.put("bookId", bookId);
      response.put("averageScore", averageScore);

      return ResponseEntity.ok(response);
  }

  // ==========================================
  // HÀM HỖ TRỢ (HELPER METHOD)
  // ==========================================

  // Kiểm tra xem User đang đăng nhập có phải là LIBRARIAN hoặc ADMIN không
  private boolean isStaffMember(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_LIBRARIAN"));
  } 
}