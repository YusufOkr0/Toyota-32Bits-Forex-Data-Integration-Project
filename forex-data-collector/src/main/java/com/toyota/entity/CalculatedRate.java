package com.toyota.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public class CalculatedRate {
    private String name;
    private BigDecimal bid;
    private BigDecimal ask;
    private Instant timestamp;

    public CalculatedRate() {
    }

    public CalculatedRate(String name, BigDecimal bid, BigDecimal ask, Instant timestamp) {
        this.name = name;
        this.bid = bid;
        this.ask = ask;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getBid() {
        return bid;
    }

    public void setBid(BigDecimal bid) {
        this.bid = bid;
    }

    public BigDecimal getAsk() {
        return ask;
    }

    public void setAsk(BigDecimal ask) {
        this.ask = ask;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Rate{" +
                "name='" + name + '\'' +
                ", bid=" + bid +
                ", ask=" + ask +
                ", timestamp=" + timestamp +
                '}';
    }
}
