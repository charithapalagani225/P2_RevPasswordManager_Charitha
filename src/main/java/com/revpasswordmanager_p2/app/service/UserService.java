package com.revpasswordmanager_p2.app.service;

import com.revpasswordmanager_p2.app.dto.*;
import com.revpasswordmanager_p2.app.entity.SecurityQuestion;
import com.revpasswordmanager_p2.app.entity.User;
import com.revpasswordmanager_p2.app.mapper.SecurityQuestionMapper;
import com.revpasswordmanager_p2.app.mapper.UserMapper;
import com.revpasswordmanager_p2.app.exception.InvalidCredentialsException;
import com.revpasswordmanager_p2.app.exception.ValidationException;
import com.revpasswordmanager_p2.app.repository.SecurityQuestionRepository;
import com.revpasswordmanager_p2.app.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private static final Logger logger = LogManager.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final SecurityQuestionRepository sqRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationService verificationService;
    private final UserMapper userMapper;
    private final SecurityQuestionMapper securityQuestionMapper;

    public UserService(UserRepository userRepository,
            SecurityQuestionRepository sqRepository,
            PasswordEncoder passwordEncoder,
            VerificationService verificationService,
            UserMapper userMapper,
            SecurityQuestionMapper securityQuestionMapper) {
        this.userRepository = userRepository;
        this.sqRepository = sqRepository;
        this.passwordEncoder = passwordEncoder;
        this.verificationService = verificationService;
        this.userMapper = userMapper;
        this.securityQuestionMapper = securityQuestionMapper;
    }

    public void preValidateRegistration(RegisterDTO dto) {
        if (!dto.getMasterPassword().equals(dto.getConfirmPassword())) {
            throw new ValidationException("Passwords do not match");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new ValidationException("Username already taken");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ValidationException("Email already registered");
        }
        if (dto.getSecurityQuestions() == null || dto.getSecurityQuestions().size() < 3) {
            throw new ValidationException("Minimum 3 security questions required");
        }
    }

    public User register(RegisterDTO dto) {
        preValidateRegistration(dto);

        User user = userMapper.toEntity(dto);

        user = userRepository.save(user);

        final User savedUser = user;
        List<SecurityQuestion> questions = dto.getSecurityQuestions().stream()
                .map(sqDto -> securityQuestionMapper.toEntity(sqDto, savedUser))
                .collect(Collectors.toList());

        sqRepository.saveAll(questions);
        logger.info("User {} registered and security questions saved successfully.", user.getUsername());
        return user;
    }

    public User findByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username/email or password"));
    }

    public boolean verifyMasterPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getMasterPasswordHash());
    }

    public User updateProfile(Long userId, ProfileUpdateDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
        // Email changes go through verify-email flow, so we only update name/phone here
        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());

        // Handle profile photo upload
        if (dto.getProfilePhoto() != null && !dto.getProfilePhoto().isEmpty()) {
            try {
                String contentType = dto.getProfilePhoto().getContentType();
                String base64Image = Base64.getEncoder().encodeToString(dto.getProfilePhoto().getBytes());
                user.setProfilePhotoUrl("data:" + contentType + ";base64," + base64Image);
            } catch (IOException e) {
                logger.error("Failed to process profile photo for user: {}", user.getUsername(), e);
                // Continue saving other fields even if photo fails
            }
        }

        // Only apply email directly if it hasn't changed
        if (user.getEmail().equals(dto.getEmail())) {
            // no change — nothing to do
        } else {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new ValidationException("Email already in use");
            }
            // Store pending and trigger OTP
            user.setPendingEmail(dto.getEmail());
            user = userRepository.save(user);
            verificationService.generateAndSendOtp(user, "EMAIL_CHANGE");
            return user; // caller checks pendingEmail set to redirect
        }
        logger.info("Profile updated for user: {}", user.getUsername());
        return userRepository.save(user);
    }

    /** Called after OTP verified on /profile/verify-email */
    public void confirmEmailChange(Long userId, String otp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
        boolean ok = verificationService.validateCode(user, otp, "EMAIL_CHANGE");
        if (!ok)
            throw new ValidationException("Invalid or expired OTP");
        user.setEmail(user.getPendingEmail());
        user.setPendingEmail(null);
        user.setEmailVerified(true);
        userRepository.save(user);
        logger.info("Email changed and verified for user: {}", user.getUsername());
    }

    /** Mark user's email as verified after registration OTP */
    public void markEmailVerified(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    public void changeMasterPassword(Long userId, ChangePasswordDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
        if (!verifyMasterPassword(user, dto.getCurrentPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmNewPassword())) {
            throw new ValidationException("New passwords do not match");
        }
        user.setMasterPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        logger.info("Master password changed for user: {}", user.getUsername());
    }

    public void toggle2FA(Long userId, boolean enable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
        if (enable) {

            user.setTotpSecret(generateTotpSecret());
        } else {
            user.setTotpSecret(null);
        }
        user.setTotpEnabled(enable);
        userRepository.save(user);
        logger.info("2FA {} for user: {}", enable ? "enabled" : "disabled", user.getUsername());
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public void deleteAccount(Long userId, String masterPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        if (!verifyMasterPassword(user, masterPassword)) {
            throw new InvalidCredentialsException("Incorrect master password");
        }

        userRepository.delete(user);
        logger.info("User {} deleted their account.", user.getUsername());
    }

    private String generateTotpSecret() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        StringBuilder sb = new StringBuilder(16);
        java.util.Random rand = new java.security.SecureRandom();
        for (int i = 0; i < 16; i++)
            sb.append(chars.charAt(rand.nextInt(chars.length())));
        return sb.toString();
    }
}
