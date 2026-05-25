package com.library.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

  private final JavaMailSender mailSender;

  public void sendEmail(String to, String subject, String text) {
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom("librarymngmnt.demo.user@gmail.com"); // Thay bằng email của bạn hoặc lấy từ properties
      message.setTo(to);
      message.setSubject(subject);
      message.setText(text);

      mailSender.send(message);
      log.info("Đã gửi email thành công đến: {}", to);
    } catch (Exception e) {
      log.error("Lỗi khi gửi email đến {}: {}", to, e.getMessage());
    }
  }
}