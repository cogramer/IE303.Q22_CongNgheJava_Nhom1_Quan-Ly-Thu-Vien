package com.library.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.context.SecurityContextHolder;

import com.library.dto.AuthDTO.ForgotPasswordRequest;
import com.library.dto.AuthDTO.LoginRequest;
import com.library.dto.AuthDTO.RegisterRequest;
import com.library.dto.AuthDTO.ResetPasswordRequest;
import com.library.dto.UserDTO;
import com.library.enums.LoginResult;
import com.library.enums.RegisterResult;
import com.library.enums.Result;
import com.library.model.RememberMeToken;
import com.library.security.JwtUtil;
import com.library.service.AuthService;
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
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserService userService;
    @Autowired
    private RememberMeService rememberMeService;

    private static final long OTP_RESEND_COOLDOWN = 60 * 1000; // 60 giây chờ gửi lại
    private static final long OTP_EXPIRATION_TIME = 60 * 1000; // 1 phút hết hạn mã

    @GetMapping("/")
    public String handleRootUrl() {
        // Cứ có người lọt được vào đây thì tự động đá sang /home
        return "redirect:/home";
    }

    @GetMapping("/login")
    public String loginPage(HttpServletRequest request) {
        return "login";
    }

    @PostMapping("/loginProcess")
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
            
            // Lưu vào Database thông qua Service của bạn
            RememberMeToken rmt = new RememberMeToken();
            rmt.setToken(rmTokenValue);
            rmt.setExpiryDate(LocalDateTime.now().plusDays(7));
            rememberMeService.save(rmt, user.getId());

            // Gửi Cookie REMEMBER_ME về trình duyệt (Sống 7 ngày)
            Cookie rmCookie = new Cookie("REMEMBER_ME", rmTokenValue);
            rmCookie.setHttpOnly(true);
            rmCookie.setPath("/");
            rmCookie.setMaxAge(7 * 24 * 60 * 60);
            response.addCookie(rmCookie);
        }

        return ResponseEntity.ok(Map.of("message", "Đăng nhập thành công"));
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/registerProcess")
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

    @GetMapping("/forgotPassword")
    public String forgotPasswordPage() {
        return "forgotPassword";
    }

    @PostMapping("/forgotPasswordProcess")
    public ResponseEntity<?> forgetPasswordProcess(@RequestBody ForgotPasswordRequest forgotPasswordRequest, HttpServletRequest request) throws MessagingException {
        HttpSession session = request.getSession();

        Long lastSentTime = (Long) session.getAttribute("otpLastSentTime");
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

        // ĐỒNG NHẤT TÊN LƯU SESSION LÀ "otp_server"
        session.setAttribute("otp_server", otp);
        session.setAttribute("emailChange", forgotPasswordRequest.getEmail());
        session.setAttribute("otpCreationTime", System.currentTimeMillis());
        session.setAttribute("verificationType", "FORGOT_PASS");

        authService.sendChangePasswordOtp(forgotPasswordRequest.getEmail(), otp);
        session.setAttribute("otpLastSentTime", currentTime); // Cập nhật lại thời gian vừa gửi

        return ResponseEntity.ok(Map.of("message", "Mã xác minh đang được gửi vào email!"));
    }

    @PostMapping("/verifyOtpAndResetPassword")
    public ResponseEntity<?> verifyOtpAndResetPassword(@RequestBody ResetPasswordRequest resetPasswordRequest, HttpServletRequest request) {
        HttpSession session = request.getSession();
        String userOtp = resetPasswordRequest.getOtp();
        String serverOtp = (String) session.getAttribute("otp_server");
        Long otpCreationTime = (Long) session.getAttribute("otpCreationTime");

        // 1. Kiểm tra xem server có đang lưu OTP nào không (Tránh lỗi NullPointerException)
        if (serverOtp == null || otpCreationTime == null) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Mã OTP không tồn tại hoặc đã bị hủy. Vui lòng gửi lại mã mới!"));
        }

        // 2. KIỂM TRA HẾT HẠN (Quan trọng nhất)
        long currentTime = System.currentTimeMillis();
        if (currentTime - otpCreationTime > OTP_EXPIRATION_TIME) {
            // Nếu quá 1 phút -> Xóa OTP cũ để ép người dùng phải gửi lại
            session.removeAttribute("otp_server");
            session.removeAttribute("otpCreationTime");

            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Mã OTP đã hết hạn (quá 1 phút). Vui lòng nhận mã mới!"));
        }

        // 3. Kiểm tra xem người dùng nhập đúng không
        if (serverOtp.equals(userOtp)) {
            // Đúng thì dọn dẹp Session cho sạch sẽ
            session.removeAttribute("otp_server");
            session.removeAttribute("otpCreationTime");

            Result resetPassResult = userService.resetPassword(resetPasswordRequest.getEmail(), resetPasswordRequest.getNewPassword());

            if (resetPassResult == Result.SUCCESS) {
                return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công!"));
            } else {
                return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Đổi mật khẩu thất bại do lỗi hệ thống!"));
            }
        }

        // Nếu chạy đến đây tức là mã không hết hạn nhưng NHẬP SAI
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of("message", "Mã OTP không đúng!"));
    }

    @PostMapping("/logoutProcess")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // Xóa JWT cookie
        Cookie jwt = new Cookie("JWT_TOKEN", null);
        jwt.setMaxAge(0);
        jwt.setPath("/");
        response.addCookie(jwt);

        // Xóa REMEMBER_ME cookie
        Cookie rm = new Cookie("REMEMBER_ME", null);
        rm.setMaxAge(0);
        rm.setPath("/");
        response.addCookie(rm);

        // Xóa token trong DB
        // lấy rmToken từ request để xóa
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("REMEMBER_ME".equals(c.getName())) {
                    rememberMeService.removeToken(c.getValue());
                }
            }
        }
        //Xóa SecurityContext
        SecurityContextHolder.clearContext();

        //Invalidate session
        request.getSession().invalidate();

        return ResponseEntity.ok().body(Map.of("message", "Logout thành công!"));
    } 

    @GetMapping("/home")
    public String homePage() {
        return "home";
    }
}