// package com.library.controller;

// import com.library.dto.BorrowRecordDTO;
// import com.library.service.BorrowRecordService;
// import com.library.service.ReservationService; // Đã bỏ comment

// import lombok.RequiredArgsConstructor;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;

// import java.util.List;
// import java.util.Map;

// @RestController
// @RequestMapping("/api/borrow-records")
// @RequiredArgsConstructor
// public class BorrowRecordController {

// private final BorrowRecordService borrowRecordService;
// private final ReservationService reservationService; // Đã bỏ comment để dùng
// cho Task 1

// // ==========================================
// // TASK 1: API TẠO PHIẾU MƯỢN TỪ RESERVATION
// // ==========================================

// @PostMapping("/librarian/loans/create")
// public ResponseEntity<?> createLoanFromReservation(@RequestBody Map<String,
// Long> payload) {
// Long reservationId = payload.get("reservationId");

// if (reservationId == null) {
// return ResponseEntity.badRequest().body(Map.of("message", "Thiếu ID phiếu đặt
// giữ (reservationId)"));
// }

// try {
// // Đã bổ sung logic: Gọi hàm fulfillReservation có sẵn trong
// ReservationService
// // Hàm này bên trong đã tự động tạo BorrowRecord và trừ số lượng sách.
// reservationService.fulfillReservation(reservationId);

// // Trả về message thành công thay vì DTO để tiết kiệm thời gian làm Mapper
// return ResponseEntity.ok(Map.of(
// "message", "Tạo phiếu mượn thành công từ Reservation ID: " + reservationId));
// } catch (Exception ex) {
// return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message",
// ex.getMessage()));
// }
// }

// // ==========================================
// // TASK 2: CÁC API QUẢN LÝ MƯỢN / TRẢ ĐẦY ĐỦ
// // ==========================================

// @PostMapping("/create")
// public ResponseEntity<?> borrowBooks(
// @RequestParam Long userId,
// @RequestBody List<Long> bookIds) {
// try {
// List<BorrowRecordDTO> records =
// borrowRecordService.borrowMultipleBooks(userId, bookIds);
// return ResponseEntity.ok(records);
// } catch (Exception ex) {
// return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
// }
// }

// @PutMapping("/return/{id}")
// public ResponseEntity<?> returnBook(@PathVariable Long id) {
// try {
// BorrowRecordDTO result = borrowRecordService.returnBook(id);
// return ResponseEntity.ok(result);
// } catch (Exception ex) {
// return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
// }
// }

// @GetMapping("/users/{userId}/history")
// public ResponseEntity<?> getUserBorrowHistory(@PathVariable Long userId) {
// try {
// List<BorrowRecordDTO> history =
// borrowRecordService.getUserBorrowHistory(userId);
// return ResponseEntity.ok(history);
// } catch (Exception ex) {
// return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message",
// ex.getMessage()));
// }
// }

// @GetMapping
// public ResponseEntity<List<BorrowRecordDTO>> getAllOrSearch(
// @RequestParam(required = false) String keyword) {
// if (keyword != null && !keyword.isBlank()) {
// return ResponseEntity.ok(borrowRecordService.searchByUsername(keyword));
// }
// return ResponseEntity.ok(borrowRecordService.getAllBorrowRecords());
// }

// @GetMapping("/overdue")
// public ResponseEntity<List<BorrowRecordDTO>> getOverdueRecords() {
// return ResponseEntity.ok(borrowRecordService.getOverdueRecords());
// }

// @GetMapping("/active")
// public ResponseEntity<List<BorrowRecordDTO>> getActiveLoans() {
// return ResponseEntity.ok(borrowRecordService.getAllActiveLoans());
// }
// }