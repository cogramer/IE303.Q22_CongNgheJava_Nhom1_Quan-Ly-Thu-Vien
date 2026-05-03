package com.library.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute("username")
    public String addUsername(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails != null) {
            return userDetails.getUsername();
        }
        return null;
    }
}