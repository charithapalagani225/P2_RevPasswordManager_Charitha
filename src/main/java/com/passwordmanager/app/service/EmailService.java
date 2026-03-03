package com.passwordmanager.app.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService implements IEmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@example.com}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendOtp(String toEmail, String otp, String purpose) {
        logger.info("Attempting to send {} OTP to address: {}", purpose, toEmail);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);

            String subject = "Your Verification Code";
            if ("2FA".equalsIgnoreCase(purpose) || "LOGIN_2FA".equalsIgnoreCase(purpose)) {
                subject = "Two-Factor Authentication Code";
            } else if ("PASSWORD_RESET".equalsIgnoreCase(purpose) || "RECOVERY_2FA".equalsIgnoreCase(purpose)) {
                subject = "Password Recovery Verification Code";
            } else if ("REGISTRATION".equalsIgnoreCase(purpose)) {
                subject = "Email Verification Code";
            }

            helper.setSubject(subject);

            String htmlContent = "<html><body><h2>" + subject + "</h2>" +
                    "<p>Your code is: <strong>" + otp + "</strong></p>" +
                    "<p>This code will expire shortly.</p></body></html>";
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Successfully sent {} OTP to {}", purpose, toEmail);
        } catch (Exception e) {
            logger.error("CRITICAL: Failed to send {} email to {}. Root cause: {}", purpose, toEmail, e.getMessage());
            throw new RuntimeException("Failed to send email. Please check your connectivity or SMTP settings.", e);
        }
    }
}
