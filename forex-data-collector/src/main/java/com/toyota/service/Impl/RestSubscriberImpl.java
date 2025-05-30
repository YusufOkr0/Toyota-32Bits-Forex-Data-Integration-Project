package com.toyota.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.toyota.config.SubscriberConfig;
import com.toyota.dtos.response.ApiKeyResponse;
import com.toyota.entity.Rate;
import com.toyota.service.CoordinatorService;
import com.toyota.service.SubscriberService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class RestSubscriberImpl implements SubscriberService {

    private static final Logger log = LogManager.getLogger(RestSubscriberImpl.class);

    private final Object disconnectLock;

    private final Integer subscriptionDelayMs;

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
    private final SubscriberConfig subscriberConfig;


    public RestSubscriberImpl(CoordinatorService coordinator, SubscriberConfig subscriberConfig) {
        this.subscriberConfig = subscriberConfig;
        this.coordinator = coordinator;
        this.disconnectLock = new Object();

        this.username = subscriberConfig.getProperty("username", String.class);
        this.password = subscriberConfig.getProperty("password", String.class);
        this.baseUrl = subscriberConfig.getProperty("baseUrl", String.class);
        this.subscriptionDelayMs = subscriberConfig.getProperty("subscriptionDelayMs", Integer.class);

        this.objectMapper = configureObjectMapper();
        this.httpClient = configureHttpClient();

        this.scheduler = Executors.newScheduledThreadPool(1);
        this.activeSubscriptions = new ConcurrentHashMap<>();
        this.receivedRates = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void connect(String platformName) {
        log.info("connect: Attempting to connect to platform: {}", platformName);
        try {
            HttpRequest authRequest = buildAuthRequest();

            HttpResponse<String> authResponse = httpClient
                    .send(authRequest, HttpResponse.BodyHandlers.ofString());

            if (authResponse.statusCode() == 200) {
                this.apiKey = objectMapper.readValue(
                        authResponse.body(),
                        ApiKeyResponse.class
                ).getApiKey();
                log.info("connect: Successfully connected to platform: {}. API Key received.", platformName);
                coordinator.onConnect(platformName, true);
            } else {
                log.error("connect: Authentication failed for platform: {}. HTTP status: {}", platformName, authResponse.statusCode());
                coordinator.onConnect(platformName, false);
            }

        } catch (IOException e) {
            log.error("connect: Failed to connect to platform: {}.", platformName,e);
            coordinator.onConnect(platformName, false);
        } catch (InterruptedException e) {
            log.error("connect: Connect process interrupted for platform: {}.", platformName,e);
            Thread.currentThread().interrupt();
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
        log.info("disConnect: Disconnected successfully. All subscriptions cleared.");
    }

    @Override
    public void subscribe(String platformName, String rateName) {
        log.info("subscribe: Subscribing to rate: {} on platform: {}", rateName, platformName);

        if (apiKey == null) {
            log.warn("subscribe: Cannot subscribe to rate: {}. API key is null.", rateName);
            return;
        }
        if (activeSubscriptions.containsKey(rateName)) {
            log.warn("subscribe: Subscription to rate: {} already exists.", rateName);
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
                3,
                subscriptionDelayMs,
                TimeUnit.MILLISECONDS
        );
        activeSubscriptions.put(rateName, scheduledJob);
    }

    @Override
    public void unSubscribe(String platformName, String rateName) {
        ScheduledFuture<?> scheduledJob = activeSubscriptions.remove(rateName);
        if (scheduledJob != null) {
            scheduledJob.cancel(false);
            receivedRates.remove(rateName);
            log.info("unSubscribe: Unsubscribed from rate: {} on platform: {}", rateName, platformName);
        } else {
            log.warn("unSubscribe: No subscription found for rate: {} on platform: {}", rateName, platformName);
        }
    }

    @Override
    public SubscriberConfig getConfig() {
        return this.subscriberConfig;
    }


    private Runnable createSubscribeJob(String platformName, String rateName, HttpRequest request) {
        return () -> {
            long startTime = System.currentTimeMillis();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .orTimeout(5, TimeUnit.SECONDS)
                    .thenAccept(response -> {
                        long endTime = System.currentTimeMillis();
                        long responseTimeMs = endTime - startTime;
                        ThreadContext.put("responseTimeMs", String.valueOf(responseTimeMs));

                        int statusCode = response.statusCode();
                        if (statusCode == 200) {
                            try {
                                log.info("createSubscribeJob: Rate {} on platform {} fetched [responseTimeMs={}]", rateName, platformName, responseTimeMs);
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
                            } catch (IOException e) {
                                log.error("createSubscribeJob: Failed to parse rate response for {}: {}", rateName, e.getMessage(),e);
                                handleConnectionFailure(platformName);
                            }
                        } else if (statusCode == 400) {     // If given rates doesnt exists in the platform then write log and stop subscription.
                            log.error("createSubscribeJob: Given rate does not exists in rest-data-provider.");
                            unSubscribe(platformName, rateName);
                        } else {
                            log.error("createSubscribeJob: Failed to fetch rate: {} from platform: {}. HTTP status: {}", rateName, platformName, statusCode);
                            handleConnectionFailure(platformName);
                        }
                    })
                    .exceptionally(ex -> {
                        long endTime = System.currentTimeMillis();
                        long responseTimeMs = endTime - startTime;
                        ThreadContext.put("responseTimeMs", String.valueOf(responseTimeMs));

                        if (ex instanceof TimeoutException || (ex.getCause() instanceof TimeoutException)) {
                            log.error("createSubscribeJob: Timeout occurred for rate: {} on platform: {}. Cancelling subscription...", rateName, platformName,ex);
                            handleTimeoutException(platformName, rateName);
                        } else {
                            log.error("createSubscribeJob: Exception while fetching rate: {} on platform: {}. Cancelling all subscriptions.: {}", rateName, platformName, ex.getMessage(),ex);
                            handleConnectionFailure(platformName);
                        }
                        ThreadContext.clearMap();
                        return null;
                    });
        };
    }


    // NOTIFY COORDINATOR ON TIMEOUT EXCEPTION IN ORDER TO RE-SUBSCRIBE TO SPECIFIC RATE WITH DELAY
    private void handleTimeoutException(String platformName, String rateName) {
        ScheduledFuture<?> job = activeSubscriptions.remove(rateName);
        if (job != null) {
            job.cancel(true);
            coordinator.onUnsubscribe(platformName, rateName);
        }
    }


    private void handleConnectionFailure(String platformName) {
        synchronized (disconnectLock) {
            if (!activeSubscriptions.isEmpty()) {
                activeSubscriptions.forEach((rateName, job) -> job.cancel(false));
                activeSubscriptions.clear();
                coordinator.onDisConnect(platformName);
            }
        }
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
                .connectTimeout(Duration.ofSeconds(5L))
                .build();
    }


}