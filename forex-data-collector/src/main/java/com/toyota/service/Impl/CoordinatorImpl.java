package com.toyota.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.config.ApplicationConfig;
import com.toyota.config.SubscriberConfig;
import com.toyota.entity.Rate;
import com.toyota.exception.*;
import com.toyota.service.CoordinatorService;
import com.toyota.service.MailSender;
import com.toyota.service.RateManager;
import com.toyota.service.SubscriberService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;


public class CoordinatorImpl implements CoordinatorService {

    private static final Logger log = LogManager.getLogger(CoordinatorImpl.class);

    private final String subscriberConfigPath;

    private final Map<String, SubscriberService> subscribers;
    private final Map<String, Integer> retryCounts;
    private final RateManager rateManager;
    private final MailSender mailSender;
    private final ThreadPoolExecutor executorService;

    public CoordinatorImpl(RateManager rateManager, MailSender mailSender, ApplicationConfig applicationConfig) {
        log.info("Coordinator: Initializing Coordinator...");
        this.mailSender = mailSender;
        this.rateManager = rateManager;

        this.subscriberConfigPath = applicationConfig.getValue("subscribers.config.path");
        this.executorService = new ThreadPoolExecutor(6,10,1,TimeUnit.MINUTES,new LinkedBlockingDeque<>(40));
        this.subscribers = new ConcurrentHashMap<>();
        this.retryCounts = new ConcurrentHashMap<>();


        loadSubscribers();
        startSubscribers();
        log.info("Coordinator: Simulation begins. Active subscriber count :{}.", subscribers.size());
    }


    @Override
    public void onConnect(String platformName, Boolean status) {
        executorService.execute(() -> {
            if (status) {
                log.info("onConnect: Platform '{}' connection status: {}", platformName, "CONNECTED");

                SubscriberService subscriber = subscribers.get(platformName);
                SubscriberConfig subscriberConfig = subscriber.getConfig();

                subscriberConfig.getExchangeRates()
                        .forEach(rateName -> {
                            subscriber.subscribe(platformName, rateName);
                        });

            } else {
                log.error("onConnect: Platform '{}' connection status: {}", platformName, "FAILED");
                retryToConnectWithDelay(platformName);
            }
        });
    }


    @Override
    public void onDisConnect(String platformName) {
        executorService.execute(() -> {
            log.error("onDisConnect: Platform '{}' disconnected. Initiating reconnection process.", platformName);
            retryToConnectWithDelay(platformName);
        });
    }


    @Override
    public void onRateAvailable(String platformName, String rateName, Rate rate) {
        executorService.execute(() -> {
            log.info("onRateAvailable: Rate {} is available for platform '{}'. Forwarding to RateManager.", rateName, platformName);
            rateManager.handleFirstInComingRate(platformName, rateName, rate);
        });
    }


    @Override
    public void onRateUpdate(String platformName, String rateName, Rate rate) {
        executorService.execute(() -> {
            log.info("onRateUpdate: Rate update for platform '{}', rate '{}'. Forwarding to RateManager.", platformName, rateName);
            rateManager.handleRateUpdate(platformName, rateName, rate);
        });
    }

    @Override
    public void onUnsubscribe(String platformName, String rateName) {
        executorService.execute(() -> {
            long delayTimeInSeconds = 5;
            log.error("onUnsubscribe: Platform '{}' unsubscribe from rate '{}'. Retrying subscription to rate after {} seconds ... ", platformName, rateName, delayTimeInSeconds);

            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(delayTimeInSeconds));

                SubscriberService subscriber = subscribers.get(platformName);
                if (subscriber != null) {
                    subscriber.subscribe(platformName, rateName);
                }
            } catch (InterruptedException e) {
                log.error("onUnsubscribe: Retrying subscription attempt for rate '{}' on platform {} was interrupted.", rateName, platformName, e);
                Thread.currentThread().interrupt();
            }
        });
    }


    private void retryToConnectWithDelay(String platformName) {
        SubscriberConfig subscriberConfig = subscribers.get(platformName).getConfig();

        int connectionRetryCounts = subscriberConfig.getConnectionRetryCounts();
        int retryDelaySeconds = subscriberConfig.getRetryDelaySeconds();

        int retryCount = retryCounts.getOrDefault(platformName, 0);

        if (retryCount >= connectionRetryCounts) {
            log.error("retryToConnectWithDelay: Retry limit {} reached for platform '{}'. Sending notification email, but will continue retrying...",
                    connectionRetryCounts, platformName);

            mailSender.sendConnectionFailureNotification(platformName, connectionRetryCounts, retryDelaySeconds);

            retryCounts.put(platformName, 0);
        } else {
            retryCounts.put(platformName, retryCount + 1);
        }

        try {
            log.warn("retryToConnectWithDelay: Retrying connection to '{}' in {} seconds (Attempt {}/{})...",
                    platformName, retryDelaySeconds, retryCount + 1, connectionRetryCounts);

            Thread.sleep(TimeUnit.SECONDS.toMillis(retryDelaySeconds));

            SubscriberService subscriber = subscribers.get(platformName);
            if (subscriber != null) {
                subscriber.connect(platformName);
            }
        } catch (InterruptedException e) {
            log.error("retryToConnectWithDelay: Reconnection attempt for '{}' was interrupted.", platformName, e);
            Thread.currentThread().interrupt();
        }
    }


    private void startSubscribers() {
        log.info("startSubscribers: Starting all loaded subscribers...");
        subscribers.forEach((platformName, subscriber) -> {
            executorService.execute(() -> subscriber.connect(platformName));
        });
    }


    private void loadSubscribers() {
        try (InputStream jsonFile = getSubscribersJsonFile(subscriberConfigPath)) {
            if (jsonFile == null) {
                log.error("loadSubscribers: Subscriber configuration file '{}' not found.", subscriberConfigPath);
                throw new ConfigFileNotFoundException(String.format("Configuration file '%s' not found.", subscriberConfigPath));
            }

            ObjectMapper mapper = new ObjectMapper();
            List<SubscriberConfig> subscriberConfigs = mapper.readValue(jsonFile, new TypeReference<List<SubscriberConfig>>() {
            });

            for (SubscriberConfig config : subscriberConfigs) {
                loadSubscriber(config);
            }

            log.info("loadSubscribers: Subscribers loaded successfully from '{}'.", subscriberConfigPath);
        } catch (IOException e) {
            log.error("loadSubscribers: Failed to read subscriber configuration file '{}'", subscriberConfigPath, e);
            throw new ConfigFileLoadingException(String.format("Failed to read configuration file '%s': %s", subscriberConfigPath, e.getMessage()));
        }
    }

    private void loadSubscriber(SubscriberConfig config) {
        String platformName = config.getPlatformName();
        String className = config.getClassName();

        log.debug("loadSubscriber: loading subscriber for platform '{}'", platformName);

        if (platformName == null || className == null) {
            log.error("loadSubscriber: Invalid subscriber entry in config file '{}': 'platformName' ({}) or 'className' ({}) is missing.", subscriberConfigPath, platformName, className);
            throw new InvalidConfigFileException(String.format("Invalid subscriber entry in config file '%s': 'platformName' or 'className' is missing.", subscriberConfigPath));
        }


        try {
            Class<?> clazz = Class.forName(className);
            if (!SubscriberService.class.isAssignableFrom(clazz)) {
                log.error("loadSubscriber: Class '{}' for platform '{}' does not implement SubscriberService.", className, platformName);
                throw new InvalidSubscriberClassException(String.format("Class '%s' is not a valid implementation of SubscriberService.", className));
            }

            SubscriberService subscriber = (SubscriberService) clazz
                    .getDeclaredConstructor(CoordinatorService.class, SubscriberConfig.class)
                    .newInstance(this, config);

            subscribers.put(platformName, subscriber);
            log.debug("loadSubscriber: Successfully loaded and instantiated subscriber for platform '{}'.", platformName);
        } catch (ClassNotFoundException e) {
            log.error("loadSubscriber: Subscriber class '{}' for platform '{}' not found in classpath.", className, platformName, e);
            throw new ClassLoadingException(String.format("Class '%s' not found in the classpath.", className));
        } catch (Exception e) {
            log.error("loadSubscriber: Failed to instantiate subscriber class '{}' for platform '{}'.", className, platformName, e);
            throw new ClassLoadingException(String.format("Unexpected error while loading subscriber class '%s': %s", className, e.getMessage()), e);
        }
    }


    private InputStream getSubscribersJsonFile(String path) throws IOException {
        File file = new File(path);
        if (file.exists() && file.isFile()) {
            log.info("Coordinator: Loading subscribers from external file: {}", path);
            return new FileInputStream(file);
        }

        log.info("Coordinator: Loading subscribers from default file in classpath: {}", path);
        return CoordinatorImpl.class.getClassLoader().getResourceAsStream(path);
    }


}