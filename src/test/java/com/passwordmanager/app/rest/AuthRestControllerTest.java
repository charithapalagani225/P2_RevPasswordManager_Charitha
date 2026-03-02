package com.passwordmanager.app.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.passwordmanager.app.dto.LoginDTO;
import com.passwordmanager.app.dto.RegisterDTO;
import com.passwordmanager.app.dto.SecurityQuestionDTO;
import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.rest.AuthRestController;
import com.passwordmanager.app.service.UserService;
import com.passwordmanager.app.util.JwtUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class AuthRestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthRestController authRestController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(authRestController).build();
    }

    @Test
    public void testLoginSuccess() throws Exception {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsernameOrEmail("testuser");
        loginDTO.setMasterPassword("password");

        Authentication authentication = new UsernamePasswordAuthenticationToken("testuser", "password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername("testuser")
                .password("password").authorities("ROLE_USER").build();
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn("mock-jwt-token");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    public void testRegisterSuccess() throws Exception {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("newuser");
        dto.setEmail("newuser@test.com");
        dto.setFullName("New User");
        dto.setMasterPassword("Password123!");
        dto.setConfirmPassword("Password123!");

        SecurityQuestionDTO sq1 = new SecurityQuestionDTO();
        sq1.setQuestionText("Q1");
        sq1.setAnswer("A1");
        SecurityQuestionDTO sq2 = new SecurityQuestionDTO();
        sq2.setQuestionText("Q2");
        sq2.setAnswer("A2");
        SecurityQuestionDTO sq3 = new SecurityQuestionDTO();
        sq3.setQuestionText("Q3");
        sq3.setAnswer("A3");
        dto.setSecurityQuestions(Arrays.asList(sq1, sq2, sq3));

        User mockUser = new User();
        mockUser.setUsername("newuser");

        when(userService.register(any(RegisterDTO.class))).thenReturn(mockUser);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.username").value("newuser"));
    }
}
