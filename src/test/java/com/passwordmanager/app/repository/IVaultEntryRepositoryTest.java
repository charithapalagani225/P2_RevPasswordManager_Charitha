package com.passwordmanager.app.repository;

import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.entity.VaultEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IVaultEntryRepositoryTest {

    @Autowired
    private IVaultEntryRepository vaultEntryRepository;

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

        VaultEntry entry1 = new VaultEntry();
        entry1.setUser(testUser);
        entry1.setAccountName("Alpha Account");
        entry1.setWebsiteUrl("https://alpha.com");
        entry1.setAccountUsername("alphauser");
        entry1.setCategory(VaultEntry.Category.EMAIL);
        entry1.setFavorite(true);
        entry1.setEncryptedPassword("encrypted_password_alpha");
        entry1.setCreatedAt(LocalDateTime.now().minusDays(1));
        entityManager.persist(entry1);

        VaultEntry entry2 = new VaultEntry();
        entry2.setUser(testUser);
        entry2.setAccountName("Beta Account");
        entry2.setWebsiteUrl("https://beta.com");
        entry2.setAccountUsername("betauser");
        entry2.setCategory(VaultEntry.Category.LOGIN);
        entry2.setFavorite(false);
        entry2.setEncryptedPassword("encrypted_password_beta");
        entry2.setCreatedAt(LocalDateTime.now());
        entityManager.persist(entry2);

        entityManager.flush();
    }

    @Test
    void whenFindByUserIdOrderByAccountNameAsc_thenReturnOrderedEntries() {
        List<VaultEntry> found = vaultEntryRepository.findByUserIdOrderByAccountNameAsc(testUser.getId());
        assertThat(found).hasSize(2);
        assertThat(found.get(0).getAccountName()).isEqualTo("Alpha Account");
        assertThat(found.get(1).getAccountName()).isEqualTo("Beta Account");
    }

    @Test
    void whenFindByUserIdAndFavoriteTrue_thenReturnFavorites() {
        List<VaultEntry> found = vaultEntryRepository
                .findByUserIdAndFavoriteTrueOrderByAccountNameAsc(testUser.getId());
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getAccountName()).isEqualTo("Alpha Account");
    }

    @Test
    void whenSearchByAccountName_thenReturnMatches() {
        List<VaultEntry> found = vaultEntryRepository.search(testUser.getId(), "Alpha");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getAccountName()).isEqualTo("Alpha Account");
    }

    @Test
    void whenFindRecentByUserId_thenReturnLatestFirst() {
        List<VaultEntry> found = vaultEntryRepository.findRecentByUserId(testUser.getId(), PageRequest.of(0, 10));
        assertThat(found).hasSize(2);
        assertThat(found.get(0).getAccountName()).isEqualTo("Beta Account"); // Created later
    }

    @Test
    void whenFindByIdAndUserId_thenReturnMatchingEntry() {
        List<VaultEntry> all = vaultEntryRepository.findAll();
        Long entryId = all.get(0).getId();

        Optional<VaultEntry> found = vaultEntryRepository.findByIdAndUserId(entryId, testUser.getId());
        assertThat(found).isPresent();
    }
}
