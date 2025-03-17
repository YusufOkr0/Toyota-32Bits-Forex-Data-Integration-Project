package com.toyota.restdataprovider.dtos.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdatePlanRequest {
    @NotBlank(message = "Pricing plan cannot be blank.")
    @Pattern(regexp = "^(STANDARD|PREMIUM)$", message = "Invalid pricing plan. Valid plans: STANDARD, PREMIUM.")
    private String newPricingPlan;
}
