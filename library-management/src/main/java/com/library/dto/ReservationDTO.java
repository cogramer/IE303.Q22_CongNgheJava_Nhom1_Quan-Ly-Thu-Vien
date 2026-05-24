package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

  // Trạng thái (PENDING, FULFILLED, CANCELLED)
  private String status;
}