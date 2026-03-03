package com.passwordmanager.app.service;

import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.entity.VerificationCode;
import com.passwordmanager.app.repository.IVerificationCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VerificationServiceTest {

    @Mock
    private IVerificationCodeRepository codeRepo;

    @Mock
    private IEmailService emailService;

    @InjectMocks
    private VerificationService verificationService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(verificationService, "expiryMinutes", 10);

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
    }

    @Test
    void testGenerateAndSendOtp() {
        String code = verificationService.generateAndSendOtp(user, "2FA");

        assertNotNull(code);
        assertEquals(6, code.length());
        verify(codeRepo).save(any(VerificationCode.class));
        verify(emailService).sendOtp(eq("test@example.com"), eq(code), eq("2FA"));
    }

    @Test
    void testSendRegistrationOtp() {
        String code = verificationService.sendRegistrationOtp("test@example.com");

        assertNotNull(code);
        assertEquals(6, code.length());
        verify(emailService).sendOtp(eq("test@example.com"), eq(code), eq("REGISTRATION"));
    }

    @Test
    void testValidateCode_Success() {
        VerificationCode vc = new VerificationCode();
        vc.setId(10L);
        vc.setCode("123456");
        vc.setPurpose("2FA");
        vc.setUsed(false);
        vc.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(codeRepo.findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(1L, "2FA"))
                .thenReturn(Optional.of(vc));

        boolean isValid = verificationService.validateCode(user, "123456", "2FA");

        assertTrue(isValid);
        assertTrue(vc.isUsed());
        verify(codeRepo).save(vc);
    }

    @Test
    void testValidateCode_WrongCode() {
        VerificationCode vc = new VerificationCode();
        vc.setCode("123456");
        vc.setUsed(false);
        vc.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(codeRepo.findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(1L, "2FA"))
                .thenReturn(Optional.of(vc));

        boolean isValid = verificationService.validateCode(user, "654321", "2FA");

        assertFalse(isValid);
        assertFalse(vc.isUsed());
    }

    @Test
    void testValidateCode_Expired() {
        VerificationCode vc = new VerificationCode();
        vc.setCode("123456");
        vc.setUsed(false);
        vc.setExpiresAt(LocalDateTime.now().minusMinutes(5));

        when(codeRepo.findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(1L, "2FA"))
                .thenReturn(Optional.of(vc));

        boolean isValid = verificationService.validateCode(user, "123456", "2FA");

        assertFalse(isValid);
        assertFalse(vc.isUsed());
    }

    @Test
    void testValidateCode_NotFound() {
        when(codeRepo.findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(1L, "2FA"))
                .thenReturn(Optional.empty());

        boolean isValid = verificationService.validateCode(user, "123456", "2FA");

        assertFalse(isValid);
    }
}
