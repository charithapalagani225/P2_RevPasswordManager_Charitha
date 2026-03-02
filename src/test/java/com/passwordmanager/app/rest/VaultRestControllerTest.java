package com.passwordmanager.app.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.passwordmanager.app.dto.VaultEntryDTO;
import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.rest.VaultRestController;
import com.passwordmanager.app.service.UserService;
import com.passwordmanager.app.service.VaultService;
import com.passwordmanager.app.util.AuthUtil;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class VaultRestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VaultService vaultService;

    @Mock
    private UserService userService;

    @Mock
    private AuthUtil authUtil;

    @InjectMocks
    private VaultRestController controller;

    private ObjectMapper objectMapper = new ObjectMapper();
    private User mockUser;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mockUser = new User();
        mockUser.setId(1L);
        when(authUtil.getCurrentUser()).thenReturn(mockUser);
    }

    @Test
    public void testGetAllEntries() throws Exception {
        VaultEntryDTO dto = new VaultEntryDTO();
        dto.setAccountName("Test Entry");
        when(vaultService.getAllEntries(1L, null, null, "name")).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/vault")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountName").value("Test Entry"));
    }

    @Test
    public void testAddEntry() throws Exception {
        VaultEntryDTO dto = new VaultEntryDTO();
        dto.setAccountName("New Entry");
        dto.setPassword("SecretPass123");

        mockMvc.perform(post("/api/vault")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Entry added successfully"));
    }
}
