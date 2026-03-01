package com.passwordmanager.app.service;

import com.passwordmanager.app.dto.VaultEntryDTO;
import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.entity.VaultEntry;
import com.passwordmanager.app.exception.ResourceNotFoundException;
import com.passwordmanager.app.repository.IVaultEntryRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VaultServiceTest {

    @Mock
    private IVaultEntryRepository vaultRepo;
    @Mock
    private EncryptionService encryptionService;
    private VaultService vaultService;
    private User testUser;

    @Before
    public void setUp() {
        vaultService = new VaultService(vaultRepo, encryptionService);
        testUser = User.builder().id(1L).username("alice").build();
    }

    // ===== addEntry() =====

    @Test
    public void addEntry_savesEncryptedEntry() {
        VaultEntryDTO dto = buildDto("Gmail", "test@gmail.com", "secret123");
        when(encryptionService.encrypt("secret123")).thenReturn("ENCRYPTED123");
        when(vaultRepo.save(any(VaultEntry.class))).thenAnswer(invocation -> {
            VaultEntry e = invocation.getArgument(0);
            e.setId(100L);
            return e;
        });

        VaultEntry saved = vaultService.addEntry(testUser, dto);
        assertEquals("ENCRYPTED123", saved.getEncryptedPassword());
        assertEquals("Gmail", saved.getAccountName());
        verify(vaultRepo).save(any(VaultEntry.class));
    }

    // ===== updateEntry() =====

    @Test
    public void updateEntry_existingEntry_updatesFields() {
        VaultEntry existing = buildVaultEntry(100L, "OldName", "OLDENC");
        when(vaultRepo.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(existing));
        when(encryptionService.encrypt("newpass")).thenReturn("NEWENC");
        when(vaultRepo.save(any(VaultEntry.class))).thenAnswer(i -> i.getArgument(0));

        VaultEntryDTO dto = buildDto("NewName", "user@test.com", "newpass");
        vaultService.updateEntry(1L, 100L, dto);

        assertEquals("NewName", existing.getAccountName());
        assertEquals("NEWENC", existing.getEncryptedPassword());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void updateEntry_notFound_throwsException() {
        when(vaultRepo.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());
        vaultService.updateEntry(1L, 999L, new VaultEntryDTO());
    }

    // ===== deleteEntry() =====

    @Test
    public void deleteEntry_existingEntry_deletesIt() {
        VaultEntry entry = buildVaultEntry(100L, "Test", "ENC");
        when(vaultRepo.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(entry));

        vaultService.deleteEntry(1L, 100L);
        verify(vaultRepo).delete(entry);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void deleteEntry_notFound_throwsException() {
        when(vaultRepo.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());
        vaultService.deleteEntry(1L, 999L);
    }

    // ===== toggleFavorite() =====

    @Test
    public void toggleFavorite_false_setsTrue() {
        VaultEntry entry = buildVaultEntry(1L, "Test", "ENC");
        entry.setFavorite(false);
        when(vaultRepo.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(entry));
        when(vaultRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        vaultService.toggleFavorite(1L, 1L);
        assertTrue(entry.isFavorite());
    }

    @Test
    public void toggleFavorite_true_setsFalse() {
        VaultEntry entry = buildVaultEntry(1L, "Test", "ENC");
        entry.setFavorite(true);
        when(vaultRepo.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(entry));
        when(vaultRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        vaultService.toggleFavorite(1L, 1L);
        assertFalse(entry.isFavorite());
    }

    // ===== getEntryMasked() =====

    @Test
    public void getEntryMasked_returnsEntryWithMaskedPassword() {
        VaultEntry entry = buildVaultEntry(1L, "Gmail", "ENCRYPTED");
        when(vaultRepo.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(entry));

        VaultEntryDTO dto = vaultService.getEntryMasked(1L, 1L);
        assertEquals("Gmail", dto.getAccountName());
        assertEquals("••••••••", dto.getPassword());
        verify(encryptionService, never()).decrypt(any()); // should NOT decrypt
    }

    // ===== getEntryWithDecryptedPassword() =====

    @Test
    public void getEntryWithDecryptedPassword_decryptsAndReturns() {
        VaultEntry entry = buildVaultEntry(1L, "Bank", "ENCRYPTED_BANKPW");
        when(vaultRepo.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(entry));
        when(encryptionService.decrypt("ENCRYPTED_BANKPW")).thenReturn("BankPass123!");

        VaultEntryDTO dto = vaultService.getEntryWithDecryptedPassword(1L, 1L);
        assertEquals("BankPass123!", dto.getPassword());
    }

    // ===== countByUser() =====

    @Test
    public void countByUser_returnsRepositoryCount() {
        when(vaultRepo.countByUserId(1L)).thenReturn(42L);
        assertEquals(42L, vaultService.countByUser(1L));
    }

    // ===== getFavorites() =====

    @Test
    public void getFavorites_returnsFavoriteEntries() {
        VaultEntry fav = buildVaultEntry(1L, "Favorite", "ENC");
        fav.setFavorite(true);
        when(vaultRepo.findByUserIdAndFavoriteTrueOrderByAccountNameAsc(1L))
                .thenReturn(Collections.singletonList(fav));

        List<VaultEntryDTO> result = vaultService.getFavorites(1L);
        assertEquals(1, result.size());
        assertEquals("Favorite", result.get(0).getAccountName());
    }

    // ===== Helpers =====

    private VaultEntryDTO buildDto(String name, String username, String password) {
        VaultEntryDTO dto = new VaultEntryDTO();
        dto.setAccountName(name);
        dto.setAccountUsername(username);
        dto.setPassword(password);
        dto.setCategory(VaultEntry.Category.OTHER);
        return dto;
    }

    private VaultEntry buildVaultEntry(Long id, String name, String encrypted) {
        return VaultEntry.builder()
                .id(id)
                .user(testUser)
                .accountName(name)
                .encryptedPassword(encrypted)
                .category(VaultEntry.Category.OTHER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
