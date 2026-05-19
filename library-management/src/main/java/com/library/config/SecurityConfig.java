package com.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.library.repository.UserRepository;
import com.library.security.JwtFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {  
    private final JwtFilter jwtFilter;

    private final UserRepository userRepository;

    public SecurityConfig(JwtFilter jwtFilter, UserRepository userRepository) {
        this.jwtFilter = jwtFilter;
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/forgot-password",
                    "/api/auth/**", "/css/**", "/js/**", "/images/**", "/error", "/img/**").permitAll()
                .requestMatchers("/librarian/**",
                    "/users/{userId}/feedback", "/feedback/{id}", "/feedback/{id}"
                ).hasAnyRole("ADMIN", "LIBRARIAN")
                .requestMatchers("/reader/**").hasAnyRole("READER", "ADMIN", "LIBRARIAN")
                .anyRequest().authenticated()
            )
            
            .formLogin(form -> form.disable())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            
            .logout(logout -> logout.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
