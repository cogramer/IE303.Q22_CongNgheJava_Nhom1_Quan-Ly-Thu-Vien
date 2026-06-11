package com.library.controller.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.library.dto.ReservationDTO;
import com.library.service.ReservationService;
import com.library.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationApiController {

  private final ReservationService reservationService;
  private final UserService userService;

  // ==========================================
  // CÁC API DÀNH CHO ĐỘC GIẢ (CÁ NHÂN)
  // ==========================================

  @PostMapping
  public ResponseEntity<?> createReservation(@RequestBody Map<String, Long> payload, Authentication authentication) {
    try {
      Long bookId = payload.get("bookId");
      if (bookId == null) {
        return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng cung cấp bookId"));
      }

      Long currentUserId = userService.getUserByUsername(authentication.getName()).getId();
      ReservationDTO reservation = reservationService.createReservation(currentUserId, bookId);
      return ResponseEntity.status(HttpStatus.CREATED).body(reservation);

    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  @PostMapping("/batch")
  public ResponseEntity<?> createReservations(@RequestBody Map<String, List<Long>> payload, Authentication authentication) {
    try {
      List<Long> bookIds = payload.get("bookIds");
      if (bookIds == null || bookIds.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng cung cấp danh sách bookId"));
      }

      Long currentUserId = userService.getUserByUsername(authentication.getName()).getId();
      List<ReservationDTO> reservations = reservationService.createReservations(currentUserId, bookIds);
      return ResponseEntity.status(HttpStatus.CREATED).body(reservations);

    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  @GetMapping("/me")
  public ResponseEntity<List<ReservationDTO>> getMyReservations(Authentication authentication) {
    Long currentUserId = userService.getUserByUsername(authentication.getName()).getId();
    return ResponseEntity.ok(reservationService.getUserReservations(currentUserId));
  }

  @PutMapping("/{id}/cancel")
  public ResponseEntity<?> cancelReservation(@PathVariable Long id, Authentication authentication) {
    try {
      // Lấy ID người đang gọi API truyền xuống Service để check quyền
      Long currentUserId = userService.getUserByUsername(authentication.getName()).getId();
      ReservationDTO cancelled = reservationService.cancelReservation(id, currentUserId);
      return ResponseEntity.ok(cancelled);
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  // ==========================================
  // CÁC API QUẢN TRỊ (CHỈ ADMIN VÀ THỦ THƯ)
  // ==========================================

  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @GetMapping
  public ResponseEntity<List<ReservationDTO>> getAllReservations() {
    return ResponseEntity.ok(reservationService.getAllReservations());
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @GetMapping("/pending")
  public ResponseEntity<List<ReservationDTO>> getPendingReservations() {
    return ResponseEntity.ok(reservationService.getPendingReservations());
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
  @PutMapping("/{id}/fulfill")
  public ResponseEntity<?> fulfillReservation(@PathVariable Long id) {
    try {
      ReservationDTO fulfilled = reservationService.fulfillReservation(id);
      return ResponseEntity.ok(fulfilled);
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }
}
