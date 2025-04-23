package com.toyota.restdataprovider.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;


    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        String username = authentication.getName();
        String password = (String) authentication.getCredentials();

        log.info("Attempting to authenticate user: {}", username);

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if(!passwordEncoder.matches(password,userDetails.getPassword())){
            log.warn("Invalid password attempt for user: {}", username);
            throw new BadCredentialsException(String.format("Given password is invalid for the user: {%s}",username));
        }

        log.info("User authenticated successfully: {}", username);
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                password,
                userDetails.getAuthorities()    // There is no authorization logic for now.
        );

    }



    /***
     * GIVES THE INFORMATION ABOUT AUTHENTICATION TYPE OF THE AUTHENTICATION PROVIDER.
     * @param authentication
     * @return
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.isAssignableFrom(UsernamePasswordAuthenticationToken.class);
    }
}
