package com.library.controller.api;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.library.dto.AuthDTO.ForgotPasswordRequest;
import com.library.dto.AuthDTO.RegisterRequest;
import com.library.dto.AuthDTO.ResendOtpRequest;
import com.library.dto.AuthDTO.ResetPasswordRequest;
import com.library.dto.AuthDTO.VerifyOtpRequest;
import com.library.enums.RegisterResult;
import com.library.enums.Result;
import com.library.service.AuthService;
import com.library.service.BookService;
import com.library.service.UserService;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  @Autowired
  private AuthService authService;

  @Autowired
  private UserService userService;
  
  @Autowired
  private BookService bookService;

  private static final String OTP_SERVER = "otp_server";
  private static final String OTP_CREATION_TIME = "otpCreationTime";
  private static final String OTP_LAST_SENT_TIME = "otpLastSentTime";
  private static final String EMAIL_CHANGE = "emailChange";
  private static final String VERIFICATION_TYPE = "verificationType";
  private static final String OTP_VERIFIED = "otpVerified";
  private static final String OTP_VERIFIED_EMAIL = "otpVerifiedEmail";
  private static final String OTP_VERIFIED_TYPE = "otpVerifiedType";
  private static final long OTP_RESEND_COOLDOWN = 60 * 1000; // 1 phút chờ gửi lại
  private static final long OTP_EXPIRATION_TIME = 5 * 60 * 1000; // 5 phút hết hạn mã

  // ⛔ ĐÃ XÓA: Hàm login() và JwtUtil.
  // Lý do: Spring Security đã tự động xử lý POST /login qua cấu hình .formLogin()
  // Nó sẽ tự động kiểm tra username/password, tạo Session và trả về cookie
  // JSESSIONID.
  
  // Xử lý đăng ký user mới và kiểm tra trùng username/email.
  @PostMapping("/register")
  public ResponseEntity<?> registerProcess(@RequestBody RegisterRequest registerRequest, HttpServletResponse response) {
      RegisterResult registerResult = userService.addNewUser(registerRequest.getUsername(),
                              registerRequest.getPassword(),
                              registerRequest.getEmail(),
                              registerRequest.getFullName()
                          );

      if (registerResult == RegisterResult.USERNAME_EXIST) {
          return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(Map.of("message", "Tên người đã dùng tồn tại!"));
      } else if (registerResult == RegisterResult.EMAIL_EXIST) {
          return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(Map.of("message", "Email đã tồn tại!"));
      }
      return ResponseEntity.ok(Map.of("message", "Đăng ký thành công"));
  }
  
  // Tạo OTP và gửi email xác minh để đặt lại mật khẩu.
  @PostMapping("/forgot-password-process")
  public ResponseEntity<?> forgetPasswordProcess(@RequestBody ForgotPasswordRequest forgotPasswordRequest, HttpServletRequest request) throws MessagingException {
      HttpSession session = request.getSession();

      Long lastSentTime = (Long) session.getAttribute(OTP_LAST_SENT_TIME);
      long currentTime = System.currentTimeMillis();

      if (lastSentTime != null && (currentTime - lastSentTime < OTP_RESEND_COOLDOWN)) {
          long waitSeconds = (OTP_RESEND_COOLDOWN - (currentTime - lastSentTime)) / 1000;
          return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(Map.of("message", "Vui lòng chờ " + waitSeconds + " giây trước khi gửi lại mã!"));
      }

      Result checkEmailResult = userService.checkEmail(forgotPasswordRequest.getUsername(), forgotPasswordRequest.getEmail());
      if(checkEmailResult == Result.EMAIL_NOT_FOUND) {
          return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(Map.of("message", "Email này chưa đăng ký trên hệ thống!"));
      } else if (checkEmailResult == Result.USERNAME_NOT_MATCH) {
          return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(Map.of("message", "Username và email không khớp nhau!"));
      }

      String otp = authService.generateOTP();

      session.setAttribute(OTP_SERVER, otp);
      session.setAttribute(EMAIL_CHANGE, forgotPasswordRequest.getEmail());
      session.setAttribute(OTP_CREATION_TIME, System.currentTimeMillis());
      session.setAttribute(VERIFICATION_TYPE, "FORGOT_PASS");
      session.removeAttribute(OTP_VERIFIED);
      session.removeAttribute(OTP_VERIFIED_EMAIL);
      session.removeAttribute(OTP_VERIFIED_TYPE);

      authService.sendChangePasswordOtp(forgotPasswordRequest.getEmail(), otp);
      session.setAttribute(OTP_LAST_SENT_TIME, currentTime);

      return ResponseEntity.ok(Map.of("message", "Mã xác minh đang được gửi vào email!"));
  }
  
  // Xác thực mã OTP trong session.
  @PostMapping("/verify-otp")
  public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest verifyOtpRequest, HttpServletRequest request) {
      HttpSession session = request.getSession();
      String userOtp = verifyOtpRequest.getOtp();
      String serverOtp = (String) session.getAttribute(OTP_SERVER);
      Long otpCreationTime = (Long) session.getAttribute(OTP_CREATION_TIME);
      String email = (String) session.getAttribute(EMAIL_CHANGE);
      String verificationType = (String) session.getAttribute(VERIFICATION_TYPE);
      if (serverOtp == null || otpCreationTime == null || email == null || verificationType == null) {
          return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(Map.of("message", "Mã OTP không tồn tại hoặc đã bị hủy. Vui lòng gửi lại mã mới!"));
      }
      long currentTime = System.currentTimeMillis();
      if (currentTime - otpCreationTime > OTP_EXPIRATION_TIME) {
          session.removeAttribute(OTP_SERVER);
          session.removeAttribute(OTP_CREATION_TIME);
          return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(Map.of("message", "Mã OTP đã hết hạn. Vui lòng nhận mã mới!"));
      }
      if (!serverOtp.equals(userOtp)) {
          return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(Map.of("message", "Mã OTP không đúng!"));
      }
      session.removeAttribute(OTP_SERVER);
      session.removeAttribute(OTP_CREATION_TIME);
      session.setAttribute(OTP_VERIFIED, true);
      session.setAttribute(OTP_VERIFIED_EMAIL, email);
      session.setAttribute(OTP_VERIFIED_TYPE, verificationType);
      return ResponseEntity.ok(Map.of("message", "Xác thực OTP thành công!"));
  }
  
  // Đặt lại mật khẩu sau khi OTP đã được xác thực.
  @PostMapping({"/reset-password"})
  public ResponseEntity<?> resetPasswordProcess(@RequestBody ResetPasswordRequest resetPasswordRequest, HttpServletRequest request) {
      HttpSession session = request.getSession();
      Boolean otpVerified = (Boolean) session.getAttribute(OTP_VERIFIED);
      String verifiedEmail = (String) session.getAttribute(OTP_VERIFIED_EMAIL);
      String verifiedType = (String) session.getAttribute(OTP_VERIFIED_TYPE);
      if (otpVerified == null || !otpVerified || verifiedEmail == null || verifiedType == null) {
          return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(Map.of("message", "Bạn chưa xác thực OTP thành công!"));
      }
      if (!"FORGOT_PASS".equals(verifiedType) || !verifiedEmail.equals(resetPasswordRequest.getEmail())) {
          return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(Map.of("message", "Phiên xác thực không hợp lệ!"));
      }

      Result resetPassResult = userService.resetPassword(
          resetPasswordRequest.getEmail(),
          resetPasswordRequest.getNewPassword()
      );

      if (resetPassResult == Result.SUCCESS) {
          session.removeAttribute(OTP_VERIFIED);
          session.removeAttribute(OTP_VERIFIED_EMAIL);
          session.removeAttribute(OTP_VERIFIED_TYPE);
          session.removeAttribute(OTP_SERVER);
          session.removeAttribute(OTP_CREATION_TIME);
          session.removeAttribute(OTP_LAST_SENT_TIME);
          session.removeAttribute(EMAIL_CHANGE);
          session.removeAttribute(VERIFICATION_TYPE);

          return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công!"));
      }

      return ResponseEntity
          .status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("message", "Đổi mật khẩu thất bại do lỗi hệ thống!"));
  }
  
      
  // Gửi lại OTP và kiểm tra thời gian chờ giữa các lần gửi.
  @PostMapping("/resend-otp")
  public ResponseEntity<?> resendOtp(
          @RequestBody ResendOtpRequest resendOtpRequest,
          HttpServletRequest request) throws MessagingException {
      HttpSession session = request.getSession();
      Long lastSentTime = (Long) session.getAttribute(OTP_LAST_SENT_TIME);
      long currentTime = System.currentTimeMillis();
      if (lastSentTime != null && (currentTime - lastSentTime < OTP_RESEND_COOLDOWN)) {
          long waitSeconds = (OTP_RESEND_COOLDOWN - (currentTime - lastSentTime)) / 1000;
          return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(Map.of("message", "Vui lòng chờ " + waitSeconds + " giây trước khi gửi lại mã!"));
      }
      Result checkEmailResult = userService.checkEmail(
          resendOtpRequest.getUsername(),
          resendOtpRequest.getEmail()
      );
      if (checkEmailResult == Result.EMAIL_NOT_FOUND) {
          return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(Map.of("message", "Email này chưa đăng ký trên hệ thống!"));
      } else if (checkEmailResult == Result.USERNAME_NOT_MATCH) {
          return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(Map.of("message", "Username và email không khớp nhau!"));
      }
      String otp = authService.generateOTP();
      session.setAttribute(OTP_SERVER, otp);
      session.setAttribute(EMAIL_CHANGE, resendOtpRequest.getEmail());
      session.setAttribute(OTP_CREATION_TIME, System.currentTimeMillis());
      session.setAttribute(VERIFICATION_TYPE, "FORGOT_PASS");
      session.setAttribute(OTP_LAST_SENT_TIME, currentTime);
      session.removeAttribute(OTP_VERIFIED);
      session.removeAttribute(OTP_VERIFIED_EMAIL);
      session.removeAttribute(OTP_VERIFIED_TYPE);
      authService.sendChangePasswordOtp(resendOtpRequest.getEmail(), otp);
      return ResponseEntity.ok(Map.of("message", "Mã xác minh đang được gửi lại vào email!"));
  }
}