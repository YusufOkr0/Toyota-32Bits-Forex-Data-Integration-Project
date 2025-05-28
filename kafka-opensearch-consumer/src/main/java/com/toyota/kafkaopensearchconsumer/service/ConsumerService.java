package com.toyota.kafkaopensearchconsumer.service;

import com.toyota.kafkaopensearchconsumer.entity.CurrencyPair;

public interface ConsumerService {
    void consumeRawRates(CurrencyPair currencyPair, String topic);

    void consumeCalculatedRates(CurrencyPair currencyPair, String topic);
}
