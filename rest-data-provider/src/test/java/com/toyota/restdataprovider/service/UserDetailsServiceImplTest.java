package com.toyota.restdataprovider.service;

import com.toyota.restdataprovider.entity.ForexUser;
import com.toyota.restdataprovider.repository.UserRepository;
import com.toyota.restdataprovider.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;



    @Test
    void testLoadUserByUsername_WhenUserExists_ShouldReturnUserDetails() {
        String testUsername = "testuser";
        String testPassword = "encodedpassword";

        ForexUser mockForexUser = new ForexUser();
        mockForexUser.setUsername(testUsername);
        mockForexUser.setPassword(testPassword);


        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(mockForexUser));


        UserDetails userDetails = userDetailsService.loadUserByUsername(testUsername);


        assertNotNull(userDetails);
        assertEquals(testUsername, userDetails.getUsername());
        assertEquals(testPassword, userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().isEmpty());

        verify(userRepository, times(1)).findByUsername(testUsername);
    }

    @Test
    void testLoadUserByUsername_WhenUserDoesNotExist_ShouldThrowUsernameNotFoundException() {
        String testUsername = "testuser";

        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.empty());


        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(testUsername)
        );

        assertEquals("User not found with username: " + testUsername, exception.getMessage());

        verify(userRepository, times(1)).findByUsername(testUsername);
    }
}
