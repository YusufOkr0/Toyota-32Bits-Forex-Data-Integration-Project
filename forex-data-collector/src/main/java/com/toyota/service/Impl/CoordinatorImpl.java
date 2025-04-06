package com.toyota.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.config.ConfigUtil;
import com.toyota.service.CoordinatorService;
import com.toyota.entity.Rate;
import com.toyota.entity.RateStatus;
import com.toyota.exception.*;
import com.toyota.service.RedisService;
import com.toyota.service.SubscriberService;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class CoordinatorImpl implements CoordinatorService {

    private final String SUBSCRIBERS_CONFIG_FILE;
    private final int CONNECTION_RETRY_LIMIT;

    private final List<String> exchangeRates;
    private final Map<String, Integer> retryCounts;
    private final Map<String, SubscriberService> subscribers;
    private final RedisService redisService;


    public CoordinatorImpl() {
        this.SUBSCRIBERS_CONFIG_FILE = ConfigUtil.getValue("subscribers.config.file");
        this.CONNECTION_RETRY_LIMIT = ConfigUtil.getIntValue("connection.retry.limit");

        this.exchangeRates = ConfigUtil.getExchangeRates();

        this.subscribers = new ConcurrentHashMap<>();
        this.retryCounts = new ConcurrentHashMap<>();
        this.redisService = new RedisServiceImpl();

        loadSubscribers();
        startSubscribers();
    }


    @Override
    public void onConnect(String platformName, Boolean status) {
        System.out.printf("Platform: %s connection status is: %s\n", platformName, status);
        System.out.println(Thread.currentThread().getName());
        if (status) {
            retryCounts.put(platformName, 0);
            SubscriberService subscriber = subscribers.get(platformName);
            exchangeRates.forEach(rate -> {
                subscriber.subscribe(platformName, rate);
            });
        } else {
            retryToConnectWithDelay(platformName);
        }
    }

    @Override
    public void onDisConnect(String platformName, Boolean status) {
        if (status) {
            retryToConnectWithDelay(platformName);
        } else {
            System.out.printf("Connection status is false for platform: %s%n", platformName);
        }
    }

    @Override
    public void onRateAvailable(String platformName, String rateName, Rate rate) {
        System.out.println(rate.toString());
    }

    @Override
    public void onRateUpdate(String platformName, String rateName, Rate rate) {
        redisService.saveRawRate(platformName,rateName,rate);
    }

    @Override
    public void onRateStatus(String platformName, String rateName, RateStatus rateStatus) {
    }

    private void retryToConnectWithDelay(String platformName) {
        System.out.println("retry to connect ...");

        int retryCount = retryCounts.getOrDefault(platformName, 0);
        if (retryCount >= CONNECTION_RETRY_LIMIT) {
            System.err.printf("Maximum retry limit (%d) reached for platform '%s'. Connection attempts abandoned.%n", CONNECTION_RETRY_LIMIT, platformName);

            return;
        }

        retryCounts.put(platformName, ++retryCount);

        try {
            Thread.sleep(10000);
            switch (platformName) {
                case "REST":
                    subscribers.get(platformName)
                            .connect(platformName);
                    break;
                case "TCP":
                    subscribers.get(platformName)
                            .connect(platformName);
                    break;
            }
        } catch (InterruptedException e) {
            System.err.printf("Failed to reconnect to platform '%s' after delay due to interruption: %s%n", platformName, e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private void loadSubscribers() {
        try (InputStream jsonFile = CoordinatorImpl.class.getClassLoader().getResourceAsStream(SUBSCRIBERS_CONFIG_FILE)) {
            if (jsonFile == null) {
                throw new ConfigFileNotFoundException(String.format("Configuration file '%s' not found in the classpath.", SUBSCRIBERS_CONFIG_FILE));
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonFile);
            JsonNode subscribersNode = rootNode.get("subscribers");

            if (subscribersNode == null || !subscribersNode.isArray()) {
                throw new InvalidConfigFileException(String.format("Invalid configuration file '%s': 'subscribers' field must be a non-null JSON array.", SUBSCRIBERS_CONFIG_FILE));
            }

            subscribersNode.forEach(this::loadSubscriber);

        } catch (IOException e) {
            throw new ConfigFileLoadingException(String.format("Failed to read configuration file '%s': %s", SUBSCRIBERS_CONFIG_FILE, e.getMessage()));
        }
    }

    private void loadSubscriber(JsonNode subscriberNode) {
        String platformName = subscriberNode.path("platformName").asText(null);
        String className = subscriberNode.path("className").asText(null);

        if (platformName == null || className == null) {
            throw new InvalidConfigFileException(String.format("Invalid subscriber entry in config file '%s': 'platformName' or 'className' is missing.", SUBSCRIBERS_CONFIG_FILE));
        }

        try {
            Class<?> clazz = Class.forName(className);
            if (!SubscriberService.class.isAssignableFrom(clazz)) {
                throw new InvalidSubscriberClassException(String.format("Class '%s' is not a valid implementation of SubscriberService.", className));
            }

            SubscriberService subscriber = (SubscriberService) clazz
                    .getDeclaredConstructor(CoordinatorService.class)
                    .newInstance(this);

            subscribers.put(platformName, subscriber);

        } catch (ClassNotFoundException e) {
            throw new ClassLoadingException(String.format("Class '%s' not found in the classpath.", className));
        } catch (Exception e) {
            throw new ClassLoadingException(String.format("Unexpected error while loading subscriber class '%s': %s", className, e.getMessage()), e);
        }
    }

    private void startSubscribers() {
        Thread tcpThread = new Thread(() -> {
            subscribers.get("TCP").connect("TCP");
        }, "TcpPlatform-Thread");

        Thread restThread = new Thread(() -> {
            subscribers.get("REST").connect("REST");
        }, "RestPlatform-Thread");

        restThread.start();
        tcpThread.start();
    }
}