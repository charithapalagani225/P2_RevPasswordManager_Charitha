package com.passwordmanager.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
        ReflectionTestUtils.setField(encryptionService, "secret", "12345678901234567890123456789012");
    }

    @Test
    void testEncryptDecrypt() {
        String plainText = "mySuperSecretPassword123!";

        String cipherText = encryptionService.encrypt(plainText);
        assertNotEquals(plainText, cipherText);

        String decryptedText = encryptionService.decrypt(cipherText);
        assertEquals(plainText, decryptedText);
    }

    @Test
    void testDecrypt_InvalidCipherText() {
        assertThrows(RuntimeException.class, () -> encryptionService.decrypt("invalid-base64-content"));
    }
}
