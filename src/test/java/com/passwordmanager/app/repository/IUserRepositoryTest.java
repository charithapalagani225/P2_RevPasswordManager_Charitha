package com.passwordmanager.app.repository;

import com.passwordmanager.app.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IUserRepositoryTest {

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setMasterPasswordHash("hashedpassword");
        testUser.setFullName("Test User");
        // Hibernate handles these, but let's see if setting them manually was the issue
        // testUser.setCreatedAt(LocalDateTime.now());
        // testUser.setUpdatedAt(LocalDateTime.now());

        entityManager.persist(testUser);
        entityManager.flush();
    }

    @Test
    void whenFindByUsername_thenReturnUser() {
        Optional<User> found = userRepository.findByUsername("testuser");
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    void whenFindByEmail_thenReturnUser() {
        Optional<User> found = userRepository.findByEmail("test@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void whenExistsByUsername_thenReturnTrue() {
        boolean exists = userRepository.existsByUsername("testuser");
        assertThat(exists).isTrue();
    }

    @Test
    void whenExistsByEmail_thenReturnTrue() {
        boolean exists = userRepository.existsByEmail("test@example.com");
        assertThat(exists).isTrue();
    }

    @Test
    void whenFindByUsernameOrEmail_thenReturnUser() {
        Optional<User> foundByUsername = userRepository.findByUsernameOrEmail("testuser", "wrong@example.com");
        Optional<User> foundByEmail = userRepository.findByUsernameOrEmail("wronguser", "test@example.com");

        assertThat(foundByUsername).isPresent();
        assertThat(foundByEmail).isPresent();
        assertThat(foundByUsername.get().getUsername()).isEqualTo("testuser");
        assertThat(foundByEmail.get().getEmail()).isEqualTo("test@example.com");
    }
}
