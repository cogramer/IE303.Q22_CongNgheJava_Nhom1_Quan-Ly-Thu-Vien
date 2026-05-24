package com.library.controller.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
}