package com.toyota.kafkaopensearchconsumer.service;

import com.toyota.kafkaopensearchconsumer.entity.CurrencyPair;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ConsumerServiceImpl {


    @KafkaListener(
            topics = "{kafka.custom.consumer.raw.topic}",
            groupId = "${kafka.custom.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeRawRates(CurrencyPair message) {
        // TODO: OPENSEARCH'E YAZ.
    }

    @KafkaListener(
            topics = "${kafka.custom.consumer.calculated.topic}",
            groupId = "${kafka.custom.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCalculatedRates(CurrencyPair message) {
        // TODO: OPENSEARCH'E YAZ.
    }
}
