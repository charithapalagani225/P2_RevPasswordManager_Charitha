package com.passwordmanager.app.repository;

import com.passwordmanager.app.entity.SecurityQuestion;
import com.passwordmanager.app.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ISecurityQuestionRepositoryTest {

    @Autowired
    private ISecurityQuestionRepository securityQuestionRepository;

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

        SecurityQuestion q1 = new SecurityQuestion();
        q1.setUser(testUser);
        q1.setQuestionText("What is your pet's name?");
        q1.setAnswerHash("hashedanswer1");
        entityManager.persist(q1);

        SecurityQuestion q2 = new SecurityQuestion();
        q2.setUser(testUser);
        q2.setQuestionText("What city were you born in?");
        q2.setAnswerHash("hashedanswer2");
        entityManager.persist(q2);

        entityManager.flush();
    }

    @Test
    void whenFindByUserId_thenReturnQuestions() {
        List<SecurityQuestion> found = securityQuestionRepository.findByUserId(testUser.getId());
        assertThat(found).hasSize(2);
    }

    @Test
    void whenCountByUserId_thenReturnCount() {
        long count = securityQuestionRepository.countByUserId(testUser.getId());
        assertThat(count).isEqualTo(2);
    }

    @Test
    void whenDeleteByUserId_thenQuestionsDeleted() {
        securityQuestionRepository.deleteByUserId(testUser.getId());
        entityManager.flush();
        entityManager.clear();

        List<SecurityQuestion> found = securityQuestionRepository.findByUserId(testUser.getId());
        assertThat(found).isEmpty();
    }
}
