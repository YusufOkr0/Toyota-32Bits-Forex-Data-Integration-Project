package com.toyota.kafkadbconsumer.service;


import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


@Service
public class ConsumerServiceImpl {


    @KafkaListener(
            topics = "${app.kafka.topic.raw}"
    )
    public void consumeRawRate(String rawRate) {
        System.out.println("Consumed Raw Rate: " + rawRate);
    }

    @KafkaListener(
            topics = "${app.kafka.topic.calculated}"
    )
    public void consumeCalculatedRate(String calculatedRate) {
        System.out.println("Consumed Calculated Rate: " + calculatedRate);
    }
}