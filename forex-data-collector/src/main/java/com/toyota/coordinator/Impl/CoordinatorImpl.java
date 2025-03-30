package com.toyota.coordinator.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.config.ConfigUtil;
import com.toyota.coordinator.CoordinatorService;
import com.toyota.entity.Rate;
import com.toyota.entity.RateStatus;
import com.toyota.exception.*;
import com.toyota.subscriber.SubscriberService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CoordinatorImpl implements CoordinatorService {
    private final String CONFIG_FILE;

    private final String TCP_USERNAME;
    private final String TCP_PASSWORD;

    private final String REST_USERNAME;
    private final String REST_PASSWORD;

    private static final int MAX_RETRY_COUNT = 12;

    private final Map<String, SubscriberService> subscribers;
    private final Map<String, Integer> retryCounts;

    public CoordinatorImpl() {
        this.CONFIG_FILE = ConfigUtil.getValue("subscribers.config.file");
        this.TCP_USERNAME = ConfigUtil.getValue("tcp.platform.username");
        this.TCP_PASSWORD = ConfigUtil.getValue("tcp.platform.password");
        this.REST_USERNAME = ConfigUtil.getValue("rest.platform.username");
        this.REST_PASSWORD = ConfigUtil.getValue("rest.platform.password");

        this.subscribers = new ConcurrentHashMap<>();
        this.retryCounts = new ConcurrentHashMap<>();

        loadSubscribers();
        startSubscribers();
    }


    @Override
    public void onConnect(String platformName, Boolean status) {
        System.out.printf("Platform: %s connection status is: %s\n", platformName, status);
        System.out.println(Thread.currentThread().getName());
        if (status) {
            retryCounts.put(platformName, 0);

            subscribers.get(platformName)
                    .subscribe(platformName, "USDTRY");
            subscribers.get(platformName)
                    .subscribe(platformName, "EURUSD");
            subscribers.get(platformName)
                    .subscribe(platformName, "GBPUSD");
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
        System.out.println(rate.toString());
    }

    @Override
    public void onRateStatus(String platformName, String rateName, RateStatus rateStatus) {
    }

    private void retryToConnectWithDelay(String platformName) {
        System.out.println("retry to connect ...");

        int retryCount = retryCounts.getOrDefault(platformName, 0);
        if (retryCount >= MAX_RETRY_COUNT) {
            System.err.printf("Max retries (%d) reached for %s. Giving up.\n", MAX_RETRY_COUNT, platformName);
            return;
        }

        retryCounts.put(platformName, ++retryCount);

        try {
            Thread.sleep(10000);
            switch (platformName) {
                case "REST":
                    subscribers.get(platformName)
                            .connect(platformName, REST_USERNAME, REST_PASSWORD);
                    break;
                case "TCP":
                    subscribers.get(platformName)
                            .connect(platformName, TCP_USERNAME, TCP_PASSWORD);
                    break;
            }
        } catch (InterruptedException e) {
            System.err.println("Exception when trying to connect with delay: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private void loadSubscribers() {
        try (InputStream jsonFile = CoordinatorImpl.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (jsonFile == null) {
                throw new ConfigFileNotFoundException("Configuration file not found in the classpath: " + CONFIG_FILE);
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonFile);
            JsonNode subscribersNode = rootNode.get("subscribers");

            if (subscribersNode == null || !subscribersNode.isArray()) {
                throw new InvalidConfigFileException("'Subscribers' must be a non-null json file.");
            }

            subscribersNode.forEach(this::loadSubscriber);

        } catch (IOException e) {
            throw new SubscriberLoadingException("Error reading configuration file: " + CONFIG_FILE, e);
        }
    }

    private void loadSubscriber(JsonNode subscriberNode) {
        String platformName = subscriberNode.path("platformName").asText(null);
        String className = subscriberNode.path("className").asText(null);

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
            throw new ClassLoadingException(String.format("Class: %s not found.",className));
        } catch (Exception e) {
            throw new ClassLoadingException(String.format("Unexpected exception while loading subscriber class: %s.", className), e);
        }
    }

    private void startSubscribers() {
        Thread tcpThread = new Thread(() -> {
            subscribers.get("TCP").connect("TCP", TCP_USERNAME, TCP_PASSWORD);
        }, "TcpPlatform-Thread");

        Thread restThread = new Thread(() -> {
            subscribers.get("REST").connect("REST", REST_USERNAME, REST_PASSWORD);
        }, "RestPlatform-Thread");

        restThread.start();
        tcpThread.start();
    }
}