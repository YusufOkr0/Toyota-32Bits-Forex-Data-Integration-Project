package com.toyota.kafkadbconsumer.service;

import com.toyota.kafkadbconsumer.dtos.CurrencyPair;

public interface ConsumerService {
    void consumeRawRate(CurrencyPair currencyPair);

    void consumeCalculatedRate(CurrencyPair currencyPair);
}