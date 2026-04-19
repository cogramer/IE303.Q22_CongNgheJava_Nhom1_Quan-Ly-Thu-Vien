package com.library.controller;

import com.library.dto.UserDTO;
import com.library.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.library.dto.AuthDTO.ChangePasswordRequest;
import com.library.dto.UserDTO;
import com.library.enums.Result;
import com.library.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    // Xem profile của chính mình
    @GetMapping("/profile")
    public String profile(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        UserDTO user = userService.getUserByUsername(userDetails.getUsername());
        model.addAttribute("user", user);
        return "user/profile";
    }

    // Cập nhật profile
    @PostMapping("/profile/update")
    public String updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @ModelAttribute UserDTO dto,
            Model model) {
        UserDTO current = userService.getUserByUsername(userDetails.getUsername());
        userService.updateUser(current.getId(), dto);
        return "redirect:/users/profile?success=true";
    }

    // Thủ thư xem danh sách tất cả user
    @GetMapping("/list")
    public String listUsers(
            @RequestParam(required = false) String keyword,
            Model model) {
        if (keyword != null && !keyword.isEmpty()) {
            model.addAttribute("users", userService.searchUsersByName(keyword));
            model.addAttribute("keyword", keyword);
        } else {
            model.addAttribute("users", userService.getAllUsers());
        }
        return "librarian/users";
    }

    // Thủ thư xóa user
    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/users/list";
    }

    @PostMapping("/changePassword")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ChangePasswordRequest changePasswordRequest) {
        Result changePasswordResult = userService.changePassword(userDetails.getUsername(), changePasswordRequest.getOldPassword(), changePasswordRequest.getNewPassword());
        if (changePasswordResult == Result.NEW_PASSWORD_SAME_AS_OLD_PASSWORD) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Mật khẩu mới trùng với mật khẩu cũ!"));
        } else if (changePasswordResult == Result.PASSWORD_INCORRECT) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Mật khẩu cũ không đúng!"));
        }
        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công!"));
    }
}