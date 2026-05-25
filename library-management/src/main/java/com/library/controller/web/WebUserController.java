package com.library.controller.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.core.Authentication;

import com.library.dto.UserDTO;
import com.library.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
public class WebUserController {

  private final UserService userService;

  @GetMapping("/profile")
  public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
    UserDTO user = userService.getUserByUsername(userDetails.getUsername());
    model.addAttribute("user", user);
    return "user/profile";
  }

  @PostMapping("/profile/update")
  public String updateProfile(@ModelAttribute("user") UserDTO userDTO, Authentication authentication) {
    try {
      // 1. Lấy ID của user đang đăng nhập
      Long currentUserId = userService.getUserByUsername(authentication.getName()).getId();

      // 2. Gọi service để cập nhật thông tin
      userService.updateUser(currentUserId, userDTO);

      // 3. Cập nhật thành công -> Redirect về trang profile kèm tham số success
      return "redirect:/users/profile?success=true";

    } catch (Exception e) {
      // Nếu có lỗi (ví dụ trùng email), trả về trang profile kèm báo lỗi
      return "redirect:/users/profile?error=true";
    }
  }
}