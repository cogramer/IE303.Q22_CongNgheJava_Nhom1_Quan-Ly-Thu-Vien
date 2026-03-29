package com.library.dto;

import com.library.model.User.Role;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

  private Long id;

  @NotBlank(message = "Tên đăng nhập không được để trống")
  @Size(min = 4, max = 50, message = "Tên đăng nhập phải từ 4-50 ký tự")
  private String username;

  // Chỉ dùng khi nhận dữ liệu (Request), không trả về (Response)
  @NotBlank(message = "Mật khẩu không được để trống")
  @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
  private String password;

  @NotBlank(message = "Email không được để trống")
  @Email(message = "Email không đúng định dạng")
  private String email;

  private String fullName;

  private Role role;

  private LocalDateTime createdAt;

  // Thêm trường này để thống kê nhanh nếu cần
  private int totalBorrowCount;
}