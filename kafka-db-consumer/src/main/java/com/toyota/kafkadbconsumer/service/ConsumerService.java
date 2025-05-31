package com.toyota.kafkadbconsumer.service;

import com.toyota.kafkadbconsumer.dtos.CurrencyPair;

/**
 * Service interface for consuming and processing currency pair rate messages from Kafka.
 */
public interface ConsumerService {

    /**
     * Consumes raw rate messages from Kafka and processes them.
     *
     * @param currencyPair the CurrencyPair data transfer object representing raw rate info
     */
    void consumeRawRate(CurrencyPair currencyPair);

    /**
     * Consumes calculated rate messages from Kafka and processes them.
     *
     * @param currencyPair the CurrencyPair data transfer object representing calculated rate info
     */
    void consumeCalculatedRate(CurrencyPair currencyPair);
}
