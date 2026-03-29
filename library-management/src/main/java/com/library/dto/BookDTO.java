package com.library.dto;

import jakarta.validation.constraints.*; // Import các ràng buộc
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO {

  private Long id;

  @NotBlank(message = "Tiêu đề sách không được để trống")
  @Size(max = 255, message = "Tiêu đề không được quá 255 ký tự")
  private String title;

  @NotBlank(message = "Tên tác giả không được để trống")
  private String author;

  @NotBlank(message = "Mã ISBN không được để trống")
  @Pattern(regexp = "^(97[89])?\\d{9}(\\d|X)$", message = "Mã ISBN không đúng định dạng (10 hoặc 13 số)")
  private String isbn;

  @Min(value = 0, message = "Tổng số lượng sách không được nhỏ hơn 0")
  private int totalCopies;

  @Min(value = 0, message = "Số lượng sách sẵn có không được nhỏ hơn 0")
  private int availableCopies;

  @NotNull(message = "Phải chọn một thể loại cho sách")
  private Long categoryId;

  private String categoryName; // Thường chỉ dùng để hiển thị nên không cần ràng buộc khi nhận vào

  private LocalDateTime createdAt;
}