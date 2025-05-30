package com.toyota.restdataprovider.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RegisterResponse {
    private String username;

    private String pricingPlan;

    private LocalDateTime createdAt;

    private String message;
}
