package com.toyota.restdataprovider.security;

import com.toyota.restdataprovider.entity.ForexUser;
import com.toyota.restdataprovider.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collections;

@RequiredArgsConstructor
@Component
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        return userRepository.findByUsername(username)
                .map(this::buildUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

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
