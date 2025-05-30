package com.toyota.publisher.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.toyota.config.ApplicationConfig;
import com.toyota.entity.CalculatedRate;
import com.toyota.entity.Rate;
import com.toyota.exception.ConnectionException;
import com.toyota.publisher.KafkaService;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;

public class KafkaServiceImpl implements KafkaService {

    public static final Logger logger = LogManager.getLogger(KafkaServiceImpl.class);

    private final String kafkaBootstrapServers;
    private final String rawRateTopic;
    private final String calculatedRateTopic;

    private final ObjectMapper objectMapper;

    private KafkaProducer<String, String> forexRatePublisher;

    public KafkaServiceImpl(ApplicationConfig appConfig) {
        this.kafkaBootstrapServers = appConfig.getValue("kafka.bootstrap.servers");
        this.rawRateTopic = appConfig.getValue("kafka.topic.raw");
        this.calculatedRateTopic = appConfig.getValue("kafka.topic.calculated");

        this.objectMapper = configureObjectMapper();
        setUpKafkaConfigurations();
    }

    @Override
    public void sendRawRate(Rate rawRate) {
        logger.trace("sendRawRate: Sending raw rate to Kafka topic: {}, rate name: {}", rawRateTopic, rawRate.getName());

        try {
            String formattedRawRate = objectMapper.writeValueAsString(rawRate);
            ProducerRecord<String, String> rawRateRecord = new ProducerRecord<>(
                    rawRateTopic,
                    formattedRawRate
            );

            forexRatePublisher.send(rawRateRecord, (recordMetadata, e) -> {
                if (e != null) {
                    logger.error("sendRawRate: Error sending raw rate to Kafka topic: {}, rate name: {}, error: {}",
                            rawRateTopic, rawRate.getName(), e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            logger.error("sendRawRate: JSON serialization failed for raw rate: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendCalculatedRate(CalculatedRate calculatedRate) {
        logger.trace("sendCalculatedRate: Sending calculated rate to Kafka topic: {}, rate name: {}", calculatedRateTopic, calculatedRate.getName());

        try {
            String formattedCalculatedRate = objectMapper.writeValueAsString(calculatedRate);
            ProducerRecord<String, String> calculatedRateRecord = new ProducerRecord<>(
                    calculatedRateTopic,
                    formattedCalculatedRate
            );

            forexRatePublisher.send(calculatedRateRecord, (recordMetadata, e) -> {
                if (e != null) {
                    logger.error("sendCalculatedRate: Error sending calculated rate to Kafka topic: {}, rate name: {}, error: {}",
                            calculatedRateTopic, calculatedRate.getName(), e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            logger.error("sendCalculatedRate: JSON serialization failed for calculated rate: {}", e.getMessage(), e);
        }
    }


    private ObjectMapper configureObjectMapper(){
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    private void setUpKafkaConfigurations() {
        Properties kafkaConfigs = new Properties();
        kafkaConfigs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        kafkaConfigs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaConfigs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaConfigs.put("max.block.ms", "5000");

        try {
            forexRatePublisher = new KafkaProducer<>(kafkaConfigs);
            forexRatePublisher.partitionsFor(rawRateTopic);
        } catch (Exception e) {
            throw new ConnectionException("Unable to connect to Kafka.");
        }
    }
}
