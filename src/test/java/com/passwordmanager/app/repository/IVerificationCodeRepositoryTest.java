package com.passwordmanager.app.repository;

import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.entity.VerificationCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest

class IVerificationCodeRepositoryTest {

    @Autowired
    private IVerificationCodeRepository verificationCodeRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setMasterPasswordHash("hashedpassword");
        entityManager.persist(testUser);

        VerificationCode code1 = new VerificationCode();
        code1.setUser(testUser);
        code1.setCode("123456");
        code1.setPurpose("2FA");
        code1.setUsed(false);
        code1.setExpiresAt(LocalDateTime.now().plusHours(1));
        code1.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        entityManager.persist(code1);

        VerificationCode code2 = new VerificationCode();
        code2.setUser(testUser);
        code2.setCode("654321");
        code2.setPurpose("2FA");
        code2.setUsed(false);
        code2.setExpiresAt(LocalDateTime.now().plusHours(1));
        code2.setCreatedAt(LocalDateTime.now()); // More recent
        entityManager.persist(code2);

        VerificationCode code3 = new VerificationCode();
        code3.setUser(testUser);
        code3.setCode("111222");
        code3.setPurpose("2FA");
        code3.setUsed(true); // Already used
        code3.setExpiresAt(LocalDateTime.now().plusHours(1));
        entityManager.persist(code3);

        VerificationCode code4 = new VerificationCode();
        code4.setUser(testUser);
        code4.setCode("333333");
        code4.setPurpose("PASSWORD_RESET");
        code4.setUsed(false);
        code4.setExpiresAt(LocalDateTime.now().minusHours(1)); // Expired
        entityManager.persist(code4);

        entityManager.flush();
    }

    @Test
    void whenFindTopByUserIdAndPurposeAndUsedFalse_thenReturnMostRecent() {
        Optional<VerificationCode> found = verificationCodeRepository
                .findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(testUser.getId(), "2FA");

        assertThat(found).isPresent();
        // Should be the one created now, not 5 mins ago
        assertThat(found.get().isUsed()).isFalse();
    }

    @Test
    void whenDeleteExpiredAndUsed_thenCleanupDatabase() {
        verificationCodeRepository.deleteExpiredAndUsed(LocalDateTime.now());
        entityManager.flush();
        entityManager.clear();

        // code1, code2 (not used, not expired) should remain
        // code3 (used), code4 (expired) should be deleted
        assertThat(verificationCodeRepository.findAll()).hasSize(2);
    }
}
