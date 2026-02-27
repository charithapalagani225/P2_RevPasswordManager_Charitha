package com.revpasswordmanager_p2.app.service;

import com.passwordmanager.app.entity.VaultEntry;
import com.passwordmanager.app.repository.VaultEntryRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SecurityAuditService {

    private static final Logger logger = LogManager.getLogger(SecurityAuditService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final VaultEntryRepository vaultRepo;
    private final EncryptionService encryptionService;
    private final PasswordGeneratorService generatorService;

    public SecurityAuditService(VaultEntryRepository vaultRepo,
            EncryptionService encryptionService,
            PasswordGeneratorService generatorService) {
        this.vaultRepo = vaultRepo;
        this.encryptionService = encryptionService;
        this.generatorService = generatorService;
    }

    public AuditReport generateReport(Long userId, int oldPasswordDays) {
        List<VaultEntry> entries = vaultRepo.findByUserIdOrderByAccountNameAsc(userId);
        AuditReport report = new AuditReport();

        Map<String, List<Long>> passwordToIds = new HashMap<>();

        for (VaultEntry entry : entries) {
            String plain;
            try {
                plain = encryptionService.decrypt(entry.getEncryptedPassword());
            } catch (Exception e) {
                logger.warn("Could not decrypt entry id={}", entry.getId());
                continue;
            }
            int score = generatorService.strengthScore(plain);

            if (score <= 1) {
                report.getWeakPasswords().add(toItem(entry, score));
            }

            // Old password check
            LocalDateTime lastChanged = entry.getUpdatedAt() != null ? entry.getUpdatedAt() : entry.getCreatedAt();
            if (lastChanged != null && lastChanged.isBefore(LocalDateTime.now().minusDays(oldPasswordDays))) {
                report.getOldPasswords().add(toItem(entry, score));
            }

            passwordToIds.computeIfAbsent(plain, k -> new ArrayList<>()).add(entry.getId());
        }

        // Reused passwords
        for (Map.Entry<String, List<Long>> me : passwordToIds.entrySet()) {
            if (me.getValue().size() > 1) {
                // Find names for each id
                List<String> names = new ArrayList<>();
                for (Long eid : me.getValue()) {
                    entries.stream().filter(e -> e.getId().equals(eid))
                            .findFirst().ifPresent(e -> {
                                names.add(e.getAccountName());
                                report.getReusedPasswords().add(toItem(e, generatorService.strengthScore(me.getKey())));
                            });
                }
            }
        }

        // Compute score: start 100, deduct for issues
        int score = 100;
        if (!entries.isEmpty()) {
            int weak = report.getWeakPasswords().size();
            int reused = report.getReusedPasswords().size();
            int old = report.getOldPasswords().size();
            score -= (weak * 10 + reused * 5 + old * 3);
        }
        report.setSecurityScore(score);
        report.setTotalEntries(entries.size());

        logger.info("Security audit for user {}: weak={}, reused={}, old={}, score={}",
                userId, report.getWeakPasswords().size(),
                report.getReusedPasswords().size(), report.getOldPasswords().size(),
                report.getSecurityScore());
        return report;
    }

    private AuditItem toItem(VaultEntry entry, int score) {
        AuditItem item = new AuditItem();
        item.setId(entry.getId());
        item.setAccountName(entry.getAccountName());
        item.setCategory(entry.getCategory() != null ? entry.getCategory().name() : "OTHER");
        item.setStrengthScore(score);
        item.setStrengthLabel(generatorService.strengthLabel(score));
        LocalDateTime ldt = entry.getUpdatedAt() != null ? entry.getUpdatedAt() : entry.getCreatedAt();
        item.setUpdatedAt(ldt != null ? ldt.format(FMT) : "");
        return item;
    }

    // ====== Inner POJOs ======
    public static class AuditReport {
        private int totalEntries;
        private int securityScore = 100;
        private List<AuditItem> weakPasswords = new ArrayList<>();
        private List<AuditItem> reusedPasswords = new ArrayList<>();
        private List<AuditItem> oldPasswords = new ArrayList<>();

        public int getTotalEntries() {
            return totalEntries;
        }

        public void setTotalEntries(int t) {
            this.totalEntries = t;
        }

        public int getSecurityScore() {
            return securityScore;
        }

        public void setSecurityScore(int s) {
            this.securityScore = Math.max(0, Math.min(100, s));
        }

        public List<AuditItem> getWeakPasswords() {
            return weakPasswords;
        }

        public List<AuditItem> getReusedPasswords() {
            return reusedPasswords;
        }

        public List<AuditItem> getOldPasswords() {
            return oldPasswords;
        }
    }

    public static class AuditItem {
        private Long id;
        private String accountName;
        private String category;
        private int strengthScore;
        private String strengthLabel;
        private String updatedAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getAccountName() {
            return accountName;
        }

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public int getStrengthScore() {
            return strengthScore;
        }

        public void setStrengthScore(int strengthScore) {
            this.strengthScore = strengthScore;
        }

        public String getStrengthLabel() {
            return strengthLabel;
        }

        public void setStrengthLabel(String strengthLabel) {
            this.strengthLabel = strengthLabel;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}
