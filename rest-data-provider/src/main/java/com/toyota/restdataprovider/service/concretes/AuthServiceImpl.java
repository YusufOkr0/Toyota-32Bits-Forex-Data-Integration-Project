package com.toyota.restdataprovider.service.concretes;

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
import com.toyota.restdataprovider.service.abstracts.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;


    @Override
    public RegisterResponse signUp(RegisterRequest registerRequest) {
        log.info("Processing registration request for username: {}", registerRequest.getUsername());

        validateRegisterRequest(registerRequest);

        String pricingPlanStr = registerRequest
                .getPricingPlan()
                .toUpperCase();
        log.debug("Creating user with pricing plan: {}", pricingPlanStr);

        ForexUser forexUser = ForexUser.builder()
                .email(registerRequest.getEmail())
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .pricingPlan(PricingPlan.valueOf(pricingPlanStr))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRepository.save(forexUser);


        RegisterResponse registerResponse = modelMapper
                .map(forexUser,RegisterResponse.class);

        registerResponse.setMessage(String.format("""
        Hey %s!
        
        You've just unlocked a treasure chest of Forex data!
        But before you start stacking profits, you'll need your golden key — the access token.
        
        Log in to claim your token and sail smoothly in the ocean of market trends.
        """, registerRequest.getUsername()));

        log.info("Registration completed successfully for username: {}", registerRequest.getUsername());
        return registerResponse;
    }



    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        log.info("Processing login request for username: {}", loginRequest.getUsername());

        try {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
            );

            Authentication authentication = authenticationManager.authenticate(authToken);   // user wants to login. authentication provider will implement authenticate function.
                                                                                            // make a custom provider or use exist ones. (ex = DAO ...)
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String jwtToken = jwtUtil.generateJwtToken(userDetails);


            log.info("Login successful for username: {}", loginRequest.getUsername());
            return new LoginResponse(jwtToken);

        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid credentials: " + e.getMessage());
        }

    }








    private void validateRegisterRequest(RegisterRequest registerRequest) {
        log.debug("Validating registration request for username: {}", registerRequest.getUsername());
        String username = registerRequest.getUsername();
        String email = registerRequest.getEmail();

        userRepository.findByUsername(username)
                .ifPresent(ex -> {
                    log.warn("Username already exists: {}", username);
                    throw new UsernameAlreadyExistsException(String.format("There is already a user in the system with the username: {%s} ",username));
                });
        userRepository.findByEmail(email)
                .ifPresent(forexUser -> {
                    log.warn("Email already taken: {}", email);
                    throw new TakenEmailException(String.format("Given email: {%s} is taken",email));
                });

        String comingPlan = registerRequest.getPricingPlan().toUpperCase();

        try {
            PricingPlan.valueOf(comingPlan);
            log.debug("Pricing plan validated: {}", comingPlan);
        }catch (IllegalArgumentException exception){
            log.warn("Invalid pricing plan: {}", comingPlan);
            throw new InvalidPricingPlanException(String.format("Given plan: %s does not exists", comingPlan));
        }

    }
}
