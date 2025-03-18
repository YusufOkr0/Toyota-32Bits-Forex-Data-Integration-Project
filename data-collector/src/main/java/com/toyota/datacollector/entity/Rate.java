package com.toyota.datacollector.entity;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
public class Rate {
    private String rateName;

    private BigDecimal bid;

    private BigDecimal ask;

    private LocalDateTime timestamp;

    public Rate(String currencyPair, BigDecimal bid, BigDecimal ask, LocalDateTime timestamp) {
        this.rateName = currencyPair;
        this.bid = bid;
        this.ask = ask;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Rate{" +
                "ask=" + ask +
                ", rateName='" + rateName + '\'' +
                ", bid=" + bid +
                ", timestamp=" + timestamp +
                '}';
    }


}
