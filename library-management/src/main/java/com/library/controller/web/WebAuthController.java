package com.library.controller.web;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class WebAuthController {

  @GetMapping("/login")
  public String loginPage() {
    return "login";
  }

  @GetMapping("/register")
  public String registerPage() {
    return "register";
  }

  @GetMapping("/forgot-password")
  public String forgotPasswordPage() {
    return "forgotPassword";
  }

  @GetMapping("/")
  public String handleRootUrl(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return "redirect:/login";
    }
    return "redirect:/reader/home";
  }

  // se cấu hình sau khi cần cho thủ thư và admin
  // @GetMapping("/")
  // public String handleRootUrl(Authentication authentication) {
  // if (authentication == null || !authentication.isAuthenticated()
  // || authentication instanceof AnonymousAuthenticationToken) {
  // return "redirect:/login";
  // }

  // // Nếu là ADMIN hoặc THỦ THƯ -> Đẩy về Dashboard
  // if (authentication.getAuthorities().stream()
  // .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
  // a.getAuthority().equals("ROLE_LIBRARIAN"))) {
  // return "redirect:/librarian/dashboard";
  // }

  // // Nếu là ĐỘC GIẢ -> Đẩy về Home
  // return "redirect:/reader/home";
  // }
}