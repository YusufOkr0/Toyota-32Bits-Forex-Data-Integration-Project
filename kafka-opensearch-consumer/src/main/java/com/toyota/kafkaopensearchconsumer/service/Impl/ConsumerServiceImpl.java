package com.toyota.kafkaopensearchconsumer.service.Impl;

import com.toyota.kafkaopensearchconsumer.entity.CurrencyPair;
import com.toyota.kafkaopensearchconsumer.service.ConsumerService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ConsumerServiceImpl implements ConsumerService {

    private final OpenSearchServiceImpl openSearchService;


    @KafkaListener(
            topics = "${kafka.custom.consumer.raw.topic}",
            groupId = "${kafka.custom.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Override
    public void consumeRawRates(
            CurrencyPair currencyPair,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        String indexName = topic + "-" + LocalDate.now();

        System.out.println(currencyPair.toString());

        openSearchService.indexCurrencyPair(
                currencyPair,
                indexName
        );
    }

    @KafkaListener(
            topics = "${kafka.custom.consumer.calc.topic}",
            groupId = "${kafka.custom.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Override
    public void consumeCalculatedRates(
            CurrencyPair currencyPair,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        String indexName = topic + "-" + LocalDate.now();

        System.out.println(currencyPair.toString());
        openSearchService.indexCurrencyPair(
                currencyPair,
                indexName
        );
    }
}
