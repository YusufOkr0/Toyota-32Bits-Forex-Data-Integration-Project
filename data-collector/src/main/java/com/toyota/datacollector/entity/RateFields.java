package com.toyota.datacollector.entity;



import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RateFields {

    private BigDecimal bid;

    private BigDecimal ask;

    private LocalDateTime timestamp;

    public RateFields(BigDecimal bid,BigDecimal ask,LocalDateTime timestamp){
        this.bid = bid;
        this.ask = ask;
        this.timestamp = timestamp;
    }


}
