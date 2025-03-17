package com.toyota.restdataprovider.service.abstracts;

import com.toyota.restdataprovider.dtos.request.LoginRequest;
import com.toyota.restdataprovider.dtos.request.RegisterRequest;
import com.toyota.restdataprovider.dtos.response.LoginResponse;
import com.toyota.restdataprovider.dtos.response.RegisterResponse;

public interface AuthService {

    RegisterResponse signUp(RegisterRequest registerRequest);

    LoginResponse login(LoginRequest loginRequest);

}
