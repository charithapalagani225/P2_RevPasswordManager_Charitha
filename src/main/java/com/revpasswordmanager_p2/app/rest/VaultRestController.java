package com.revpasswordmanager_p2.app.rest;

import com.passwordmanager.app.dto.VaultEntryDTO;
import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.service.UserService;
import com.passwordmanager.app.service.VaultService;
import com.passwordmanager.app.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vault")
public class VaultRestController {

    private final VaultService vaultService;
    private final UserService userService;
    private final AuthUtil authUtil;

    public VaultRestController(VaultService vaultService, UserService userService, AuthUtil authUtil) {
        this.vaultService = vaultService;
        this.userService = userService;
        this.authUtil = authUtil;
    }

    @GetMapping
    public ResponseEntity<List<VaultEntryDTO>> getAllEntries(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "name") String sort) {
        User user = authUtil.getCurrentUser();
        List<VaultEntryDTO> entries = vaultService.getAllEntries(user.getId(), search, category, sort);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VaultEntryDTO> getEntry(@PathVariable Long id) {
        User user = authUtil.getCurrentUser();
        VaultEntryDTO entry = vaultService.getEntryMasked(user.getId(), id);
        return ResponseEntity.ok(entry);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> addEntry(@Valid @RequestBody VaultEntryDTO dto) {
        User user = authUtil.getCurrentUser();
        Map<String, String> response = new HashMap<>();
        try {
            vaultService.addEntry(user, dto);
            response.put("message", "Entry added successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> updateEntry(@PathVariable Long id,
            @Valid @RequestBody VaultEntryDTO dto) {
        User user = authUtil.getCurrentUser();
        Map<String, String> response = new HashMap<>();
        try {
            vaultService.updateEntry(user.getId(), id, dto);
            response.put("message", "Entry updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long id) {
        User user = authUtil.getCurrentUser();
        vaultService.deleteEntry(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reveal")
    public ResponseEntity<?> revealPassword(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        User user = authUtil.getCurrentUser();
        String masterPassword = payload.get("masterPassword");

        if (masterPassword == null || !userService.verifyMasterPassword(user, masterPassword)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Incorrect master password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        VaultEntryDTO entry = vaultService.getEntryWithDecryptedPassword(user.getId(), id);
        return ResponseEntity.ok(entry);
    }

    @PostMapping("/{id}/favorite")
    public ResponseEntity<Void> toggleFavorite(@PathVariable Long id) {
        User user = authUtil.getCurrentUser();
        vaultService.toggleFavorite(user.getId(), id);
        return ResponseEntity.ok().build();
    }
}
