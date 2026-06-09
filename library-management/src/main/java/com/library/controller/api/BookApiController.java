package com.library.controller.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.library.dto.BookDTO;
import com.library.service.BookService;
import com.library.service.ImageStorageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookApiController {

  private final BookService bookService;
  private final ImageStorageService imageStorageService;

  // ==========================================
  // CÁC API PUBLIC (KHÁCH VÃNG LAI, ĐỘC GIẢ ĐỀU XEM ĐƯỢC)
  // Đã cấu hình permitAll() trong SecurityConfig
  // ==========================================

  // Lấy toàn bộ sách
  @GetMapping
  public ResponseEntity<List<BookDTO>> getAllBooks() {
    return ResponseEntity.ok(bookService.getAllBooks());
  }

  // Lấy chi tiết một cuốn sách theo ID
  @GetMapping("/{id}")
  public ResponseEntity<BookDTO> getBookById(@PathVariable Long id) {
    return ResponseEntity.ok(bookService.getBookById(id));
  }

  // Tìm kiếm sách theo tên hoặc tác giả
  @GetMapping("/search")
  public ResponseEntity<List<BookDTO>> searchBooks(@RequestParam String keyword) {
    return ResponseEntity.ok(bookService.searchBooks(keyword));
  }

  // Lấy danh sách sách nổi bật
  @GetMapping("/featured")
  public ResponseEntity<List<BookDTO>> getFeaturedBooks() {
    return ResponseEntity.ok(bookService.getFeaturedBooks());
  }

  // Lấy danh sách sách mới nhất
  @GetMapping("/new")
  public ResponseEntity<List<BookDTO>> getNewBooks() {
    return ResponseEntity.ok(bookService.getNewBooks());
  }

  // Lấy danh sách sách còn sẵn trong kho
  @GetMapping("/available")
  public ResponseEntity<List<BookDTO>> getAvailableBooks() {
    return ResponseEntity.ok(bookService.getAvailableBooks());
  }

  // ==========================================
  // CÁC API QUẢN TRỊ (CHỈ ADMIN VÀ THỦ THƯ ĐƯỢC THAO TÁC)
  // ==========================================

  // Thêm sách mới (Yêu cầu validate dữ liệu đầu vào)
  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @PostMapping
  public ResponseEntity<BookDTO> createBook(@Valid @RequestBody BookDTO bookDTO) {
    // Đảm bảo ID là null để JPA hiểu là tạo mới
    bookDTO.setId(null);
    return ResponseEntity.status(HttpStatus.CREATED).body(bookService.saveBook(bookDTO));
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BookDTO> createBookWithImage(
      @Valid @ModelAttribute BookDTO bookDTO,
      @RequestParam(name = "bookImage", required = false) MultipartFile bookImage) {
    bookDTO.setId(null);
    String uploadedImageUrl = imageStorageService.storeBookImage(bookImage);
    if (uploadedImageUrl != null) {
      bookDTO.setImageUrl(uploadedImageUrl);
    }
    return ResponseEntity.status(HttpStatus.CREATED).body(bookService.saveBook(bookDTO));
  }

  // Cập nhật thông tin sách
  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @PutMapping("/{id}")
  public ResponseEntity<BookDTO> updateBook(@PathVariable Long id, @Valid @RequestBody BookDTO bookDTO) {
    // Gắn ID từ URL vào DTO để Service đè lên bản ghi cũ
    bookDTO.setId(id);
    return ResponseEntity.ok(bookService.saveBook(bookDTO));
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BookDTO> updateBookWithImage(
      @PathVariable Long id,
      @Valid @ModelAttribute BookDTO bookDTO,
      @RequestParam(name = "bookImage", required = false) MultipartFile bookImage) {
    bookDTO.setId(id);
    String uploadedImageUrl = imageStorageService.storeBookImage(bookImage);
    if (uploadedImageUrl != null) {
      bookDTO.setImageUrl(uploadedImageUrl);
    }
    return ResponseEntity.ok(bookService.saveBook(bookDTO));
  }

  // Xóa sách
  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteBook(@PathVariable Long id) {
    try {
      bookService.deleteBook(id);
      return ResponseEntity.noContent().build();
    } catch (DataIntegrityViolationException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("message", "Khong the xoa sach nay vi sach dang co phieu muon, dat giu hoac danh gia lien quan."));
    }
  }

  // Cập nhật nhanh tổng số lượng sách khi nhập kho thêm
  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @PatchMapping("/{id}/total-copies")
  public ResponseEntity<BookDTO> updateTotalCopies(
      @PathVariable Long id,
      @RequestParam int newTotal) {
    return ResponseEntity.ok(bookService.updateTotalCopies(id, newTotal));
  }

  // Xem thống kê tỷ lệ mượn sách
  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @GetMapping("/stats/borrowing-rate")
  public ResponseEntity<Map<String, Object>> getBorrowingRate() {
    return ResponseEntity.ok(bookService.getBorrowingRate());
  }
}
