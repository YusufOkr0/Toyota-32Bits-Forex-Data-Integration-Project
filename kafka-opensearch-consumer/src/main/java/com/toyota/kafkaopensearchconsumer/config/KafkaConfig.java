package com.toyota.kafkaopensearchconsumer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.kafkaopensearchconsumer.entity.CurrencyPair;
import lombok.RequiredArgsConstructor;
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
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Configuration
@EnableKafka
public class KafkaConfig {

    private final ObjectMapper objectMapper;

    @Value("${kafka.custom.bootstrap-servers}") String bootstrapServer;
    @Value("${kafka.custom.consumer.group-id}") String consumerGroupId;
    @Value("${kafka.custom.consumer.raw.topic}") String rawRatesTopicName;
    @Value("${kafka.custom.consumer.calc.topic}") String calcRatesTopicName;
    @Value("${kafka.custom.consumer.auto-offset-reset}") String autoOffsetReset;


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


    private Map<String, Object> consumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.toyota.kafkaopensearchconsumer.entity");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, CurrencyPair.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        return props;
    }
    @Bean
    public ConsumerFactory<String, CurrencyPair> consumerFactory() {
        JsonDeserializer<CurrencyPair> jsonDeserializer = new JsonDeserializer<>(
                CurrencyPair.class,
                objectMapper
        );
        return new DefaultKafkaConsumerFactory<>(
                consumerProps(),
                new StringDeserializer(),
                jsonDeserializer
        );
    }


    @Bean
    public DefaultErrorHandler defaultErrorHandler() {
        ExponentialBackOff backOff = new ExponentialBackOff(2000L, 2.0);
        backOff.setMaxInterval(30000L);
        backOff.setMaxAttempts(4);
        return new DefaultErrorHandler(backOff);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CurrencyPair> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CurrencyPair> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(defaultErrorHandler());
        return factory;
    }


}
