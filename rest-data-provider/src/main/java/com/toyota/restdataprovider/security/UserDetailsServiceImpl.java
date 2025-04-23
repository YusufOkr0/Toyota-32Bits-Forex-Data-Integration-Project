package com.toyota.restdataprovider.security;

import com.toyota.restdataprovider.entity.ForexUser;
import com.toyota.restdataprovider.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
@Component
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Attempting to load user with username: {}", username);

        return userRepository.findByUsername(username)
                .map(user -> {
                    log.debug("User found: {}", user.getUsername());
                    return buildUserDetails(user);
                })
                .orElseThrow(() -> {
                    log.warn("User not found with username: {}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

    }

    private UserDetails buildUserDetails(ForexUser forexUser) {
        return User.builder()
                .username(forexUser.getUsername())
                .password(forexUser.getPassword())      // Encoded password
                .authorities(Collections.emptyList())   // NO AUTHORIZATION. IF NEEDED THEN ADJUST HERE LATER.
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}
