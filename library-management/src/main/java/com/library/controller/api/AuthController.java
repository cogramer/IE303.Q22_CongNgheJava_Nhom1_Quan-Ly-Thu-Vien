package com.library.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.library.dto.AuthDTO.*;
import com.library.enums.RegisterResult;
import com.library.service.AuthService;
import com.library.service.UserService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  @Autowired
  private AuthService authService;

  @Autowired
  private UserService userService;

  // ⛔ ĐÃ XÓA: Hàm login() và JwtUtil.
  // Lý do: Spring Security đã tự động xử lý POST /login qua cấu hình .formLogin()
  // Nó sẽ tự động kiểm tra username/password, tạo Session và trả về cookie
  // JSESSIONID.

  // --- 1. ĐĂNG KÝ ĐỘC GIẢ (READER) ---
  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
    // Gọi hàm addNewUser từ UserService với các trường từ request
    RegisterResult result = userService.addNewUser(
        request.getUsername(),
        request.getPassword(),
        request.getEmail(),
        request.getFullName());

    // Xử lý các trường hợp lỗi trùng lặp
    if (result == RegisterResult.USERNAME_EXIST) {
      return ResponseEntity
          .status(HttpStatus.BAD_REQUEST)
          .body(Map.of("message", "Tên đăng nhập đã tồn tại!"));
    } else if (result == RegisterResult.EMAIL_EXIST) {
      return ResponseEntity
          .status(HttpStatus.BAD_REQUEST)
          .body(Map.of("message", "Email đã được sử dụng!"));
    }

    // Trường hợp RegisterResult.SUCCESS
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
        "message", "Đăng ký tài khoản độc giả thành công!"));
  }

  // --- 2. QUÊN MẬT KHẨU (GỬI OTP) ---
  @PostMapping("/forgot-password")
  public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
    // Sinh OTP
    String otp = authService.generateOTP();

    // Cần lưu OTP vào Cache/Redis hoặc Database với thời hạn 5 phút (tùy logic của
    // bạn)
    // userService.saveResetOtp(request.getEmail(), otp);

    // Gửi email bất đồng bộ
    authService.sendChangePasswordOtp(request.getEmail(), otp);

    return ResponseEntity.ok(Map.of(
        "message", "Mã OTP đã được gửi đến email của bạn."));
  }
}