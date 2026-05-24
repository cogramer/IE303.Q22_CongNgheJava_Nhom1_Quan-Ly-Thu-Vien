package com.library.controller.api;

import com.library.dto.BorrowRecordDTO;
import com.library.service.BorrowRecordService;
import com.library.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/borrow")
@RequiredArgsConstructor
public class BorrowRecordApiController {

  private final BorrowRecordService borrowService;
  private final UserService userService;

  // ==========================================
  // CÁC API DÀNH CHO ĐỘC GIẢ (CÁ NHÂN)
  // ==========================================

  // 1. Độc giả tự mượn sách trực tiếp (Giả định thư viện cho phép Self-checkout)
  @PostMapping
  public ResponseEntity<?> borrowBooks(@RequestBody List<Long> bookIds, Authentication authentication) {
    try {
      if (bookIds == null || bookIds.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("message", "Danh sách sách không được để trống!"));
      }
      Long currentUserId = userService.getUserByUsername(authentication.getName()).getId();
      List<BorrowRecordDTO> records = borrowService.borrowMultipleBooks(currentUserId, bookIds);

      return ResponseEntity.status(HttpStatus.CREATED).body(records);
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  // 2. Lấy lịch sử mượn trả của CHÍNH MÌNH
  @GetMapping("/me")
  public ResponseEntity<List<BorrowRecordDTO>> getMyBorrowHistory(Authentication authentication) {
    Long currentUserId = userService.getUserByUsername(authentication.getName()).getId();
    return ResponseEntity.ok(borrowService.getUserBorrowHistory(currentUserId));
  }

  // ==========================================
  // CÁC API QUẢN TRỊ (CHỈ ADMIN VÀ THỦ THƯ)
  // ==========================================

  // 3. Trả sách (Thủ thư quét mã / nhập ID phiếu để trả)
  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @PutMapping("/{id}/return")
  public ResponseEntity<?> returnBook(@PathVariable Long id) {
    try {
      BorrowRecordDTO returnedRecord = borrowService.returnBook(id);
      return ResponseEntity.ok(returnedRecord);
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  // 4. Lấy tất cả các phiếu mượn (đang mượn, quá hạn, đã trả)
  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @GetMapping("/all")
  public ResponseEntity<List<BorrowRecordDTO>> getAllBorrowRecords() {
    return ResponseEntity.ok(borrowService.getAllBorrowRecords());
  }

  // 5. Lấy danh sách các phiếu đang hoạt động (BORROWING, OVERDUE)
  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @GetMapping("/active")
  public ResponseEntity<List<BorrowRecordDTO>> getAllActiveLoans() {
    return ResponseEntity.ok(borrowService.getAllActiveLoans());
  }

  // 6. Lấy danh sách phiếu ĐÃ QUÁ HẠN
  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @GetMapping("/overdue")
  public ResponseEntity<List<BorrowRecordDTO>> getOverdueRecords() {
    return ResponseEntity.ok(borrowService.getOverdueRecords());
  }

  // 7. Tìm kiếm phiếu mượn theo tên độc giả
  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @GetMapping("/search")
  public ResponseEntity<List<BorrowRecordDTO>> searchByUsername(@RequestParam(required = false) String keyword) {
    return ResponseEntity.ok(borrowService.searchByUsername(keyword));
  }

  // 8. Kích hoạt cập nhật trạng thái QUÁ HẠN bằng tay (Manual Trigger)
  // Thực tế hàm này nên chạy ngầm bằng @Scheduled(cron = "0 0 0 * * ?"), nhưng
  // đưa ra API để Admin dễ test
  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @PostMapping("/trigger-overdue-check")
  public ResponseEntity<?> triggerOverdueCheck() {
    borrowService.updateOverdueStatus();
    return ResponseEntity.ok(Map.of("message", "Đã cập nhật các phiếu quá hạn thành công!"));
  }

  // ==========================================
  // CÁC API THỐNG KÊ (DÀNH CHO DASHBOARD QUẢN TRỊ)
  // ==========================================

  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @GetMapping("/stats/recent")
  public ResponseEntity<List<BorrowRecordDTO>> getRecentActivity() {
    return ResponseEntity.ok(borrowService.getRecentActivity());
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @GetMapping("/stats/top-books")
  public ResponseEntity<List<Map<String, Object>>> getTopBorrowedBooks() {
    return ResponseEntity.ok(borrowService.getTopBorrowedBooks());
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @GetMapping("/stats/monthly")
  public ResponseEntity<Map<Integer, Long>> getBorrowCountByMonth(@RequestParam int year) {
    return ResponseEntity.ok(borrowService.getBorrowCountByMonth(year));
  }
}