package com.library.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.library.dto.AuthDTO.ForgotPasswordRequest;
import com.library.dto.AuthDTO.LoginRequest;
import com.library.dto.AuthDTO.RegisterRequest;
import com.library.dto.AuthDTO.ResendOtpRequest;
import com.library.dto.AuthDTO.ResetPasswordRequest;
import com.library.dto.AuthDTO.VerifyOtpRequest;
import com.library.dto.UserDTO;
import com.library.enums.LoginResult;
import com.library.enums.RegisterResult;
import com.library.enums.Result;
import com.library.model.RememberMeToken;
import com.library.model.User;
import com.library.security.JwtUtil;
import com.library.service.AuthService;
import com.library.service.BookService;
import com.library.service.RememberMeService;
import com.library.service.UserService;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {
    @Autowired
    private AuthService authService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserService userService;
    @Autowired
    private BookService bookService;
    @Autowired
    private RememberMeService rememberMeService;

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

    // Hiển thị trang đăng nhập.
    @GetMapping("/login")
    public String loginPage(HttpServletRequest request) {
        return "login";
    }


    // Xử lý đăng nhập, tạo JWT cookie và remember-me nếu được chọn.
    @PostMapping("api/auth/login-process")
    public ResponseEntity<?> loginProcess(@RequestBody LoginRequest loginRequest, HttpServletResponse response, HttpServletRequest request) {
        LoginResult loginResult = authService.checkAccount(loginRequest.getUsername(), loginRequest.getPassword());

        if (loginResult == LoginResult.USERNAME_NOT_FOUND) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Tên đăng nhập không tồn tại!"));
        } else if (loginResult == LoginResult.PASSWORD_NOT_MATCH) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Sai mật khẩu!"));
        }

        UserDTO user = userService.getUserByUsername(loginRequest.getUsername());

        // --- 1. LUÔN CẤP JWT_TOKEN (Chìa khóa chính - Session cookie) ---
        String jwtToken = jwtUtil.generateToken(user.getUsername());

        Cookie jwtCookie = new Cookie("JWT_TOKEN", jwtToken);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(-1); // Xóa khi đóng trình duyệt
        response.addCookie(jwtCookie);

        // --- 2. NẾU CHỌN REMEMBER ME -> CẤP THÊM TOKEN LƯU DB ---
        String isRemember = String.valueOf(loginRequest.getRememberMe());

        if ("true".equalsIgnoreCase(isRemember)) {
            String rmTokenValue = java.util.UUID.randomUUID().toString();
            
            RememberMeToken rmt = new RememberMeToken();
            rmt.setToken(rmTokenValue);
            rmt.setExpiryDate(LocalDateTime.now().plusDays(7));
            rememberMeService.save(rmt, user.getId());

            // Gửi Cookie REMEMBER_ME về trình duyệt
            Cookie rmCookie = new Cookie("REMEMBER_ME", rmTokenValue);
            rmCookie.setHttpOnly(true);
            rmCookie.setPath("/");
            rmCookie.setMaxAge(7 * 24 * 60 * 60);
            response.addCookie(rmCookie);
        }

        String redirectUrl = "/reader/home";

        if (user.getRole() == User.Role.LIBRARIAN || user.getRole() == User.Role.ADMIN) {
            redirectUrl = "/librarian/dashboard";
        }

        return ResponseEntity.ok(Map.of("message", "Đăng nhập thành công",
                                        "redirectUrl", redirectUrl));
    }

    // Hiển thị trang đăng ký tài khoản.
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    // Xử lý đăng ký user mới và kiểm tra trùng username/email.
    @PostMapping("api/auth/register-process")
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

    // Hiển thị trang quên mật khẩu.
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgotPassword";
    }

    // Tạo OTP và gửi email xác minh để đặt lại mật khẩu.
    @PostMapping("api/auth/forgot-password-process")
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
    @PostMapping("/api/auth/verify-otp")
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
    @PostMapping({"/api/auth/reset-password"})
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
    @PostMapping("/api/auth/resend-otp")
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

    // Đăng xuất, xóa cookie JWT/remember-me và hủy session.
    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie jwt = new Cookie("JWT_TOKEN", null);
        jwt.setMaxAge(0);
        jwt.setPath("/");
        response.addCookie(jwt);

        Cookie rm = new Cookie("REMEMBER_ME", null);
        rm.setMaxAge(0);
        rm.setPath("/");
        response.addCookie(rm);

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("REMEMBER_ME".equals(c.getName())) {
                    rememberMeService.removeToken(c.getValue());
                }
            }
        }

        SecurityContextHolder.clearContext();
        request.getSession().invalidate();

        return "redirect:/login?logout=true";
    } 

    // Điều hướng route gốc theo trạng thái đăng nhập.
    @GetMapping("/")
    public String handleRootUrl(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        return "redirect:/reader/home";
    }
}
