package com.revpay.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void sendWelcomeEmail(String toEmail, String username) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Welcome to Our Platform!");
            helper.setText(
                    "<h2>Hi " + username + "!</h2>" +
                            "<p styple>Welcome! Your registration was <strong>successful</strong>.</p>" +
                            "<p>Thank you for joining us!</p>" +
                            "<br><p>Best regards,<br><b>The Revpay Team</b></p>",
                    true // enables HTML
            );

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Async
    public void sendOtpEmail(String toEmail, String username, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Your RevPay Verification Code");
            helper.setText(
                    "<h2>Hello " + username + "!</h2>" +
                            "<p>Your verification code for RevPay login is:</p>" +
                            "<h1 style='color: #2563eb; letter-spacing: 5px;'>" + otp + "</h1>" +
                            "<p>This code will expire in 10 minutes.</p>" +
                            "<p>If you didn't request this code, please secure your account.</p>" +
                            "<br><p>Best regards,<br><b>The RevPay Team</b></p>",
                    true
            );

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
}