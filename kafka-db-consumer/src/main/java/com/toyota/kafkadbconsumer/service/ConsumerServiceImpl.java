package com.toyota.kafkadbconsumer.service;


import com.toyota.kafkadbconsumer.entity.CalculatedRate;
import com.toyota.kafkadbconsumer.entity.RawRate;
import com.toyota.kafkadbconsumer.repository.CalculatedRateRepository;
import com.toyota.kafkadbconsumer.repository.RawRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Service
@RequiredArgsConstructor
public class ConsumerServiceImpl {

    private final RawRateRepository rawRateRepository;
    private final CalculatedRateRepository calculatedRateRepository;

    @KafkaListener(
            topics = "${kafka.custom.consumer.raw.topic}",
            groupId = "${kafka.custom.consumer.raw.group-id}",
            containerFactory = "rawRatesKafkaListenerContainerFactory"
    )
    public void consumeRawRate(String rawRateMessage) {
        if(rawRateMessage == null || rawRateMessage.isBlank()){
            return;
        }

        RawRate rawRate = parseMessageToObject(
                rawRateMessage,
                RawRate.class
        );

        if(rawRate != null){
            rawRateRepository.save(rawRate);
        }
    }

    @KafkaListener(
            topics = "${kafka.custom.consumer.calculated.topic}",
            groupId = "${kafka.custom.consumer.calculated.group-id}",
            containerFactory = "calculatedRatesKafkaListenerContainerFactory"
    )
    public void consumeCalculatedRate(String calculatedRateMessage) {

        if(calculatedRateMessage == null || calculatedRateMessage.isBlank()){
            return;
        }

        CalculatedRate calculatedRate = parseMessageToObject(
                calculatedRateMessage,
                CalculatedRate.class
        );

        if(calculatedRate != null){
            calculatedRateRepository.save(calculatedRate);
        }

    }


    public <T> T parseMessageToObject(String rateMessage, Class<T> toRateType) {

        String [] rateParts = rateMessage.split("\\|");
        String rateName = rateParts[0];
        BigDecimal bid = new BigDecimal(rateParts[1]);
        BigDecimal ask = new BigDecimal(rateParts[2]);
        LocalDateTime timeStamp = LocalDateTime.parse(rateParts[3], DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        if(toRateType == RawRate.class){
            RawRate rawRate = RawRate.builder()
                    .name(rateName)
                    .bid(bid)
                    .ask(ask)
                    .rateUpdateTime(timeStamp)
                    .dbUpdateTime(LocalDateTime.now())
                    .build();

            return toRateType.cast(rawRate);

        } else if (toRateType == CalculatedRate.class) {
            CalculatedRate calculatedRate = CalculatedRate.builder()
                    .name(rateName)
                    .bid(bid)
                    .ask(ask)
                    .rateUpdateTime(timeStamp)
                    .dbUpdateTime(LocalDateTime.now())
                    .build();

            return toRateType.cast(calculatedRate);

        }else {
            return null;
        }
    }





}