package com.toyota.kafkadbconsumer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "calculated_rates")
public class CalculatedRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rate_name")
    private String name;

    @Column(name = "bid", precision = 20, scale = 16)
    private BigDecimal bid;

    @Column(name = "ask", precision = 20, scale = 16)
    private BigDecimal ask;

    @Column(name = "rate_update_time")
    private LocalDateTime rateUpdateTime;

    @Column(name = "db_update_time")
    private LocalDateTime dbUpdateTime;

    @PrePersist
    private void onSave(){
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
