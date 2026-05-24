package com.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Bật CSRF (Tắt tạm cho API để test qua Postman/Fetch)
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))

                // 2. Phân quyền URL
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/forgot-password",
                                "/api/auth/**", "/css/**", "/js/**", "/images/**", "/error", "/img/**")
                        .permitAll()
                        .requestMatchers("/api/books/**", "/api/categories/**").permitAll()
                        .anyRequest().authenticated())

                // 3. CẤU HÌNH ĐĂNG NHẬP (Session)
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true) // Đá về GET / để WebAuthController điều phối tiếp
                        .failureUrl("/login?error=true")
                        .permitAll())

                // 🌟 BỔ SUNG: Bật tính năng Nhớ mật khẩu
                .rememberMe(remember -> remember
                        .key("uniqueAndSecretLibraryKey2026") // Key bảo mật để mã hóa token
                        .rememberMeParameter("remember-me") // Khớp với name="remember-me" trong HTML
                        .tokenValiditySeconds(7 * 24 * 60 * 60) // Thời gian nhớ: 7 ngày (tính bằng giây)
                )

                // 4. CẤU HÌNH ĐĂNG XUẤT
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        // Sửa lại tên cookie mặc định của remember-me là "remember-me"
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}