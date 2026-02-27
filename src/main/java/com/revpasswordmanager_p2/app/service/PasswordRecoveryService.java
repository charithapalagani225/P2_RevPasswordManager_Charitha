package com.revpasswordmanager_p2.app.service;

import com.revpasswordmanager_p2.app.entity.SecurityQuestion;
import com.revpasswordmanager_p2.app.entity.User;
import com.revpasswordmanager_p2.app.exception.InvalidCredentialsException;
import com.revpasswordmanager_p2.app.exception.ValidationException;
import com.revpasswordmanager_p2.app.repository.SecurityQuestionRepository;
import com.revpasswordmanager_p2.app.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class PasswordRecoveryService {

    private static final Logger logger = LogManager.getLogger(PasswordRecoveryService.class);

    private final UserRepository userRepository;
    private final SecurityQuestionRepository sqRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordRecoveryService(UserRepository userRepository,
            SecurityQuestionRepository sqRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.sqRepository = sqRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<SecurityQuestion> getQuestions(String usernameOrEmail) {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Account not found"));
        List<SecurityQuestion> questions = sqRepository.findByUserId(user.getId());
        if (questions.isEmpty()) {
            throw new ValidationException("No security questions configured for this account");
        }
        return questions;
    }

    /**
     * Validates answers for all questions. Returns true only if ALL answers match.
     */
    public boolean validateAnswers(Long userId, List<String> answers) {
        List<SecurityQuestion> questions = sqRepository.findByUserId(userId);
        if (answers.size() != questions.size())
            return false;
        for (int i = 0; i < questions.size(); i++) {
            String answer = answers.get(i) == null ? "" : answers.get(i).toLowerCase().trim();
            if (!passwordEncoder.matches(answer, questions.get(i).getAnswerHash())) {
                logger.warn("Security answer mismatch for user {} question idx={}", userId, i);
                return false;
            }
        }
        return true;
    }

    public void resetPassword(String usernameOrEmail, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new ValidationException("New password must be at least 8 characters");
        }
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Account not found"));
        user.setMasterPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        logger.info("Password reset via security questions for user: {}", user.getUsername());
    }
}
