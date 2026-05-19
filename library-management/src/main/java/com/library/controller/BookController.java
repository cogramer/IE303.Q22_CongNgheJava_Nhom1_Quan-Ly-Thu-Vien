package com.library.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/book")
public class BookController {
    // Điều hướng route /books sang trang danh sách sách của reader.
    @GetMapping("/")
    public String booksPage() {
        return "redirect:/reader/books";
    }
}