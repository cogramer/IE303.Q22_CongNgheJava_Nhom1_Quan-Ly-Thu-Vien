package com.library.security;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.library.model.RememberMeToken;
import com.library.service.RememberMeService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RememberMeService rememberMeService;
    @Autowired
    private UserDetailsService userDetailsService;

    //Danh sách API public
    private static final List<String> PUBLIC_URLS = List.of(
            "/login", "/loginProcess", "/register", "/registerProcess", "/forgotPassword", "/forgotPasswordProcess", "/logoutProcess", "/verifyOtpAndResetPassword"
    );

    //Bỏ qua filter cho API public
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return PUBLIC_URLS.contains(path)
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/");
    }

   @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String jwtToken = null;
        String rmToken = null;

        // Lấy các Cookie từ trình duyệt gửi lên
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("JWT_TOKEN".equals(c.getName())) jwtToken = c.getValue();
                if ("REMEMBER_ME".equals(c.getName())) rmToken = c.getValue();
            }
        }

        String username = null;

        // BƯỚC 1: Thử dùng JWT (Chìa khóa chính)
        if (jwtToken != null) {
            try {
                username = jwtUtil.extractUsername(jwtToken);
            } catch (Exception e) {
                // JWT lỗi hoặc hết hạn -> username vẫn null
            }
        }

        // BƯỚC 2: Nếu JWT mất (do tắt tab) mà có Remember Me -> Khôi phục lại
        if (username == null && rmToken != null) {
            RememberMeToken rmt = rememberMeService.findByToken(rmToken);
            
            if (rmt != null) {
                // 🔥 Nếu hết hạn → xóa DB + xóa cookie
                if (rmt.getExpiryDate().isBefore(LocalDateTime.now())) {
                    rememberMeService.removeToken(rmToken);

                    Cookie rm = new Cookie("REMEMBER_ME", null);
                    rm.setHttpOnly(true);
                    rm.setPath("/");
                    rm.setMaxAge(0);
                    response.addCookie(rm);

                } else {
                    // ✔️ Còn hạn → cấp lại JWT
                    username = rmt.getUser().getUsername();

                    String newJwt = jwtUtil.generateToken(username);
                    Cookie newJwtCookie = new Cookie("JWT_TOKEN", newJwt);
                    newJwtCookie.setHttpOnly(true);
                    newJwtCookie.setPath("/");
                    newJwtCookie.setMaxAge(-1);
                    response.addCookie(newJwtCookie);
                }
            } else {
                // 🔥 Token không tồn tại trong DB → xóa cookie luôn
                Cookie rm = new Cookie("REMEMBER_ME", null);
                rm.setHttpOnly(true);
                rm.setPath("/");
                rm.setMaxAge(0);
                response.addCookie(rm);
            }
        }

        // BƯỚC 3: Quyết định cho vào hay đuổi ra
        if (username != null) {
            // Xác thực thành công
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            filterChain.doFilter(request, response);
        } else {
            // Không có JWT, không có Remember Me -> Mời đăng nhập lại
            response.sendRedirect(request.getContextPath() + "/login");
        }
    }
}