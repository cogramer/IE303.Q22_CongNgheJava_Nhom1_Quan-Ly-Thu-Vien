package com.library.controller.api;

import com.library.dto.CategoryDTO;
import com.library.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryApiController {

  private final CategoryService categoryService;

  // ==========================================
  // CÁC API PUBLIC (AI CŨNG XEM ĐƯỢC)
  // Dành cho trang chủ, bộ lọc tìm kiếm sách...
  // ==========================================

  // Lấy danh sách toàn bộ thể loại
  @GetMapping
  public ResponseEntity<List<CategoryDTO>> getAllCategories() {
    return ResponseEntity.ok(categoryService.getAllCategories());
  }

  // Thống kê số lượng sách mỗi thể loại (để hiển thị trên menu lọc)
  @GetMapping("/stats/books-count")
  public ResponseEntity<List<CategoryDTO>> getBookCountByCategory() {
    return ResponseEntity.ok(categoryService.getBookCountByCategory());
  }

  // Thống kê thể loại HOT được mượn nhiều nhất (dành cho trang chủ Độc giả)
  @GetMapping("/stats/hot")
  public ResponseEntity<List<CategoryDTO>> getCategoryHotStats() {
    return ResponseEntity.ok(categoryService.getCategoryHotStats());
  }

  // ==========================================
  // CÁC API QUẢN TRỊ (CHỈ ADMIN & LIBRARIAN)
  // ==========================================

  // Thêm thể loại mới
  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @PostMapping
  public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
    // Đảm bảo ID = null để tạo mới
    categoryDTO.setId(null);
    return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.saveCategory(categoryDTO));
  }

  // Cập nhật thể loại
  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @PutMapping("/{id}")
  public ResponseEntity<CategoryDTO> updateCategory(
      @PathVariable Long id,
      @Valid @RequestBody CategoryDTO categoryDTO) {
    // Ép ID từ URL vào DTO để tránh việc payload truyền sai ID
    categoryDTO.setId(id);
    return ResponseEntity.ok(categoryService.saveCategory(categoryDTO));
  }

  // Xóa thể loại
  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
    categoryService.deleteCategory(id);
    return ResponseEntity.noContent().build();
  }
}