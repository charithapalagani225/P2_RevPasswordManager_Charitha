package com.passwordmanager.app.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@example.com");
    }

    @Test
    void testSendOtp_Registration() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendOtp("test@example.com", "123456", "REGISTRATION");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testSendOtp_2FA() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendOtp("test@example.com", "123456", "2FA");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testSendOtp_PasswordReset() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendOtp("test@example.com", "123456", "PASSWORD_RESET");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testSendOtp_Exception() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Mail server down"));

        assertThrows(RuntimeException.class, () -> emailService.sendOtp("test@example.com", "123456", "REGISTRATION"));
    }
}
