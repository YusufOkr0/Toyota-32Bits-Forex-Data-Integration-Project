package com.toyota.restdataprovider.service;

import com.toyota.restdataprovider.dtos.request.LoginRequest;
import com.toyota.restdataprovider.dtos.request.RegisterRequest;
import com.toyota.restdataprovider.dtos.response.LoginResponse;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private Authentication authentication;
    @Mock
    private UserDetails userDetails;
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
        PricingPlan pricingPlan = PricingPlan.STANDARD;

        RegisterRequest registerRequest = createRegisterRequest(email, username, password, pricingPlan.name());

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(new ForexUser()));

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
        PricingPlan pricingPlan = PricingPlan.STANDARD;

        RegisterRequest registerRequest = createRegisterRequest(email, username, password, pricingPlan.name());
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(new ForexUser()));


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
        String expectedJwtToken = "THIS_IS_A_JWT_TOKEN";

        LoginRequest loginRequest = new LoginRequest(username, password);


        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtil.generateJwtToken(userDetails)).thenReturn(expectedJwtToken);


        LoginResponse response = authService.login(loginRequest);


        assertNotNull(response);
        assertEquals(expectedJwtToken, response.getApiKey());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(authentication, times(1)).getPrincipal();
        verify(jwtUtil, times(1)).generateJwtToken(userDetails);
    }


    @Test
    void whenLoginWithInvalidCredentials_ThenThrowBadCredentialsException() {
        String username = "invalidUsername";
        String password = "orInvalidPassword";
        LoginRequest loginRequest = new LoginRequest(username, password);


        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new AuthenticationException("Username or password not valid") {});

        BadCredentialsException ex = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequest)
        );
        assertEquals("Invalid credentials: Username or password not valid",ex.getMessage());


        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil, never()).generateJwtToken(any());
    }







    private RegisterRequest createRegisterRequest(String email, String username, String password, String pricingPlan) {
        return RegisterRequest.builder()
                .email(email)
                .username(username)
                .password(password)
                .pricingPlan(pricingPlan)
                .build();
    }


}
