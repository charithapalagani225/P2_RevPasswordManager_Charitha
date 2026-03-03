package com.passwordmanager.app.service;

import com.passwordmanager.app.dto.PasswordGeneratorConfigDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordGeneratorServiceTest {

    private PasswordGeneratorService service;

    @BeforeEach
    public void setUp() {
        service = new PasswordGeneratorService();
    }

    // ===== generate() tests =====

    @Test
    public void generate_defaultConfig_returnsCorrectCount() {
        PasswordGeneratorConfigDTO config = defaultConfig(16, 3);
        List<String> passwords = service.generate(config);
        assertEquals(3, passwords.size());
    }

    @Test
    public void generate_lengthRespected() {
        PasswordGeneratorConfigDTO config = defaultConfig(24, 1);
        String pw = service.generate(config).get(0);
        assertEquals(24, pw.length());
    }

    @Test
    public void generate_includesUppercase_whenEnabled() {
        PasswordGeneratorConfigDTO config = defaultConfig(20, 5);
        config.setIncludeUppercase(true);
        config.setIncludeLowercase(false);
        config.setIncludeNumbers(false);
        config.setIncludeSymbols(false);
        boolean hasUpper = service.generate(config).stream()
                .anyMatch(pw -> pw.chars().anyMatch(Character::isUpperCase));
        assertTrue(hasUpper, "At least one generated password should have uppercase");
    }

    @Test
    public void generate_includesNumbers_whenEnabled() {
        PasswordGeneratorConfigDTO config = defaultConfig(20, 5);
        config.setIncludeNumbers(true);
        boolean hasDigit = service.generate(config).stream()
                .anyMatch(pw -> pw.chars().anyMatch(Character::isDigit));
        assertTrue(hasDigit, "At least one generated password should contain a digit");
    }

    @Test
    public void generate_includesSpecialChars_whenEnabled() {
        PasswordGeneratorConfigDTO config = defaultConfig(20, 5);
        config.setIncludeSymbols(true);
        boolean hasSymbol = service.generate(config).stream()
                .anyMatch(pw -> pw.chars().anyMatch(c -> !Character.isLetterOrDigit(c)));
        assertTrue(hasSymbol, "At least one generated password should contain a special character");
    }

    @Test
    public void generate_excludeSimilar_noSimilarChars() {
        PasswordGeneratorConfigDTO config = defaultConfig(30, 20);
        config.setExcludeSimilar(true);
        for (String pw : service.generate(config)) {
            assertFalse(pw.contains("O"), "Password should not contain 'O'");
            assertFalse(pw.contains("I"), "Password should not contain 'I'");
            assertFalse(pw.contains("l"), "Password should not contain 'l'");
        }
    }

    @Test
    public void generate_passwordsAreUnique() {
        PasswordGeneratorConfigDTO config = defaultConfig(16, 5);
        List<String> passwords = service.generate(config);
        long distinct = passwords.stream().distinct().count();
        assertTrue(distinct >= 4, "Generated passwords should be mostly unique");
    }

    @Test
    public void generate_minLengthEnforced_whenTooShort() {
        PasswordGeneratorConfigDTO config = defaultConfig(4, 1); // below minimum of 8
        List<String> passwords = service.generate(config);
        assertEquals(1, passwords.size());
        assertTrue(passwords.get(0).length() >= 8, "Length should be at least 8 when 4 is requested");
    }

    // ===== strengthScore() tests =====

    @Test
    public void strengthScore_emptyPassword_returnsZero() {
        assertEquals(0, service.strengthScore(""));
    }

    @Test
    public void strengthScore_nullPassword_returnsZero() {
        assertEquals(0, service.strengthScore(null));
    }

    @Test
    public void strengthScore_veryShortSimple_returnsLowScore() {
        int score = service.strengthScore("abc");
        assertTrue(score <= 1, "Very short simple password should score <= 1");
    }

    @Test
    public void strengthScore_strongPassword_returnsHighScore() {
        // Has uppercase, lowercase, digit, special, length >= 12
        int score = service.strengthScore("Tr0ub4dor&3!Extra");
        assertTrue(score >= 3, "Strong password should score >= 3");
    }

    @Test
    public void strengthScore_veryStrongPassword_returnsFour() {
        int score = service.strengthScore("I8$qX!@mN3pLk#Wr");
        assertEquals(4, score);
    }

    // ===== strengthLabel() tests =====

    @Test
    public void strengthLabel_score0_returnsWeak() {
        assertEquals("Weak", service.strengthLabel(0));
    }

    @Test
    public void strengthLabel_score1_returnsWeak() {
        assertEquals("Weak", service.strengthLabel(1));
    }

    @Test
    public void strengthLabel_score2_returnsMedium() {
        assertEquals("Medium", service.strengthLabel(2));
    }

    @Test
    public void strengthLabel_score3_returnsStrong() {
        assertEquals("Strong", service.strengthLabel(3));
    }

    @Test
    public void strengthLabel_score4_returnsVeryStrong() {
        assertEquals("Very Strong", service.strengthLabel(4));
    }

    // ===== Helper =====
    private PasswordGeneratorConfigDTO defaultConfig(int length, int count) {
        PasswordGeneratorConfigDTO config = new PasswordGeneratorConfigDTO();
        config.setLength(length);
        config.setCount(count);
        config.setIncludeUppercase(true);
        config.setIncludeLowercase(true);
        config.setIncludeNumbers(true);
        config.setIncludeSymbols(false);
        config.setExcludeSimilar(false);
        return config;
    }
}
