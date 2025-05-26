package com.toyota.kafkadbconsumer.service.Impl;

import com.toyota.kafkadbconsumer.dtos.CurrencyPair;
import com.toyota.kafkadbconsumer.entity.CalculatedRate;
import com.toyota.kafkadbconsumer.entity.RawRate;
import com.toyota.kafkadbconsumer.repository.CalculatedRateRepository;
import com.toyota.kafkadbconsumer.repository.RawRateRepository;
import com.toyota.kafkadbconsumer.service.ConsumerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumerServiceImpl implements ConsumerService {

    private final RawRateRepository rawRateRepository;
    private final CalculatedRateRepository calculatedRateRepository;

    @KafkaListener(
            topics = "${kafka.custom.consumer.raw.topic}",
            groupId = "${kafka.custom.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Override
    public void consumeRawRate(CurrencyPair currencyPair) {
        try {
            if (currencyPair == null) {
                log.warn("Received null CalculatedRateDto, skipping processing. Topic: {}", "${kafka.custom.consumer.calculated.topic}");
                return;
            }

            RawRate rawRate = RawRate.builder()
                    .name(currencyPair.getName())
                    .bid(currencyPair.getBid())
                    .ask(currencyPair.getAsk())
                    .rateUpdateTime(currencyPair.getTimestamp())
                    .dbUpdateTime(Instant.now())
                    .build();

            rawRateRepository.save(rawRate);

            log.info("Successfully saved RawRate: {}", rawRate);
        } catch (Exception e) {
            log.error("Error processing RawRateDto. DTO: {}, Error: {}", currencyPair, e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "${kafka.custom.consumer.calc.topic}",
            groupId = "${kafka.custom.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Override
    public void consumeCalculatedRate(CurrencyPair currencyPair) {
        try {
            if (currencyPair == null) {
                log.warn("Received null CalculatedRateDto, skipping processing. Topic: {}", "${kafka.custom.consumer.calculated.topic}");
                return;
            }

            CalculatedRate calculatedRate = CalculatedRate.builder()
                    .name(currencyPair.getName())
                    .bid(currencyPair.getBid())
                    .ask(currencyPair.getAsk())
                    .rateUpdateTime(currencyPair.getTimestamp())
                    .dbUpdateTime(Instant.now())
                    .build();
            calculatedRateRepository.save(calculatedRate);
            log.info("Successfully saved CalculatedRate: {}", calculatedRate);
        } catch (Exception e) {
            log.error("Error processing CalculatedRateDto. DTO: {}, Error: {}", currencyPair, e.getMessage(), e);
        }

    }
}