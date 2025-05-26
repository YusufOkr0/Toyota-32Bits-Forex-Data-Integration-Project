package com.toyota.restdataprovider.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RateDto {
    private String name;

    private BigDecimal bid;

    private BigDecimal ask;

    private Instant timestamp;
}
