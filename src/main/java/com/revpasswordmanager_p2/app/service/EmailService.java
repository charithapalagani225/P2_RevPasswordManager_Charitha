package com.revpasswordmanager_p2.app.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LogManager.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendOtp(String toEmail, String otp, String purpose) {
        try {
            String subject = getSubject(purpose);
            String body = buildHtml(otp, purpose);
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(msg);
            logger.info("OTP email sent to {} for purpose={}", toEmail, purpose);
        } catch (MessagingException e) {
            logger.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String getSubject(String purpose) {
        return switch (purpose) {
            case "EMAIL_VERIFY" -> "RevPassManager – Verify Your Email";
            case "2FA" -> "RevPassManager – Your 2FA Code";
            case "EMAIL_CHANGE" -> "RevPassManager – Confirm New Email";
            default -> "RevPassManager – Verification Code";
        };
    }

    private String buildHtml(String otp, String purpose) {
        String action = switch (purpose) {
            case "EMAIL_VERIFY" -> "verify your email address";
            case "2FA" -> "complete your login";
            case "EMAIL_CHANGE" -> "confirm your new email address";
            default -> "verify your identity";
        };
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family:Inter,Arial,sans-serif;background:#0d0f14;margin:0;padding:32px;">
                  <div style="max-width:480px;margin:auto;background:#1a1d27;border-radius:16px;
                              border:1px solid rgba(255,255,255,.08);padding:40px;text-align:center;">
                    <h1 style="color:#a78bfa;font-size:24px;margin-bottom:8px;">🔐 RevPassManager</h1>
                    <p style="color:#ccc;margin-bottom:24px;">Use the code below to %s.</p>
                    <div style="background:#0d0f14;border-radius:12px;padding:24px;
                                border:1px solid rgba(167,139,250,.3);margin-bottom:24px;">
                      <div style="font-size:40px;font-weight:700;letter-spacing:10px;color:#a78bfa;">%s</div>
                    </div>
                    <p style="color:#888;font-size:13px;">This code expires in 10 minutes.<br>
                       If you did not request this, please ignore this email.</p>
                  </div>
                </body>
                </html>
                """.formatted(action, otp);
    }
}
