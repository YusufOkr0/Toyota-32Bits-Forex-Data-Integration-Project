package com.toyota.config;

import java.math.BigDecimal;

public class RateInfo {
    private String rateName;
    private BigDecimal bid;
    private BigDecimal ask;
    private BigDecimal minLimit;
    private BigDecimal maxLimit;

    public RateInfo(String rateName, BigDecimal maxLimit, BigDecimal minLimit, BigDecimal ask, BigDecimal bid) {
        this.rateName = rateName;
        this.maxLimit = maxLimit;
        this.minLimit = minLimit;
        this.ask = ask;
        this.bid = bid;
    }

    public RateInfo() {
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

    public BigDecimal getMaxLimit() {
        return maxLimit;
    }

    public void setMaxLimit(BigDecimal maxLimit) {
        this.maxLimit = maxLimit;
    }

    public BigDecimal getMinLimit() {
        return minLimit;
    }

    public void setMinLimit(BigDecimal minLimit) {
        this.minLimit = minLimit;
    }
}
