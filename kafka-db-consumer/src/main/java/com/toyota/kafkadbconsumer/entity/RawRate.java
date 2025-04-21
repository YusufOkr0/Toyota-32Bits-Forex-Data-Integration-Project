package com.toyota.kafkadbconsumer.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RawRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rate_name")
    private String name;

    @Column(name = "bid")
    private BigDecimal bid;

    @Column(name = "ask")
    private BigDecimal ask;

    @Column(name = "rate_update_time")
    private LocalDateTime rateUpdateTime;

    @Column(name = "db_update_time")
    private LocalDateTime dbUpdateTime;

    @PrePersist
    private void onUpdate(){
        dbUpdateTime = LocalDateTime.now();
    }
}
