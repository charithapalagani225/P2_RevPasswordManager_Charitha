package com.revpasswordmanager_p2.app.service;

import com.passwordmanager.app.dto.PasswordGeneratorConfigDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class PasswordGeneratorService {

    private static final Logger logger = LogManager.getLogger(PasswordGeneratorService.class);

    private static final String UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String UPPERCASE_SIMILAR = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghjkmnpqrstuvwxyz";
    private static final String LOWERCASE_SIMILAR = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMBERS = "23456789";
    private static final String NUMBERS_SIMILAR = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()-_=+[]{}|;:,.<>?";

    // Similar chars excluded: 0,O,l,1,I
    private final SecureRandom random = new SecureRandom();

    public List<String> generate(PasswordGeneratorConfigDTO config) {
        validateConfig(config);

        List<String> passwords = new ArrayList<>();
        for (int i = 0; i < config.getCount(); i++) {
            passwords.add(generateOne(config));
        }
        logger.debug("Generated {} password(s) with length {}", config.getCount(), config.getLength());
        return passwords;
    }

    private String generateOne(PasswordGeneratorConfigDTO config) {
        boolean excl = config.isExcludeSimilar();

        StringBuilder charset = new StringBuilder();
        List<Character> required = new ArrayList<>();

        if (config.isIncludeUppercase()) {
            String pool = excl ? UPPERCASE : UPPERCASE_SIMILAR;
            charset.append(pool);
            required.add(pool.charAt(random.nextInt(pool.length())));
        }
        if (config.isIncludeLowercase()) {
            String pool = excl ? LOWERCASE : LOWERCASE_SIMILAR;
            charset.append(pool);
            required.add(pool.charAt(random.nextInt(pool.length())));
        }
        if (config.isIncludeNumbers()) {
            String pool = excl ? NUMBERS : NUMBERS_SIMILAR;
            charset.append(pool);
            required.add(pool.charAt(random.nextInt(pool.length())));
        }
        if (config.isIncludeSymbols()) {
            charset.append(SPECIAL);
            required.add(SPECIAL.charAt(random.nextInt(SPECIAL.length())));
        }

        if (charset.isEmpty()) {
            charset.append(LOWERCASE_SIMILAR);
        }

        char[] pool = charset.toString().toCharArray();
        List<Character> chars = new ArrayList<>(required);

        for (int i = required.size(); i < config.getLength(); i++) {
            chars.add(pool[random.nextInt(pool.length)]);
        }

        Collections.shuffle(chars, random);

        StringBuilder sb = new StringBuilder();
        for (char c : chars)
            sb.append(c);
        return sb.toString();
    }

    public int strengthScore(String password) {
        int score = 0;
        if (password == null || password.isEmpty())
            return 0;
        if (password.length() >= 8)
            score++;
        if (password.length() >= 12)
            score++;
        if (password.matches(".*[A-Z].*"))
            score++;
        if (password.matches(".*[a-z].*"))
            score++;
        if (password.matches(".*[0-9].*"))
            score++;
        if (password.matches(".*[^A-Za-z0-9].*"))
            score++;
        return Math.min(score, 4);
    }

    public String strengthLabel(int score) {
        return switch (score) {
            case 0, 1 -> "Weak";
            case 2 -> "Medium";
            case 3 -> "Strong";
            default -> "Very Strong";
        };
    }

    private void validateConfig(PasswordGeneratorConfigDTO config) {
        if (config.getLength() < 8)
            config.setLength(8);
        if (config.getLength() > 64)
            config.setLength(64);
        if (config.getCount() < 1)
            config.setCount(1);
        if (config.getCount() > 10)
            config.setCount(10);
    }
}
