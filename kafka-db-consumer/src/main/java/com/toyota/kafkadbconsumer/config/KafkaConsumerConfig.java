package com.toyota.kafkadbconsumer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.kafkadbconsumer.dtos.CalculatedRateDto;
import com.toyota.kafkadbconsumer.dtos.RawRateDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${kafka.custom.bootstrap-servers}")
    private String bootstrapServer;

    @Value("${kafka.custom.consumer.raw.group-id}")
    private String RawRatesConsumerGroup;

    @Value("${kafka.custom.consumer.calculated.group-id}")
    private String calculatedRatesConsumerGroup;

    @Value("${kafka.custom.consumer.auto-offset-reset}")
    private String autoOffsetReset;

    @Bean
    public ConsumerFactory<String, RawRateDto> rawRateDtosConsumerFactory(ObjectMapper objectMapper) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, RawRatesConsumerGroup);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.toyota.kafkadbconsumer.dtos");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, RawRateDto.class.getName());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);

        JsonDeserializer<RawRateDto> deserializer = new JsonDeserializer<>(
                RawRateDto.class,
                objectMapper
        );
        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RawRateDto> rawRatesKafkaListenerContainerFactory(ObjectMapper objectMapper) {
        ConcurrentKafkaListenerContainerFactory<String, RawRateDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(rawRateDtosConsumerFactory(objectMapper));
        return factory;
    }

    @Bean
    public ConsumerFactory<String, CalculatedRateDto> calculatedRatesConsumerFactory(ObjectMapper objectMapper) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, calculatedRatesConsumerGroup);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.toyota.kafkadbconsumer.dtos");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, CalculatedRateDto.class.getName());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);

        JsonDeserializer<CalculatedRateDto> deserializer = new JsonDeserializer<>(
                CalculatedRateDto.class,
                objectMapper
        );
        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CalculatedRateDto> calculatedRatesKafkaListenerContainerFactory(ObjectMapper objectMapper) {
        ConcurrentKafkaListenerContainerFactory<String, CalculatedRateDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(calculatedRatesConsumerFactory(objectMapper));
        return factory;
    }
}