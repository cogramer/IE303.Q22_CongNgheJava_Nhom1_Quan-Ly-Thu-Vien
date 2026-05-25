package com.library.controller.api;

import com.library.dto.BookDTO;
import com.library.service.RecommendService;
import com.library.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendApiController {

  private final RecommendService recommendService;
  private final UserService userService;

  // ==========================================
  // API CÁ NHÂN HÓA (DÀNH CHO ĐỘC GIẢ ĐÃ ĐĂNG NHẬP)
  // ==========================================

  @GetMapping
  public ResponseEntity<List<BookDTO>> getRecommendations(Authentication authentication) {
    // Trích xuất an toàn User ID từ token JWT của người đang request
    Long currentUserId = userService.getUserByUsername(authentication.getName()).getId();

    // Gọi Service lấy danh sách sách gợi ý
    List<BookDTO> recommendedBooks = recommendService.recommendBooks(currentUserId);

    return ResponseEntity.ok(recommendedBooks);
  }
}