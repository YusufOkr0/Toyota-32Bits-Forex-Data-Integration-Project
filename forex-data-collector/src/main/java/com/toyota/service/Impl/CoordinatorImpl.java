package com.toyota.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.config.ApplicationConfig;
import com.toyota.service.CoordinatorService;
import com.toyota.entity.Rate;
import com.toyota.exception.*;
import com.toyota.service.RateManager;
import com.toyota.service.SubscriberService;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CoordinatorImpl implements CoordinatorService {

    private static final int THREAD_POOL_SIZE = 10;

    private final String subscribersConfigFile;
    private final int connectionRetryLimit;

    private final List<String> exchangeRates;
    private final Map<String, Integer> retryCounts;

    private final RateManager rateManager;
    private final ApplicationConfig appConfig;
    private final ExecutorService executorService;
    private final Map<String, SubscriberService> subscribers;

    public CoordinatorImpl(RateManager rateManager, ApplicationConfig applicationConfig) {
        this.appConfig = applicationConfig;
        this.subscribersConfigFile = appConfig.getValue("subscribers.config.file");
        this.connectionRetryLimit = appConfig.getIntValue("connection.retry.limit");
        this.exchangeRates = appConfig.getExchangeRates();

        this.rateManager = rateManager;
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        this.subscribers = new ConcurrentHashMap<>();
        this.retryCounts = new ConcurrentHashMap<>();


        // create the context for per thread.
        for(int i = 1; i <= THREAD_POOL_SIZE; i++){
            executorService.execute(rateManager::warmUpCalculationService);
        }
        loadSubscribers();
        startSubscribers();
    }


    @Override
    public void onConnect(String platformName, Boolean status) {
        executorService.execute(() -> {
            System.out.printf("Platform: %s connection status is: %s\n", platformName, status);
            if (status) {
                retryCounts.put(platformName, 0);
                SubscriberService subscriber = subscribers.get(platformName);
                exchangeRates.forEach(rate -> {
                    subscriber.subscribe(platformName, rate);
                });
            } else {
                retryToConnectWithDelay(platformName);
            }
        });
    }

    @Override
    public void onDisConnect(String platformName) {
        executorService.execute(() -> {
            retryToConnectWithDelay(platformName);
        });
    }

    @Override
    public void onRateAvailable(String platformName, String rateName, Rate rate) {
        executorService.execute(() -> rateManager.handleFirstInComingRate(platformName, rateName, rate));
    }

    @Override
    public void onRateUpdate(String platformName, String rateName, Rate rate) {
        executorService.execute(() -> rateManager.handleRateUpdate(platformName, rateName, rate));
    }


    private void retryToConnectWithDelay(String platformName) {
        int retryCount = retryCounts.getOrDefault(platformName, 0);

        if (retryCount >= connectionRetryLimit) {
            System.err.printf("Maximum retry limit (%d) reached for platform '%s'. Connection attempts abandoned.%n",
                    connectionRetryLimit, platformName);
            // TODO:: ADD MAIL LOGIC.
            return;
        }

        retryCounts.put(platformName, retryCount + 1);

        try {
            System.out.printf("Retrying to connect to '%s' in 10 seconds (attempt %d/%d)%n",
                    platformName, retryCount + 1, connectionRetryLimit);
            Thread.sleep(10_000);

            subscribers.get(platformName).connect(platformName);

        } catch (InterruptedException e) {
            System.err.printf("Reconnect interrupted for '%s': %s%n", platformName, e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private void startSubscribers() {
        subscribers.forEach((platformName, subscriber) -> {
            executorService.execute(() -> subscriber.connect(platformName));
        });
    }

    private void loadSubscribers() {
        try (InputStream jsonFile = CoordinatorImpl.class.getClassLoader().getResourceAsStream(subscribersConfigFile)) {
            if (jsonFile == null) {
                throw new ConfigFileNotFoundException(String.format("Configuration file '%s' not found in the classpath.", subscribersConfigFile));
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonFile);
            JsonNode subscribersNode = rootNode.get("subscribers");

            if (subscribersNode == null || !subscribersNode.isArray()) {
                throw new InvalidConfigFileException(String.format("Invalid configuration file '%s': 'subscribers' field must be a non-null JSON array.", subscribersConfigFile));
            }

            subscribersNode.forEach(this::loadSubscriber);

        } catch (IOException e) {
            throw new ConfigFileLoadingException(String.format("Failed to read configuration file '%s': %s", subscribersConfigFile, e.getMessage()));
        }
    }

    private void loadSubscriber(JsonNode subscriberNode) {
        String platformName = subscriberNode.path("platformName").asText(null);
        String className = subscriberNode.path("className").asText(null);

        if (platformName == null || className == null) {
            throw new InvalidConfigFileException(String.format("Invalid subscriber entry in config file '%s': 'platformName' or 'className' is missing.", subscribersConfigFile));
        }

        try {
            Class<?> clazz = Class.forName(className);
            if (!SubscriberService.class.isAssignableFrom(clazz)) {
                throw new InvalidSubscriberClassException(String.format("Class '%s' is not a valid implementation of SubscriberService.", className));
            }

            SubscriberService subscriber = (SubscriberService) clazz
                    .getDeclaredConstructor(CoordinatorService.class, ApplicationConfig.class)
                    .newInstance(this, this.appConfig);

            subscribers.put(platformName, subscriber);

        } catch (ClassNotFoundException e) {
            throw new ClassLoadingException(String.format("Class '%s' not found in the classpath.", className));
        } catch (Exception e) {
            throw new ClassLoadingException(String.format("Unexpected error while loading subscriber class '%s': %s", className, e.getMessage()), e);
        }
    }

}