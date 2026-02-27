package com.revpasswordmanager_p2.app.mapper;

import com.revpasswordmanager_p2.app.dto.VaultEntryDTO;
import com.revpasswordmanager_p2.app.entity.User;
import com.revpasswordmanager_p2.app.entity.VaultEntry;
import com.revpasswordmanager_p2.app.service.EncryptionService;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class VaultEntryMapper {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final EncryptionService encryptionService;

    public VaultEntryMapper(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public VaultEntry toEntity(VaultEntryDTO dto, User user) {
        if (dto == null) {
            return null;
        }
        return VaultEntry.builder()
                .user(user)
                .accountName(dto.getAccountName())
                .websiteUrl(dto.getWebsiteUrl())
                .accountUsername(dto.getAccountUsername())
                .encryptedPassword(encryptionService.encrypt(dto.getPassword()))
                .category(dto.getCategory() != null ? dto.getCategory() : VaultEntry.Category.OTHER)
                .notes(dto.getNotes())
                .favorite(dto.isFavorite())
                .build();
    }

    public void updateEntityFromDto(VaultEntry entry, VaultEntryDTO dto) {
        if (dto == null || entry == null) {
            return;
        }
        entry.setAccountName(dto.getAccountName());
        entry.setWebsiteUrl(dto.getWebsiteUrl());
        entry.setAccountUsername(dto.getAccountUsername());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            entry.setEncryptedPassword(encryptionService.encrypt(dto.getPassword()));
        }
        entry.setCategory(dto.getCategory() != null ? dto.getCategory() : VaultEntry.Category.OTHER);
        entry.setNotes(dto.getNotes());
        entry.setFavorite(dto.isFavorite());
    }

    public VaultEntryDTO toDto(VaultEntry e) {
        if (e == null) {
            return null;
        }
        VaultEntryDTO dto = new VaultEntryDTO();
        dto.setId(e.getId());
        dto.setAccountName(e.getAccountName());
        dto.setWebsiteUrl(e.getWebsiteUrl());
        dto.setAccountUsername(e.getAccountUsername());
        dto.setCategory(e.getCategory());
        dto.setNotes(e.getNotes());
        dto.setFavorite(e.isFavorite());
        dto.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().format(FMT) : "");
        dto.setUpdatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().format(FMT) : "");
        dto.setPassword("••••••••"); // Masked by default
        return dto;
    }

    public VaultEntryDTO toDecryptedDto(VaultEntry e) {
        if (e == null) {
            return null;
        }
        VaultEntryDTO dto = toDto(e);
        dto.setPassword(encryptionService.decrypt(e.getEncryptedPassword()));
        return dto;
    }
}
