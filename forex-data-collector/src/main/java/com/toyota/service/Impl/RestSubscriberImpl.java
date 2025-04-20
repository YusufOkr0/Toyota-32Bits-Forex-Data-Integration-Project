package com.toyota.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.toyota.config.ApplicationConfig;
import com.toyota.dtos.response.ApiKeyResponse;
import com.toyota.entity.Rate;
import com.toyota.service.CoordinatorService;
import com.toyota.service.SubscriberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class RestSubscriberImpl implements SubscriberService {

    private static final Logger log = LoggerFactory.getLogger(RestSubscriberImpl.class);

    private final long subscriptionDelayMs;

    private String apiKey;

    private final String username;
    private final String password;
    private final String baseUrl;
    private final HttpClient httpClient;

    private final Set<String> receivedRates;
    private final ObjectMapper objectMapper;

    private final Map<String, ScheduledFuture<?>> activeSubscriptions;
    private final ScheduledExecutorService scheduler;
    private final CoordinatorService coordinator;


    public RestSubscriberImpl(CoordinatorService coordinator,ApplicationConfig applicationConfig) {
        this.coordinator = coordinator;

        this.username = applicationConfig.getValue("rest.platform.username");
        this.password = applicationConfig.getValue("rest.platform.password");
        this.baseUrl = applicationConfig.getValue("rest.platform.base.url");
        this.subscriptionDelayMs = applicationConfig.getIntValue("subscription.delay.ms");

        this.objectMapper = configureObjectMapper();
        this.httpClient = configureHttpClient();

        this.scheduler = Executors.newScheduledThreadPool(1);
        this.activeSubscriptions = new ConcurrentHashMap<>();
        this.receivedRates = new HashSet<>();
    }

    @Override
    public void connect(String platformName) {
        log.debug("Rest Subscriber::Attempting to connect to platform: {}", platformName);
        try {
            HttpRequest authRequest = buildAuthRequest();

            HttpResponse<String> authResponse = httpClient
                    .send(authRequest, HttpResponse.BodyHandlers.ofString());

            if (authResponse.statusCode() == 200) {
                this.apiKey = objectMapper.readValue(
                        authResponse.body(),
                        ApiKeyResponse.class
                ).getApiKey();
                log.info("Rest Subscriber::Successfully connected to platform: {}. API Key received.", platformName);
                coordinator.onConnect(platformName, true);
            } else {
                log.warn("Authentication failed for platform: {}. HTTP status: {}", platformName, authResponse.statusCode());
                coordinator.onConnect(platformName, false);
            }

        } catch (IOException | InterruptedException e) {
            log.warn("Failed to connect to platform: {}.", platformName, e);
            coordinator.onConnect(platformName, false);
        }
    }

    @Override
    public void disConnect() {
        activeSubscriptions.values()
                .forEach(scheduledJob -> scheduledJob.cancel(false));
        activeSubscriptions.clear();
        this.receivedRates.clear();
        this.apiKey = null;
        log.info("Rest Subscriber::Disconnected successfully. All subscriptions cleared.");
    }

    @Override
    public void subscribe(String platformName, String rateName) {
        if (apiKey == null) {
            log.warn("Rest Subscriber::Cannot subscribe to rate: {}. API key is null.", rateName);
            return;
        }
        if (activeSubscriptions.containsKey(rateName)) {
            log.warn("Rest Subscriber::Subscription to rate: {} already exists.", rateName);
            return;
        }

        String rateUrl = String.format("%s/api/rates/%s_%s", baseUrl, platformName, rateName); //localhost:8092/api/rates/REST_USDTRY

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(rateUrl))
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .build();


        Runnable subscribeJob = createSubscribeJob(
                platformName,
                rateName,
                request
        );

        ScheduledFuture<?> scheduledJob = scheduler.scheduleWithFixedDelay(
                subscribeJob,
                1,
                subscriptionDelayMs,
                TimeUnit.MILLISECONDS
        );
        activeSubscriptions.put(rateName, scheduledJob);
        log.info("Rest Subscriber::Subscribed to rate: {} on platform: {}", rateName, platformName);
    }

    @Override
    public void unSubscribe(String platformName, String rateName) {
        ScheduledFuture<?> scheduledJob = activeSubscriptions.remove(rateName);
        if (scheduledJob != null) {
            scheduledJob.cancel(false);
            receivedRates.remove(rateName);
            log.info("Rest Subscriber::Unsubscribed from rate: {} on platform: {}", rateName, platformName);
        }else {
            log.warn("Rest Subscriber::No subscription found for rate: {} on platform: {}", rateName, platformName);
        }
    }


    private Runnable createSubscribeJob(String platformName, String rateName, HttpRequest request) {
        return () -> {
            try {
                log.trace("Rest Subscriber::Fetching rate: {} for platform: {}", rateName, platformName);
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    Rate rate = objectMapper.readValue(
                            response.body(),
                            Rate.class
                    );

                    if (receivedRates.contains(rateName)) {
                        coordinator.onRateUpdate(platformName, rateName, rate);
                    } else {
                        receivedRates.add(rateName);
                        coordinator.onRateAvailable(platformName, rateName, rate);
                    }
                } else {
                    log.warn("Rest Subscriber::Failed to fetch rate: {} from platform: {}. HTTP status: {}", rateName, platformName, response.statusCode());
                    handleConnectionFailure(platformName);
                }
            } catch (Exception e){
                log.error("Rest Subscriber::Error fetching rate: {} from platform: {}. Error: {}", rateName, platformName, e.getMessage(), e);
                handleConnectionFailure(platformName);
                if(e instanceof InterruptedException){
                    Thread.currentThread().interrupt();
                    log.error("Rest Subscriber::Thread is interrupted.");
                }
            }
        };
    }

    private void handleConnectionFailure(String platformName) {
        log.warn("Handling connection failure for platform: {}. Cancelling all subscriptions.", platformName);
        activeSubscriptions.values()
                .forEach(scheduledJob -> scheduledJob.cancel(false));
        activeSubscriptions.clear();
        receivedRates.clear();
        coordinator.onDisConnect(platformName);
    }


    private HttpRequest buildAuthRequest() {
        String requestBody = """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password);
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    private ObjectMapper configureObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    private HttpClient configureHttpClient() {
        return HttpClient.newBuilder()
                .build();
    }


}