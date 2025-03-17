package com.toyota.restdataprovider.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @Email(message = "Invalid email format.")
    private String email;

    @NotBlank(message = "Username cannot be blank.")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters.")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores.")
    private String username;

    @NotBlank(message = "Password cannot be blank.")
    @Size(min = 8, message = "Password must be at least 8 characters.")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-zA-Z])(?=\\S+$).{8,}$",
            message = "Password must be at least 8 characters and contain at least one letter and one number.")
    private String password;

    @NotBlank(message = "Pricing plan cannot be blank.")
    @Pattern(regexp = "^(STANDARD|PREMIUM)$", message = "Invalid pricing plan. Valid plans: STANDARD, PREMIUM.")
    private String pricingPlan;
}