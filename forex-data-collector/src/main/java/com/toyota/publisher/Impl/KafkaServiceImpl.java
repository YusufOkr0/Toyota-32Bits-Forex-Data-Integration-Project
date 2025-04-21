package com.toyota.publisher.Impl;

import com.toyota.config.ApplicationConfig;
import com.toyota.entity.CalculatedRate;
import com.toyota.entity.Rate;
import com.toyota.publisher.KafkaService;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import java.util.Properties;

public class KafkaServiceImpl implements KafkaService {

    private final String kafkaBootstrapServers;
    private final String rawRateTopic;
    private final String calculatedRateTopic;


    private KafkaProducer<String, String > forexRatePublisher;


    public KafkaServiceImpl(ApplicationConfig appConfig){

        this.kafkaBootstrapServers = appConfig.getValue("kafka.bootstrap.servers");
        this.rawRateTopic = appConfig.getValue("kafka.topic.raw");
        this.calculatedRateTopic = appConfig.getValue("kafka.topic.calculated");

        setUpKafkaConfigurations();
    }


    @Override
    public void sendRawRate(Rate rawRate){

        String formattedRawRate = String.format(
                "%s|%s|%s|%s",
                rawRate.getName(),
                rawRate.getBid(),
                rawRate.getAsk(),
                rawRate.getTimestamp()
        );

        ProducerRecord<String,String> rawRateRecord = new ProducerRecord<>(
                rawRateTopic,
                formattedRawRate
        );
        forexRatePublisher.send(rawRateRecord, (recordMetadata, e) -> {
            if(e != null){
                System.err.printf("ERROR WHILE SENDING RAW RATE TO KAFKA: %s\n",e.getMessage());
            }
        });

    }

    @Override
    public void sendCalculatedRate(CalculatedRate calculatedRate){

        String formattedRawRate = String.format(
                "%s|%s|%s|%s",
                calculatedRate.getName(),
                calculatedRate.getBid(),
                calculatedRate.getAsk(),
                calculatedRate.getTimestamp()
        );

        ProducerRecord<String,String> calculatedRateRecord = new ProducerRecord<>(
                calculatedRateTopic,
                formattedRawRate
        );
        forexRatePublisher.send(calculatedRateRecord, (recordMetadata, e) -> {
            if(e != null){
                System.err.printf("ERROR WHILE SENDING RAW RATE TO KAFKA: %s\n",e.getMessage());
            }
        });
    }



    private void setUpKafkaConfigurations(){
        Properties kafkaConfigs = new Properties();
        kafkaConfigs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        kafkaConfigs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaConfigs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaConfigs.put("max.block.ms", "5000");

        forexRatePublisher = new KafkaProducer<>(kafkaConfigs);
    }



}
