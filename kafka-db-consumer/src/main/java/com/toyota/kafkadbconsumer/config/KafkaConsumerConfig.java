package com.toyota.kafkadbconsumer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.kafkadbconsumer.dtos.CurrencyPair;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerConfig {

    @Value("${kafka.custom.bootstrap-servers}")
    String bootstrapServer;
    @Value("${kafka.custom.consumer.group-id}")
    String consumerGroupId;
    @Value("${kafka.custom.consumer.raw.topic}")
    String rawRatesTopicName;
    @Value("${kafka.custom.consumer.calc.topic}")
    String calcRatesTopicName;
    @Value("${kafka.custom.consumer.auto-offset-reset}")
    String autoOffsetReset;

    private final ObjectMapper objectMapper;

    @Bean
    public KafkaAdmin admin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        return new KafkaAdmin(configs);
    }

    @Bean
    NewTopic topic1() {
        return TopicBuilder.name(rawRatesTopicName)
                .partitions(1)
                .replicas(1)
                .build();
    }
    @Bean
    NewTopic topic2() {
        return TopicBuilder.name(calcRatesTopicName)
                .partitions(1)
                .replicas(1)
                .build();
    }

    Map<String, Object> consumerProperties(){
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServer);
        props.put(ConsumerConfig.GROUP_ID_CONFIG,consumerGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.toyota.kafkadbconsumer.dtos");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, CurrencyPair.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        return props;
    }

    @Bean
    ConsumerFactory<String,CurrencyPair> consumerFactory(){
        JsonDeserializer<CurrencyPair> jsonDeserializer = new JsonDeserializer<>(
                CurrencyPair.class,
                objectMapper
        );
        return new DefaultKafkaConsumerFactory<>(
                consumerProperties(),
                new StringDeserializer(),
                jsonDeserializer
        );
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, CurrencyPair> kafkaListenerContainerFactory(){
        ConcurrentKafkaListenerContainerFactory<String, CurrencyPair> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        return factory;
    }

}