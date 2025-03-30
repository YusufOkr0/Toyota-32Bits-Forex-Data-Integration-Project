package com.toyota.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Rate {
    private final String name;
    private final BigDecimal bid;
    private final BigDecimal ask;
    private final LocalDateTime timestamp;

    public Rate(String name, BigDecimal bid, BigDecimal ask, LocalDateTime timestamp) {
        this.name = name;
        this.bid = bid;
        this.ask = ask;
        this.timestamp = timestamp;
    }



    @Override
    public String toString() {
        return "Rate{ask=" + ask + ", name='" + name + "', bid=" + bid + ", timestamp=" + timestamp + "}";
    }
}