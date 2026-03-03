package com.passwordmanager.app.service;

import com.passwordmanager.app.entity.SecurityQuestion;
import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.repository.ISecurityQuestionRepository;
import com.passwordmanager.app.repository.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PasswordRecoveryServiceTest {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private ISecurityQuestionRepository questionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordRecoveryService passwordRecoveryService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
    }

    @Test
    void testGetQuestions_Success() {
        when(userRepository.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.of(user));

        SecurityQuestion q1 = new SecurityQuestion();
        q1.setId(10L);
        SecurityQuestion q2 = new SecurityQuestion();
        q2.setId(20L);

        when(questionRepository.findByUserId(1L)).thenReturn(List.of(q1, q2));

        List<SecurityQuestion> questions = passwordRecoveryService.getQuestions("testuser");

        assertEquals(2, questions.size());
        verify(questionRepository).findByUserId(1L);
    }

    @Test
    void testGetQuestions_UserNotFound() {
        when(userRepository.findByUsernameOrEmail("nonexistent", "nonexistent")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> passwordRecoveryService.getQuestions("nonexistent"));
    }

    @Test
    void testValidateAnswers_Success() {
        SecurityQuestion q1 = new SecurityQuestion();
        q1.setAnswerHash("hash1");
        SecurityQuestion q2 = new SecurityQuestion();
        q2.setAnswerHash("hash2");

        when(questionRepository.findByUserId(1L)).thenReturn(List.of(q1, q2));
        when(passwordEncoder.matches("answer1", "hash1")).thenReturn(true);
        when(passwordEncoder.matches("answer2", "hash2")).thenReturn(true);

        boolean isValid = passwordRecoveryService.validateAnswers(1L, List.of("answer1", "answer2"));

        assertTrue(isValid);
    }

    @Test
    void testValidateAnswers_IncorrectAnswer() {
        SecurityQuestion q1 = new SecurityQuestion();
        q1.setAnswerHash("hash1");

        when(questionRepository.findByUserId(1L)).thenReturn(List.of(q1));
        when(passwordEncoder.matches("wronganswer", "hash1")).thenReturn(false);

        boolean isValid = passwordRecoveryService.validateAnswers(1L, List.of("wronganswer"));

        assertFalse(isValid);
    }

    @Test
    void testValidateAnswers_MismatchedCount() {
        SecurityQuestion q1 = new SecurityQuestion();
        when(questionRepository.findByUserId(1L)).thenReturn(List.of(q1));

        boolean isValid = passwordRecoveryService.validateAnswers(1L, List.of("answer1", "answer2"));

        assertFalse(isValid);
    }

    @Test
    void testResetPassword_Success() {
        when(userRepository.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpassword")).thenReturn("newhash");

        passwordRecoveryService.resetPassword("testuser", "newpassword");

        assertEquals("newhash", user.getMasterPasswordHash());
        verify(userRepository).save(user);
    }
}
