package com.passwordmanager.app.config;

import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.repository.IUserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final IUserRepository userRepository;
    private final com.passwordmanager.app.service.IVerificationService verificationService;

    public CustomAuthenticationSuccessHandler(IUserRepository userRepository,
            com.passwordmanager.app.service.IVerificationService verificationService) {
        this.userRepository = userRepository;
        this.verificationService = verificationService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        String username = authentication.getName();
        Optional<User> optUser = userRepository.findByUsername(username);

        if (optUser.isPresent() && optUser.get().isTotpEnabled()) {
            User user = optUser.get();
            // Trigger OTP send
            verificationService.generateAndSendOtp(user, "LOGIN_2FA");

            // Store pending userId and clear authentication so user is not fully logged in
            // yet
            HttpSession session = request.getSession(true);
            session.setAttribute("pending2faUserId", user.getId());
            SecurityContextHolder.clearContext();
            response.sendRedirect("/auth/2fa-login");
        } else {
            response.sendRedirect("/dashboard");
        }
    }
}
