package com.library.controller;

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

import com.library.dto.AuthDTO;
import com.library.dto.AuthDTO.*;
import com.library.service.AuthService;
import com.library.security.JwtUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class AuthController {
    @Autowired
    private AuthService authService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/login")
    public String loginPage(HttpServletRequest request) { 
        return "login";
    }

    @PostMapping("/loginProcess")
    public ResponseEntity<?> loginProcess(@RequestBody LoginRequest loginRequest, HttpServletResponse response ) {

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new LoginResponse("Sai tên đăng nhập hoặc mật khẩu", null));
        }

        String token = jwtUtil.generateToken(loginRequest.getUsername());

        Cookie jwtCookie = new Cookie("JWT_TOKEN", token);
        jwtCookie.setHttpOnly(true); // Quan trọng: Chống XSS (JS không đọc được)
        jwtCookie.setSecure(false);  // Đặt là true nếu chạy trên HTTPS (production), false nếu localhost
        jwtCookie.setPath("/");      // Áp dụng cookie này cho toàn bộ domain (bao gồm cả /home)
        jwtCookie.setMaxAge(24 * 60 * 60); // Thời gian sống của cookie (1 ngày tính bằng giây)

        response.addCookie(jwtCookie);

        return ResponseEntity.status(HttpStatus.OK).body(new LoginResponse("Đăng nhập thành công", token));
    }

    @GetMapping("/register") 
    public String registerPage() {
        return "register";
    }

    @PostMapping("/registerProcess")
    public ResponseEntity<?> registerProcess(@RequestBody RegisterRequest registerRequest, HttpServletResponse response) {
        if (authService.checkExistedUsername(registerRequest.getUsername())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new RegisterResponse("Tên người dùng đã tồn tại", null, null, null));
        }

        if (authService.checkExistedEmail(registerRequest.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new RegisterResponse("Email đã tồn tại", null, null, null));
        }
        
        authService.addNewUser(registerRequest.getUsername(), registerRequest.getPassword(), registerRequest.getEmail(), registerRequest.getFullName());
        return ResponseEntity.status(HttpStatus.OK).body(new RegisterResponse("Đăng ký thành công", registerRequest.getUsername(), registerRequest.getEmail(), registerRequest.getFullName()));
    }

    @GetMapping("/home")
    public String homePage() {
        return "home";
    }
}