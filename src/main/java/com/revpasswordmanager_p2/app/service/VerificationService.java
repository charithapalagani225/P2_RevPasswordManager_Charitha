package com.revpasswordmanager_p2.app.service;

import com.revpasswordmanager_p2.app.entity.User;
import com.revpasswordmanager_p2.app.entity.VerificationCode;
import com.revpasswordmanager_p2.app.repository.VerificationCodeRepository;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@Transactional
public class VerificationService {

    private static final Logger logger = LogManager.getLogger(VerificationService.class);
    private final SecureRandom random = new SecureRandom();

    @Value("${app.verification.expiry-minutes:10}")
    private int expiryMinutes;

    private final VerificationCodeRepository vcRepo;
    private final EmailService emailService;

    public VerificationService(VerificationCodeRepository vcRepo, EmailService emailService) {
        this.vcRepo = vcRepo;
        this.emailService = emailService;
    }

    /**
     * Generate a 6-digit OTP, persist it, and send it by email.
     * Returns the code (for logging only — never expose to UI).
     */
    public String generateAndSendOtp(User user, String purpose) {
        vcRepo.deleteExpiredAndUsed(LocalDateTime.now());

        String code = String.format("%06d", random.nextInt(1_000_000));
        VerificationCode vc = VerificationCode.builder()
                .user(user)
                .code(code)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .build();
        vcRepo.save(vc);

        String targetEmail = ("EMAIL_CHANGE".equals(purpose) && user.getPendingEmail() != null)
                ? user.getPendingEmail()
                : user.getEmail();

        emailService.sendOtp(targetEmail, code, purpose);
        logger.info("OTP generated and emailed to {} for purpose={}", targetEmail, purpose);
        return code;
    }

    /**
     * Generate a 6-digit OTP and send it by email without persisting it to the
     * database.
     * This is used for new registrations where the User entity does not exist yet.
     */
    public String sendRegistrationOtp(String email) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        emailService.sendOtp(email, code, "EMAIL_VERIFY");
        logger.info("Registration OTP generated and emailed to {}", email);
        return code;
    }

    /** Kept for legacy / internal use (no email). */
    public String generateCode(User user, String purpose) {
        vcRepo.deleteExpiredAndUsed(LocalDateTime.now());

        String code = String.format("%06d", random.nextInt(1_000_000));
        VerificationCode vc = VerificationCode.builder()
                .user(user)
                .code(code)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .build();
        vcRepo.save(vc);
        logger.info("Verification code generated for user {} purpose={}", user.getUsername(), purpose);
        return code;
    }

    public boolean validateCode(User user, String code, String purpose) {
        return vcRepo.findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(user.getId(), purpose)
                .map(vc -> {
                    if (vc.getCode().equals(code) && vc.isValid()) {
                        vc.setUsed(true);
                        vcRepo.save(vc);
                        logger.info("Code validated for user {} purpose={}", user.getUsername(), purpose);
                        return true;
                    }
                    logger.warn("Invalid or expired code for user {} purpose={}", user.getUsername(), purpose);
                    return false;
                })
                .orElse(false);
    }
}
