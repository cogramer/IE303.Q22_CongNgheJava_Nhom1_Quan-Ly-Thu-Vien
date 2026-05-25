package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate; // Import thêm thư viện thời gian tương ứng với Service

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDTO {
  private Long id;

  // Thông tin người đặt
  private Long userId;
  private String username;
  private String fullName;

  // Thông tin sách
  private Long bookId;
  private String bookTitle;
  private Integer availableCopies; // 1. Bổ sung số lượng tồn kho

  // Trạng thái (PENDING, FULFILLED, CANCELLED)
  private String status;

  private LocalDate reservationDate; // 2. Bổ sung ngày đặt giữ (dùng LocalDate hoặc LocalDateTime tùy thuộc vào
                                     // Entity)
}