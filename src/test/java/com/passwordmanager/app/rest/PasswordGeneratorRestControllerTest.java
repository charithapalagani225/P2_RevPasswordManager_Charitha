package com.passwordmanager.app.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.passwordmanager.app.dto.PasswordGeneratorConfigDTO;
import com.passwordmanager.app.rest.PasswordGeneratorRestController;
import com.passwordmanager.app.service.PasswordGeneratorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class PasswordGeneratorRestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PasswordGeneratorService generatorService;

    @InjectMocks
    private PasswordGeneratorRestController controller;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void testGenerate() throws Exception {
        PasswordGeneratorConfigDTO config = new PasswordGeneratorConfigDTO();
        config.setLength(12);

        when(generatorService.generate(any(PasswordGeneratorConfigDTO.class)))
                .thenReturn(Collections.singletonList("GeneratedPassword123!"));
        when(generatorService.strengthScore(anyString())).thenReturn(4);
        when(generatorService.strengthLabel(4)).thenReturn("Strong");

        mockMvc.perform(post("/api/generator/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].password").value("GeneratedPassword123!"))
                .andExpect(jsonPath("$[0].score").value(4))
                .andExpect(jsonPath("$[0].label").value("Strong"));
    }
}
