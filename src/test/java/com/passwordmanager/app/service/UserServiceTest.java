package com.passwordmanager.app.service;

import com.passwordmanager.app.dto.ChangePasswordDTO;
import com.passwordmanager.app.dto.ProfileUpdateDTO;
import com.passwordmanager.app.dto.RegisterDTO;
import com.passwordmanager.app.dto.SecurityQuestionDTO;
import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.exception.InvalidCredentialsException;
import com.passwordmanager.app.exception.ValidationException;
import com.passwordmanager.app.repository.ISecurityQuestionRepository;
import com.passwordmanager.app.repository.IUserRepository;
import com.passwordmanager.app.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {

    @Mock
    private IUserRepository IUserRepository;

    @Mock
    private ISecurityQuestionRepository sqRepository;

    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @Before
    public void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(IUserRepository, sqRepository, passwordEncoder);
    }

    // ===== register() tests =====

    @Test
    public void register_validInput_savesUser() {
        RegisterDTO dto = buildRegisterDTO("alice", "alice@example.com", "SecurePass1!", "SecurePass1!");
        when(IUserRepository.existsByUsername("alice")).thenReturn(false);
        when(IUserRepository.existsByEmail("alice@example.com")).thenReturn(false);
        User saved = User.builder().id(1L).username("alice").email("alice@example.com")
                .masterPasswordHash(passwordEncoder.encode("SecurePass1!")).build();
        when(IUserRepository.save(any(User.class))).thenReturn(saved);

        User result = userService.register(dto);
        assertNotNull(result);
        assertEquals("alice", result.getUsername());
        verify(sqRepository).saveAll(argThat(list -> list.iterator().hasNext()));
    }

    @Test(expected = ValidationException.class)
    public void register_passwordMismatch_throwsValidationException() {
        RegisterDTO dto = buildRegisterDTO("bob", "bob@example.com", "Password1!", "Different!");
        userService.register(dto);
    }

    @Test(expected = ValidationException.class)
    public void register_duplicateUsername_throwsValidationException() {
        RegisterDTO dto = buildRegisterDTO("alice", "new@example.com", "Pass1234!", "Pass1234!");
        when(IUserRepository.existsByUsername("alice")).thenReturn(true);
        userService.register(dto);
    }

    @Test(expected = ValidationException.class)
    public void register_duplicateEmail_throwsValidationException() {
        RegisterDTO dto = buildRegisterDTO("newuser", "alice@example.com", "Pass1234!", "Pass1234!");
        when(IUserRepository.existsByUsername("newuser")).thenReturn(false);
        when(IUserRepository.existsByEmail("alice@example.com")).thenReturn(true);
        userService.register(dto);
    }

    @Test(expected = ValidationException.class)
    public void register_insufficientSecurityQuestions_throwsValidationException() {
        RegisterDTO dto = buildRegisterDTO("carol", "carol@test.com", "Pass1234!", "Pass1234!");
        dto.getSecurityQuestions().remove(2); // Only 2 questions
        when(IUserRepository.existsByUsername(any())).thenReturn(false);
        when(IUserRepository.existsByEmail(any())).thenReturn(false);
        userService.register(dto);
    }

    // ===== verifyMasterPassword() tests =====

    @Test
    public void verifyMasterPassword_correctPassword_returnsTrue() {
        User user = User.builder().masterPasswordHash(passwordEncoder.encode("MyPass123!")).build();
        assertTrue(userService.verifyMasterPassword(user, "MyPass123!"));
    }

    @Test
    public void verifyMasterPassword_wrongPassword_returnsFalse() {
        User user = User.builder().masterPasswordHash(passwordEncoder.encode("MyPass123!")).build();
        assertFalse(userService.verifyMasterPassword(user, "WrongPassword"));
    }

    // ===== updateProfile() tests =====

    @Test
    public void updateProfile_validData_updatesUser() {
        User user = User.builder().id(1L).username("alice").email("same@example.com").build();
        when(IUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(IUserRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        ProfileUpdateDTO dto = new ProfileUpdateDTO();
        dto.setFullName("Alice Smith");
        dto.setEmail("same@example.com"); // same email -> no OTP triggered
        dto.setPhone("+91 99999 99999");

        User updated = userService.updateProfile(1L, dto);
        assertEquals("Alice Smith", updated.getFullName());
        assertEquals("same@example.com", updated.getEmail());
    }

    @Test(expected = ValidationException.class)
    public void updateProfile_emailTaken_throwsValidationException() {
        User user = User.builder().id(1L).email("alice@example.com").build();
        when(IUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(IUserRepository.existsByEmail("taken@example.com")).thenReturn(true);

        ProfileUpdateDTO dto = new ProfileUpdateDTO();
        dto.setEmail("taken@example.com");
        userService.updateProfile(1L, dto);
    }

    // ===== changeMasterPassword() tests =====

    @Test
    public void changeMasterPassword_validInput_updatesHash() {
        User user = User.builder().id(1L).username("alice")
                .masterPasswordHash(passwordEncoder.encode("OldPass1!")).build();
        when(IUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(IUserRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setCurrentPassword("OldPass1!");
        dto.setNewPassword("NewPass2@");
        dto.setConfirmNewPassword("NewPass2@");

        userService.changeMasterPassword(1L, dto);
        assertTrue(passwordEncoder.matches("NewPass2@", user.getMasterPasswordHash()));
    }

    @Test(expected = InvalidCredentialsException.class)
    public void changeMasterPassword_wrongCurrent_throwsException() {
        User user = User.builder().id(1L)
                .masterPasswordHash(passwordEncoder.encode("OldPass1!")).build();
        when(IUserRepository.findById(1L)).thenReturn(Optional.of(user));

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setCurrentPassword("WrongPassword");
        dto.setNewPassword("NewPass2@");
        dto.setConfirmNewPassword("NewPass2@");

        userService.changeMasterPassword(1L, dto);
    }

    @Test(expected = ValidationException.class)
    public void changeMasterPassword_mismatchNew_throwsValidationException() {
        User user = User.builder().id(1L)
                .masterPasswordHash(passwordEncoder.encode("OldPass1!")).build();
        when(IUserRepository.findById(1L)).thenReturn(Optional.of(user));

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setCurrentPassword("OldPass1!");
        dto.setNewPassword("NewPass2@");
        dto.setConfirmNewPassword("Different!!");

        userService.changeMasterPassword(1L, dto);
    }

    // ===== toggle2FA() tests =====

    @Test
    public void toggle2FA_enable_setsTotpFields() {
        User user = User.builder().id(1L).username("alice").totpEnabled(false).build();
        when(IUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(IUserRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.toggle2FA(1L, true);
        assertTrue(user.isTotpEnabled());
        assertNotNull(user.getTotpSecret());
    }

    @Test
    public void toggle2FA_disable_clearsTotpSecret() {
        User user = User.builder().id(1L).totpEnabled(true).totpSecret("ABCDESECRET").build();
        when(IUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(IUserRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.toggle2FA(1L, false);
        assertFalse(user.isTotpEnabled());
        assertNull(user.getTotpSecret());
    }

    // ===== Helper =====
    private RegisterDTO buildRegisterDTO(String username, String email, String pw, String confirm) {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername(username);
        dto.setEmail(email);
        dto.setFullName("Test User");
        dto.setMasterPassword(pw);
        dto.setConfirmPassword(confirm);
        SecurityQuestionDTO sq1 = new SecurityQuestionDTO();
        sq1.setQuestionText("What is pet name?");
        sq1.setAnswer("fluffy");
        SecurityQuestionDTO sq2 = new SecurityQuestionDTO();
        sq2.setQuestionText("Mother's maiden name?");
        sq2.setAnswer("smith");
        SecurityQuestionDTO sq3 = new SecurityQuestionDTO();
        sq3.setQuestionText("First school?");
        sq3.setAnswer("lincoln");
        dto.setSecurityQuestions(new java.util.ArrayList<>(Arrays.asList(sq1, sq2, sq3)));
        return dto;
    }
}
