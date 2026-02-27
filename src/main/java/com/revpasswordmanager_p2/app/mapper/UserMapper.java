package com.revpasswordmanager_p2.app.mapper;

import com.passwordmanager.app.dto.RegisterDTO;
import com.passwordmanager.app.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    private final PasswordEncoder passwordEncoder;

    public UserMapper(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Maps a RegisterDTO to a User entity.
     * Note: emailVerified is set to true by default, matching UserService behavior.
     */
    public User toEntity(RegisterDTO dto) {
        if (dto == null) {
            return null;
        }
        return User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .fullName(dto.getFullName())
                .phone(dto.getPhone())
                .masterPasswordHash(passwordEncoder.encode(dto.getMasterPassword()))
                .emailVerified(true)
                .build();
    }
}
