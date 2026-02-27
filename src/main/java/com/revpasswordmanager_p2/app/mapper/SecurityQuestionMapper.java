package com.revpasswordmanager_p2.app.mapper;

import com.revpasswordmanager_p2.app.dto.SecurityQuestionDTO;
import com.revpasswordmanager_p2.app.entity.SecurityQuestion;
import com.revpasswordmanager_p2.app.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SecurityQuestionMapper {

    private final PasswordEncoder passwordEncoder;

    public SecurityQuestionMapper(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public SecurityQuestion toEntity(SecurityQuestionDTO dto, User user) {
        if (dto == null) {
            return null;
        }
        return SecurityQuestion.builder()
                .user(user)
                .questionText(dto.getQuestionText())
                .answerHash(passwordEncoder.encode(dto.getAnswer().toLowerCase().trim()))
                .build();
    }
}
