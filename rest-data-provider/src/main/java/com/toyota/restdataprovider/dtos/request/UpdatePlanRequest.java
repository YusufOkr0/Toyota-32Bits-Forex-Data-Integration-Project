package com.toyota.restdataprovider.dtos.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdatePlanRequest {
    @NotBlank(message = "Pricing plan cannot be blank.")
    @Pattern(regexp = "^(STANDARD|PREMIUM)$", message = "Invalid pricing plan. Valid plans: STANDARD, PREMIUM.")
    private String newPricingPlan;
}
