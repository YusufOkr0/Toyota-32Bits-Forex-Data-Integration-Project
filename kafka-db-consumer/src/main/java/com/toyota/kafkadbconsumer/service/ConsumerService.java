package com.toyota.kafkadbconsumer.service;

import com.toyota.kafkadbconsumer.dtos.CalculatedRateDto;
import com.toyota.kafkadbconsumer.dtos.RawRateDto;

public interface ConsumerService {
    void consumeRawRate(RawRateDto rawRateDto);
    void consumeCalculatedRate(CalculatedRateDto calculatedRateDto);
}