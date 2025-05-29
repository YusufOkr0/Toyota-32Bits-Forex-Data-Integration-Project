package com.toyota.restdataprovider.service;

import com.toyota.restdataprovider.dtos.request.RegisterRequest;
import com.toyota.restdataprovider.dtos.response.RegisterResponse;
import com.toyota.restdataprovider.entity.ForexUser;
import com.toyota.restdataprovider.entity.PricingPlan;
import com.toyota.restdataprovider.exception.InvalidPricingPlanException;
import com.toyota.restdataprovider.exception.TakenEmailException;
import com.toyota.restdataprovider.exception.UsernameAlreadyExistsException;
import com.toyota.restdataprovider.repository.UserRepository;
import com.toyota.restdataprovider.security.JwtUtil;
import com.toyota.restdataprovider.service.concretes.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private AuthServiceImpl authService;


    @Test
    void whenValidRegisterRequestProvided_ThenUserIsCreatedSuccessfully() {
        String email = "test@test.com";
        String username = "testUsername";
        String password = "testPassword";
        String encodedPassword = "encodedPassword";
        PricingPlan pricingPlan = PricingPlan.STANDARD;
        LocalDateTime now = LocalDateTime.now();

        RegisterRequest registerRequest = createRegisterRequest(email, username, password, pricingPlan.name());

        RegisterResponse registerResponse = RegisterResponse.builder()
                .username(username)
                .pricingPlan(pricingPlan.name())
                .build();


        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(modelMapper.map(any(ForexUser.class), eq(RegisterResponse.class))).thenReturn(registerResponse);


        RegisterResponse response = authService.signUp(registerRequest);


        assertNotNull(response);
        assertEquals(username, response.getUsername());
        assertEquals(pricingPlan.name(), response.getPricingPlan());

        verify(userRepository, times(1)).findByUsername(username);
        verify(userRepository, times(1)).findByEmail(email);
        verify(passwordEncoder, times(1)).encode(password);
        verify(userRepository, times(1)).save(any(ForexUser.class));
        verify(modelMapper, times(1)).map(any(ForexUser.class), eq(RegisterResponse.class));
    }


    @Test
    void whenRegisterWithExistsUsername_ThenThrowUsernameAlreadyExistsException() {
        String email = "test@test.com";
        String username = "testUsername";
        String password = "testPassword";
        String encodedPassword = "encodedPassword";
        PricingPlan pricingPlan = PricingPlan.STANDARD;
        LocalDateTime now = LocalDateTime.now();

        RegisterRequest registerRequest = createRegisterRequest(email, username, password, pricingPlan.name());
        ForexUser forexUser = createForexUser(email, username, encodedPassword, pricingPlan, now, now);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(forexUser));

        assertThrows(
                UsernameAlreadyExistsException.class,
                () -> authService.signUp(registerRequest)
        );

        verify(userRepository, times(1)).findByUsername(username);
        verify(userRepository, never()).findByEmail(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(ForexUser.class));
        verify(modelMapper, never()).map(any(), any());
    }


    @Test
    void whenRegisterWithExistsEmail_ThenThrowTakenEmailException() {
        String email = "test@test.com";
        String username = "testUsername";
        String password = "testPassword";
        String encodedPassword = "encodedPassword";
        PricingPlan pricingPlan = PricingPlan.STANDARD;
        LocalDateTime now = LocalDateTime.now();

        RegisterRequest registerRequest = createRegisterRequest(email, username, password, pricingPlan.name());
        ForexUser forexUser = createForexUser(email, username, encodedPassword, pricingPlan, now, now);


        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(forexUser));


        assertThrows(
                TakenEmailException.class,
                () -> authService.signUp(registerRequest)
        );

        verify(userRepository, times(1)).findByUsername(username);
        verify(userRepository, times(1)).findByEmail(email);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(ForexUser.class));
        verify(modelMapper, never()).map(any(), any());
    }


    @Test
    void whenRegisterWithInvalidPricingPlan_ThenThrowInvalidPricingPlanException() {
        String email = "test@test.com";
        String username = "testUsername";
        String password = "testPassword";
        String pricingPlan = "INVALID_PLAN";

        RegisterRequest registerRequest = createRegisterRequest(email, username, password, pricingPlan);


        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());


        assertThrows(
                InvalidPricingPlanException.class,
                () -> authService.signUp(registerRequest)
        );

        verify(userRepository, times(1)).findByUsername(username);
        verify(userRepository, times(1)).findByEmail(email);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(ForexUser.class));
        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void whenLoginWithValidCredentials_ThenReturnLoginResponse(){
        String username = "testUsername";
        String password = "testPassword";


        // TODO: CONTINUE :))
    }






    private RegisterRequest createRegisterRequest(String email, String username, String password, String pricingPlan) {
        return RegisterRequest.builder()
                .email(email)
                .username(username)
                .password(password)
                .pricingPlan(pricingPlan)
                .build();
    }

    private ForexUser createForexUser(String email, String username, String password, PricingPlan pricingPlan, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return ForexUser.builder()
                .email(email)
                .username(username)
                .password(password)
                .pricingPlan(pricingPlan)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
