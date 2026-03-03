package com.passwordmanager.app.service;

import com.passwordmanager.app.dto.AuditReport;
import com.passwordmanager.app.entity.VaultEntry;
import com.passwordmanager.app.repository.IVaultEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SecurityAuditServiceTest {

    @Mock
    private IVaultEntryRepository vaultRepo;

    @Mock
    private IEncryptionService encryptionService;

    @Mock
    private IPasswordGeneratorService generatorService;

    @InjectMocks
    private SecurityAuditService securityAuditService;

    @Test
    void testGenerateReport() {
        VaultEntry e1 = new VaultEntry();
        e1.setId(1L);
        e1.setAccountName("Facebook");
        e1.setEncryptedPassword("enc1");
        e1.setCreatedAt(LocalDateTime.now().minusDays(100)); // Old password
        e1.setUpdatedAt(LocalDateTime.now().minusDays(100));

        VaultEntry e2 = new VaultEntry();
        e2.setId(2L);
        e2.setAccountName("Twitter");
        e2.setEncryptedPassword("enc2");
        e2.setCreatedAt(LocalDateTime.now());
        e2.setUpdatedAt(LocalDateTime.now());

        VaultEntry e3 = new VaultEntry();
        e3.setId(3L);
        e3.setAccountName("Google");
        e3.setEncryptedPassword("enc3");
        e3.setCreatedAt(LocalDateTime.now());
        e3.setUpdatedAt(LocalDateTime.now());

        when(vaultRepo.findByUserIdOrderByAccountNameAsc(1L)).thenReturn(List.of(e1, e2, e3));

        when(encryptionService.decrypt("enc1")).thenReturn("weak");
        when(encryptionService.decrypt("enc2")).thenReturn("weak"); // Reused as well
        when(encryptionService.decrypt("enc3")).thenReturn("strong");

        when(generatorService.strengthScore("weak")).thenReturn(1);
        when(generatorService.strengthScore("strong")).thenReturn(4);
        // Removed when(generatorService.strengthLabel(1)).thenReturn("WEAK");
        AuditReport report = securityAuditService.generateReport(1L, 90);

        assertEquals(3, report.getTotalEntries());
        assertEquals(2, report.getWeakPasswords().size()); // e1, e2
        assertEquals(1, report.getOldPasswords().size()); // e1
        // Since "weak" is used twice, they are both considered reused
        assertEquals(2, report.getReusedPasswords().size()); // e1, e2
    }
}
