package com.library.controller.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class GlobalWebAdvice {

  @ModelAttribute("username")
  public String addUsername(@AuthenticationPrincipal UserDetails userDetails) {
    if (userDetails != null) {
      return userDetails.getUsername();
    }
    return null;
  }
}