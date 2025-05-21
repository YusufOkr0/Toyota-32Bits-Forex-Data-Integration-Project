package com.toyota.restdataprovider.entity;

import lombok.Getter;

@Getter
public enum PricingPlan {

    STANDARD( 20),
    PREMIUM( 60);

    private final int limitPerMinute;

    PricingPlan(int rateLimit){
        this.limitPerMinute = rateLimit;
    }
}

