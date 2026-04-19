package com.library.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import com.library.model.User;
import com.library.repository.UserRepository;
import com.library.enums.LoginResult;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.MessagingException;


@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository; 
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JavaMailSender mailSender;

    public LoginResult checkAccount(String username, String password) {

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return LoginResult.USERNAME_NOT_FOUND;
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return LoginResult.PASSWORD_NOT_MATCH;
        }

        return LoginResult.SUCCESS;
    }

    public String generateOTP() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            otp.append("0123456789".charAt(random.nextInt(10)));
        }
        return otp.toString();
    }

    private String createEmailTemplate(String title, String bodyContent) {
        return "<!DOCTYPE html>"
                + "<html><head>"
                + "<meta charset='UTF-8'>"
                + "<style>"
                + "body { font-family: Helvetica, Arial, sans-serif; background-color: #fafafa; margin: 0; padding: 0; }"
                + ".btn { display: inline-block; background-color: #ff6600; color: #ffffff !important; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold; margin-top: 20px; }"
                + "</style></head>"

                + "<body style='margin: 0; padding: 0; background-color: #fafafa; font-family: Helvetica, Arial, sans-serif;'>"

                + "<div style='width: 100%; background-color: #fafafa; padding: 30px 0;'>"

                + "<div class='container' style='max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; overflow: hidden; border: 1px solid #eee; box-shadow: 0 2px 8px rgba(0,0,0,0.05);'>"

                + "  <div class='content' style='padding: 40px 30px; color: #000000; line-height: 1.7; font-size: 16px;'>"
                + "    <h2 style='color: #000000; margin-top: 0; text-align: center; font-weight: bold;'>" + title + "</h2>"

                + "    <div style='color: #000000 !important;'>"
                +          bodyContent
                + "    </div>"

                + "  </div>"
                + "</div>"
                + "</div>"
                + "</body></html>";
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("pdcb29@gmail.com", "Library Management");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Async("mailExecutor")
    public void sendChangePasswordOtp(String email, String otpCode) {
        String body = "<p style='text-align: center; color: #000000;'>Xin chào,</p>"
                + "<p style='color: #000000;'>Hệ thống nhận được yêu cầu <b>đổi mật khẩu</b> cho tài khoản liên kết với email này.</p>"
                + "<p style='color: #000000;'>Đây là mã OTP xác thực của bạn:</p>"

                + "<div style='background-color: #e3f2fd; color: #1565c0; font-size: 36px; font-weight: bold; text-align: center; padding: 20px; margin: 30px 0; letter-spacing: 8px; border-radius: 8px; border: 1px dashed #2196f3;'>"
                + otpCode
                + "</div>"

                + "<p style='text-align: center; color: #000000;'>Mã này có hiệu lực trong vòng <b>5 phút</b>. Vui lòng không chia sẻ cho bất kỳ ai.</p>"
                + "<p style='text-align: center; color: #000000;'>Nếu bạn không thực hiện yêu cầu này, vui lòng đổi mật khẩu ngay lập tức.</p>";

        String htmlContent = createEmailTemplate("Yêu cầu đổi mật khẩu", body);
        sendHtmlEmail(email, "Mã OTP đổi mật khẩu", htmlContent);
    }
}
