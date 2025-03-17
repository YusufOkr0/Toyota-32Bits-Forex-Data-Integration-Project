package com.toyota.restdataprovider.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePlanResponse {
    private String username;

    private String pricingPlan;
}
