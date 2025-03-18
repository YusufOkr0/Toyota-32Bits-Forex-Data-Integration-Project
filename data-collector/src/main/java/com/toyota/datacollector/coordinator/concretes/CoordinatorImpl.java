package com.toyota.datacollector.coordinator.concretes;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.datacollector.coordinator.abstracts.CoordinatorService;
import com.toyota.datacollector.entity.Rate;
import com.toyota.datacollector.entity.RateFields;
import com.toyota.datacollector.entity.RateStatus;
import com.toyota.datacollector.exception.*;
import com.toyota.datacollector.subscriber.abstracts.SubscriberService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CoordinatorImpl implements CoordinatorService {

    @Value("${subscribers.config.file}")
    private String CONFIG_FILE;

    private final ConcurrentHashMap<String, SubscriberService> subscribers;


    public CoordinatorImpl(){
        this.subscribers = new ConcurrentHashMap<>();
    }


    @PostConstruct
    public void initializer() {
        System.out.println("Initializing Coordinator...");
        loadSubscribers(CONFIG_FILE);

    }


    @Override
    public void onConnect(String platformName, Boolean status) {

    }

    @Override
    public void onDisConnect(String platformName, Boolean status) {
    }

    @Override
    public void onRateAvailable(String platformName, String rateName, Rate rate) {


    }

    @Override
    public void onRateUpdate(String platformName, String rateName, RateFields rateFields) {

    }


    @Override
    public void onRateStatus(String platformName, String rateName, RateStatus rateStatus) {

    }











    private void loadSubscribers(String configFile) {
        try (InputStream jsonFile = ClassLoader.getSystemResourceAsStream(configFile)) {
            if (jsonFile == null) {
                throw new ConfigurationFileNotFoundException("Configuration file not found: " + configFile);
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonFile);
            JsonNode subscribersNode = rootNode.get("subscribers");

            if (subscribersNode == null || !subscribersNode.isArray()) {
                throw new InvalidConfigFileException("'Subscribers' must be a non-null json file.");
            }

            subscribersNode.forEach(subscriberNode -> loadSubscriber(subscribersNode));

        } catch (IOException e) {
            throw new SubscriberLoadingException("Error reading configuration file: " + configFile, e);
        }
    }

    private void loadSubscriber(JsonNode subscriberNode) {
        String platformName = subscriberNode.get("platformName").asText();
        String className = subscriberNode.get("className").asText();

        if (platformName == null || className == null) {
            throw new InvalidConfigFileException("Invalid subscriber entry - missing platformName or className.");
        }

        try {
            Class<?> clazz = Class.forName(className);
            if (!SubscriberService.class.isAssignableFrom(clazz)) {
                throw new InvalidSubscriberClassException("Class " + className + " is not a valid SubscriberService implementation.");
            }

            SubscriberService subscriber = (SubscriberService) clazz
                    .getDeclaredConstructor(CoordinatorService.class)
                    .newInstance(this);

            subscribers.put(platformName, subscriber);

        } catch (ClassNotFoundException e) {
            throw new ClassLoadingException("Class not found: " + className, e);
        } catch (NoSuchMethodException e) {
            throw new ClassLoadingException("No constructor found that takes CoordinatorService in class: " + className, e);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new ClassLoadingException("Failed to instantiate class: " + className, e);
        }
    }


}
