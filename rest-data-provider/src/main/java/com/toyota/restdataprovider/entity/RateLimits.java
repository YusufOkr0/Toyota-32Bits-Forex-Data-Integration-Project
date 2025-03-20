package com.toyota.restdataprovider.entity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RateLimits {
    private final BigDecimal minLimit;
    private final BigDecimal maxLimit;

    public RateLimits(BigDecimal minLimit, BigDecimal maxLimit) {
        this.minLimit = minLimit;
        this.maxLimit = maxLimit;
    }

}