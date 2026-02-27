package com.revpasswordmanager_p2.app.rest;

import com.revpasswordmanager_p2.app.dto.LoginDTO;
import com.revpasswordmanager_p2.app.dto.RegisterDTO;
import com.revpasswordmanager_p2.app.entity.User;
import com.revpasswordmanager_p2.app.service.UserService;
import com.revpasswordmanager_p2.app.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public AuthRestController(AuthenticationManager authenticationManager, UserService userService, JwtUtil jwtUtil,
            UserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginDTO loginDTO,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Note: In a complete implementation, 2FA via REST would involve returning an
            // intermediate token
            // and an additional endpoint to verify the code. For simplicity here, we assume
            // standard login.
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDTO.getUsernameOrEmail(),
                            loginDTO.getMasterPassword()));

            UserDetails userDetails = userDetailsService.loadUserByUsername(authentication.getName());
            String token = jwtUtil.generateToken(userDetails);

            response.put("message", "Login successful");
            response.put("username", authentication.getName());
            response.put("token", token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Invalid credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterDTO registerDTO) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Skips email OTP verification in REST for simplicity,
            // or we could throw a custom status to tell the client to verify
            User user = userService.register(registerDTO);
            response.put("message", "Registration successful");
            response.put("username", user.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
