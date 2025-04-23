package com.toyota.restdataprovider.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "rest")

public class InitialRateConfig {

    Map<String,CurrencyRateConfig> rates;

    @Data
    public static class CurrencyRateConfig {
        private BigDecimal bid;
        private BigDecimal ask;
        private BigDecimal minLimit;
        private BigDecimal maxLimit;
    }
}
