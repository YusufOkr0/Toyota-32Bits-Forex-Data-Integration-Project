package com.toyota.kafkadbconsumer.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalculatedRate {

    private Long id;

    private String name;

    private BigDecimal bid;


    private BigDecimal ask;

    private LocalDateTime rateUpdateTime;

    private LocalDateTime dbUpdateTime;

    private void onUpdate(){
        dbUpdateTime = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "CalculatedRate{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", bid=" + bid +
                ", ask=" + ask +
                ", rateUpdateTime=" + rateUpdateTime +
                ", dbUpdateTime=" + dbUpdateTime +
                '}';
    }
}
