package com.toyota.restdataprovider.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class Rate {

    private String name;

    private BigDecimal bid;

    private BigDecimal ask;

    private LocalDateTime timestamp;

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
