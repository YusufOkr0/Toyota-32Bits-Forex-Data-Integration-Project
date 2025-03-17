package com.toyota.restdataprovider.controller;

import com.toyota.restdataprovider.dtos.request.LoginRequest;
import com.toyota.restdataprovider.dtos.request.RegisterRequest;
import com.toyota.restdataprovider.dtos.response.LoginResponse;
import com.toyota.restdataprovider.dtos.response.RegisterResponse;
import com.toyota.restdataprovider.service.abstracts.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    @PostMapping(value = "/register",consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RegisterResponse> register(@RequestBody @Valid RegisterRequest registerRequest){

        RegisterResponse registerResponse = authService.signUp(registerRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(registerResponse);

    }


    @PostMapping(value = "/login",consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest){

        LoginResponse loginResponse = authService.login(loginRequest);

        return ResponseEntity
                .ok(loginResponse);

    }

}
