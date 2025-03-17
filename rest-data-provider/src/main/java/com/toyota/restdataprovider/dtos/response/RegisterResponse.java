package com.toyota.restdataprovider.dtos.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RegisterResponse {
    private String username;

    private String pricingPlan;

    private LocalDateTime createdAt;

    private String message;
}
