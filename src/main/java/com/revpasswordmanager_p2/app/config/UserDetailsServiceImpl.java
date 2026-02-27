package com.revpasswordmanager_p2.app.config;

import com.revpasswordmanager_p2.app.entity.User;
import com.revpasswordmanager_p2.app.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrEmail));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getMasterPasswordHash(),
                user.isEmailVerified(), // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                !user.isAccountLocked(), // accountNonLocked
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
