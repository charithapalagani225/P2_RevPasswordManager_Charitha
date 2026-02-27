package com.revpasswordmanager_p2.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResultDTO {
    private String password;
    private int score;
    private String label;
}
