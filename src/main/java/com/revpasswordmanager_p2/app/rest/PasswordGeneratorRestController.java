package com.revpasswordmanager_p2.app.rest;

import com.revpasswordmanager_p2.app.dto.PasswordGeneratorConfigDTO;
import com.revpasswordmanager_p2.app.dto.PasswordResultDTO;
import com.revpasswordmanager_p2.app.service.PasswordGeneratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/generator")
public class PasswordGeneratorRestController {

    private final PasswordGeneratorService generatorService;

    public PasswordGeneratorRestController(PasswordGeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    @PostMapping("/generate")
    public ResponseEntity<List<PasswordResultDTO>> generatePasswords(@RequestBody PasswordGeneratorConfigDTO config) {
        // Enforce limits to prevent abuse
        if (config.getLength() < 8)
            config.setLength(8);
        if (config.getLength() > 128)
            config.setLength(128);
        if (config.getCount() < 1)
            config.setCount(1);
        if (config.getCount() > 50)
            config.setCount(50);

        List<String> generated = generatorService.generate(config);
        List<PasswordResultDTO> results = generated.stream()
                .map(pwd -> new PasswordResultDTO(
                        pwd,
                        generatorService.strengthScore(pwd),
                        generatorService.strengthLabel(generatorService.strengthScore(pwd))))
                .collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }
}
