package com.toyota.kafkaopensearchconsumer.service;

import com.toyota.kafkaopensearchconsumer.entity.CurrencyPair;

/**
 * Service interface for consuming Kafka messages containing CurrencyPair data.
 */
public interface ConsumerService {

    /**
     * Processes raw currency pair rate messages consumed from Kafka.
     *
     * @param currencyPair the object received from the raw rates topic
     * @param topic the Kafka topic name from which the message was consumed
     */
    void consumeRawRates(CurrencyPair currencyPair, String topic);

    /**
     * Processes calculated currency pair rate messages consumed from Kafka.
     *
     * @param currencyPair the object received from the calculated rates topic
     * @param topic the Kafka topic name from which the message was consumed
     */
    void consumeCalculatedRates(CurrencyPair currencyPair, String topic);
}
