package com.toyota.kafkadbconsumer.service.Impl;

import com.toyota.kafkadbconsumer.dtos.CalculatedRateDto;
import com.toyota.kafkadbconsumer.dtos.RawRateDto;
import com.toyota.kafkadbconsumer.entity.CalculatedRate;
import com.toyota.kafkadbconsumer.entity.RawRate;
import com.toyota.kafkadbconsumer.repository.CalculatedRateRepository;
import com.toyota.kafkadbconsumer.repository.RawRateRepository;
import com.toyota.kafkadbconsumer.service.ConsumerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumerServiceImpl implements ConsumerService {

    private final RawRateRepository rawRateRepository;
    private final CalculatedRateRepository calculatedRateRepository;

    @KafkaListener(
            topics = "${kafka.custom.consumer.raw.topic}",
            groupId = "${kafka.custom.consumer.raw.group-id}",
            containerFactory = "rawRatesKafkaListenerContainerFactory"
    )
    @Override
    public void consumeRawRate(RawRateDto rawRateDto) {
        try {
            if (rawRateDto == null) {
                log.warn("Received null CalculatedRateDto, skipping processing. Topic: {}", "${kafka.custom.consumer.calculated.topic}");
                return;
            }

            RawRate rawRate = RawRate.builder()
                    .name(rawRateDto.getName())
                    .bid(rawRateDto.getBid())
                    .ask(rawRateDto.getAsk())
                    .rateUpdateTime(rawRateDto.getTimestamp())
                    .dbUpdateTime(LocalDateTime.now())
                    .build();

            rawRateRepository.save(rawRate);

            log.info("Successfully saved RawRate: {}", rawRate);
        } catch (Exception e) {
            log.error("Error processing RawRateDto. DTO: {}, Error: {}", rawRateDto, e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "${kafka.custom.consumer.calculated.topic}",
            groupId = "${kafka.custom.consumer.calculated.group-id}",
            containerFactory = "calculatedRatesKafkaListenerContainerFactory"
    )
    @Override
    public void consumeCalculatedRate(CalculatedRateDto calculatedRateDto) {
        try {
            if (calculatedRateDto == null) {
                log.warn("Received null CalculatedRateDto, skipping processing. Topic: {}", "${kafka.custom.consumer.calculated.topic}");
                return;
            }

            CalculatedRate calculatedRate = CalculatedRate.builder()
                    .name(calculatedRateDto.getName())
                    .bid(calculatedRateDto.getBid())
                    .ask(calculatedRateDto.getAsk())
                    .rateUpdateTime(calculatedRateDto.getTimestamp())
                    .dbUpdateTime(LocalDateTime.now())
                    .build();
            calculatedRateRepository.save(calculatedRate);
            log.info("Successfully saved CalculatedRate: {}", calculatedRate);
        } catch (Exception e) {
            log.error("Error processing CalculatedRateDto. DTO: {}, Error: {}", calculatedRateDto, e.getMessage(), e);
        }

    }
}