package com.toyota.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.config.ApplicationConfig;
import com.toyota.config.SubscriberConfig;
import com.toyota.service.CoordinatorService;
import com.toyota.entity.Rate;
import com.toyota.exception.*;
import com.toyota.service.MailSender;
import com.toyota.service.RateManager;
import com.toyota.service.SubscriberService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class CoordinatorImpl implements CoordinatorService {

    private static final Logger log = LogManager.getLogger(CoordinatorImpl.class);
    private static final int THREAD_POOL_SIZE = 10;

    private final String subscribersConfigFile;

    private final int connectionRetryLimit;
    private final int retryDelaySeconds;
    private final Map<String, Integer> retryCounts;

    private final RateManager rateManager;
    private final MailSender mailSender;
    private final ExecutorService executorService;
    private final Map<String, SubscriberService> subscribers;

    public CoordinatorImpl(RateManager rateManager, MailSender mailSender, ApplicationConfig applicationConfig) {
        log.info("Coordinator: Initializing Coordinator...");
        this.mailSender = mailSender;
        this.rateManager = rateManager;

        this.subscribersConfigFile = applicationConfig.getValue("subscribers.config.file");
        this.connectionRetryLimit = applicationConfig.getIntValue("connection.retry.limit");
        this.retryDelaySeconds = applicationConfig.getIntValue("retry.delay.seconds");

        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        this.subscribers = new ConcurrentHashMap<>();
        this.retryCounts = new ConcurrentHashMap<>();


        // create the context for per thread.
        log.info("Coordinator: Warming up calculation service with {} threads...", THREAD_POOL_SIZE);
        for (int i = 1; i <= THREAD_POOL_SIZE; i++) {
            executorService.execute(rateManager::warmUpCalculationService);
        }

        loadSubscribers();
        startSubscribers();
        log.info("Coordinator: Simulation begins. Active subscriber count :{}.", subscribers.size());
    }


    @Override
    public void onConnect(String platformName, Boolean status) {
        executorService.execute(() -> {
            if (status) {
                log.info("Coordinator: Platform '{}' connection status: {}", platformName, "CONNECTED");

                SubscriberService subscriber = subscribers.get(platformName);
                SubscriberConfig subscriberConfig = subscriber.getConfig();

                subscriberConfig.getExchangeRates()
                        .forEach(rateName -> {
                            executorService.execute(() -> subscriber.subscribe(platformName, rateName));
                        });

            } else {
                log.error("Coordinator: Platform '{}' connection status: {}", platformName, "FAILED");
                retryToConnectWithDelay(platformName);
            }
        });
    }


    @Override
    public void onDisConnect(String platformName) {
        executorService.execute(() -> {
            log.error("Coordinator: Platform '{}' disconnected. Initiating reconnection process.", platformName);
            retryToConnectWithDelay(platformName);
        });
    }


    @Override
    public void onRateAvailable(String platformName, String rateName, Rate rate) {
        executorService.execute(() -> {
            log.info("Coordinator: Rate {} is available for platform '{}'. Forwarding to RateManager.", rateName, platformName);
            rateManager.handleFirstInComingRate(platformName, rateName, rate);
        });
    }


    @Override
    public void onRateUpdate(String platformName, String rateName, Rate rate) {
        executorService.execute(() -> {
            log.info("Coordinator: Rate update for platform '{}', rate '{}'. Forwarding to RateManager.", platformName, rateName);
            rateManager.handleRateUpdate(platformName, rateName, rate);
        });
    }


    private void retryToConnectWithDelay(String platformName) {
        int retryCount = retryCounts.getOrDefault(platformName, 0);

        if (retryCount >= connectionRetryLimit) {
            log.error("Coordinator: Retry limit {} reached for platform '{}'. Sending notification email, but will continue retrying...",
                    connectionRetryLimit, platformName);

            mailSender.sendConnectionFailureNotification(platformName, connectionRetryLimit, retryDelaySeconds);

            retryCounts.put(platformName, 0);
        } else {
            retryCounts.put(platformName, retryCount + 1);
        }

        try {
            log.warn("Coordinator: Retrying connection to '{}' in {} seconds (Attempt {}/{})...",
                    platformName, retryDelaySeconds, retryCount + 1, connectionRetryLimit);

            Thread.sleep(TimeUnit.SECONDS.toMillis(retryDelaySeconds));

            SubscriberService subscriber = subscribers.get(platformName);
            if (subscriber != null) {
                subscriber.connect(platformName);
            }
        } catch (InterruptedException e) {
            log.error("Coordinator: Reconnection attempt for '{}' was interrupted.", platformName, e);
            Thread.currentThread().interrupt();
        }
    }


    private void startSubscribers() {
        log.info("Coordinator: Starting all loaded subscribers...");
        subscribers.forEach((platformName, subscriber) -> {
            executorService.execute(() -> subscriber.connect(platformName));
        });
    }


    private void loadSubscribers() {
        log.debug("Coordinator: Loading subscriber configurations from '{}'...", subscribersConfigFile);
        try (InputStream jsonFile = CoordinatorImpl.class.getClassLoader().getResourceAsStream(subscribersConfigFile)) {
            if (jsonFile == null) {
                log.error("Coordinator: Subscriber configuration file '{}' not found in classpath.", subscribersConfigFile);
                throw new ConfigFileNotFoundException(String.format("Configuration file '%s' not found in the classpath.", subscribersConfigFile));
            }

            ObjectMapper mapper = new ObjectMapper();
            List<SubscriberConfig> subscriberConfigs = mapper
                    .readValue(jsonFile,new TypeReference<List<SubscriberConfig>>() {});

            for (SubscriberConfig config : subscriberConfigs) {
                loadSubscriber(config);
            }

            log.info("Coordinator: Subscribers loaded successfully from configuration file: '{}'.", subscribersConfigFile);
        } catch (IOException e) {
            log.error("Coordinator: Failed to read subscriber configuration file '{}'", subscribersConfigFile, e);
            throw new ConfigFileLoadingException(String.format("Failed to read configuration file '%s': %s", subscribersConfigFile, e.getMessage()));
        }
    }

    private void loadSubscriber(SubscriberConfig config) {
        String platformName = config.getPlatformName();
        String className = config.getClassName();

        log.debug("Coordinator: loading subscriber for platform '{}'", platformName);

        if (platformName == null || className == null) {
            log.error("Coordinator: Invalid subscriber entry in config file '{}': 'platformName' ({}) or 'className' ({}) is missing.", subscribersConfigFile, platformName, className);
            throw new InvalidConfigFileException(String.format("Invalid subscriber entry in config file '%s': 'platformName' or 'className' is missing.", subscribersConfigFile));
        }


        try {
            Class<?> clazz = Class.forName(className);
            if (!SubscriberService.class.isAssignableFrom(clazz)) {
                log.error("Coordinator: Class '{}' for platform '{}' does not implement SubscriberService.", className, platformName);
                throw new InvalidSubscriberClassException(String.format("Class '%s' is not a valid implementation of SubscriberService.", className));
            }

            SubscriberService subscriber = (SubscriberService) clazz
                    .getDeclaredConstructor(CoordinatorService.class, SubscriberConfig.class)
                    .newInstance(this, config);

            subscribers.put(platformName, subscriber);
            log.debug("Coordinator: Successfully loaded and instantiated subscriber for platform '{}'.", platformName);
        } catch (ClassNotFoundException e) {
            log.error("Coordinator: Subscriber class '{}' for platform '{}' not found in classpath.", className, platformName, e);
            throw new ClassLoadingException(String.format("Class '%s' not found in the classpath.", className));
        } catch (Exception e) {
            log.error("Coordinator: Failed to instantiate subscriber class '{}' for platform '{}'.", className, platformName, e);
            throw new ClassLoadingException(String.format("Unexpected error while loading subscriber class '%s': %s", className, e.getMessage()), e);
        }
    }


}