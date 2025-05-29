package com.toyota.restdataprovider.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Builder
@RedisHash(value = "rest_platform_rates")
public class Rate {

    @Id
    private String name;

    private BigDecimal bid;

    private BigDecimal ask;

    private Instant timestamp;

    private BigDecimal minLimit;

    private BigDecimal maxLimit;

    @Override
    public String toString() {
        return "Rate{" +
                "ask=" + ask +
                ", name='" + name + '\'' +
                ", bid=" + bid +
                ", timestamp=" + timestamp +
                '}';
    }
}
