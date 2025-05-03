package com.toyota.publisher;

import com.toyota.entity.CalculatedRate;
import com.toyota.entity.Rate;


/**
 * KafkaService defines a contract for publishing forex rate data to Kafka topics.
 * <p>
 * Implementations of this interface are responsible for sending both raw and calculated
 * exchange rate data to external systems through Kafka.
 * </p>
 */
public interface KafkaService {

    /**
     * Publishes a raw exchange rate to the designated Kafka topic.
     *
     * @param rawRate the raw exchange rate to be published
     */
    void sendRawRate(Rate rawRate);

    /**
     * Publishes a calculated or derived exchange rate to the designated Kafka topic.
     *
     * @param calculatedRate the derived/calculated exchange rate to be published
     */
    void sendCalculatedRate(CalculatedRate calculatedRate);
}
