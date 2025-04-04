package com.toyota.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Rate {

    private String rateName;

    private BigDecimal bid;

    private BigDecimal ask;

    private LocalDateTime timestamp;

    private BigDecimal minLimit;

    private BigDecimal maxLimit;


    public Rate(String rateName, BigDecimal bid, BigDecimal ask, LocalDateTime timestamp) {
        this.rateName = rateName;
        this.bid = bid;
        this.ask = ask;
        this.timestamp = timestamp;
    }

    public Rate(String rateName, BigDecimal bid, BigDecimal ask, LocalDateTime timestamp,BigDecimal minLimit,BigDecimal maxLimit) {
        this.rateName = rateName;
        this.bid = bid;
        this.ask = ask;
        this.timestamp = timestamp;
        this.minLimit = minLimit;
        this.maxLimit = maxLimit;
    }



    public String getRateName() {
        return rateName;
    }

    public void setRateName(String rateName) {
        this.rateName = rateName;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public BigDecimal getMinLimit() {
        return minLimit;
    }

    public void setMinLimit(BigDecimal minLimit) {
        this.minLimit = minLimit;
    }

    public BigDecimal getMaxLimit() {
        return maxLimit;
    }

    public void setMaxLimit(BigDecimal maxLimit) {
        this.maxLimit = maxLimit;
    }
}
