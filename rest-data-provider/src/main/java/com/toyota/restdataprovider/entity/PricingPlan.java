package com.toyota.restdataprovider.entity;

import lombok.Getter;

@Getter
public enum PricingPlan {

    STANDARD( 20),
    PREMIUM( 100);

    private final int rateLimit;

    PricingPlan(int rateLimit){
        this.rateLimit = rateLimit;
    }
}

