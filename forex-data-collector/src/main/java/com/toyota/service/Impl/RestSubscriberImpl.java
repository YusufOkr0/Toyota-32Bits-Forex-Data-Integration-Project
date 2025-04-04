package com.toyota.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.toyota.config.ConfigUtil;
import com.toyota.dtos.response.ApiKeyResponse;
import com.toyota.entity.Rate;
import com.toyota.service.CoordinatorService;
import com.toyota.service.SubscriberService;


import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class RestSubscriberImpl implements SubscriberService {

    private static final long SUBSCRIPTION_DELAY_MS = 500;

    private String API_KEY;
    private final String BASE_URL;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Set<String> receivedRates;
    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> activeSubscriptions;

    private final CoordinatorService coordinator;


    public RestSubscriberImpl(CoordinatorService coordinator) {
        this.coordinator = coordinator;

        this.BASE_URL = ConfigUtil.getValue("rest.platform.base.url");

        this.objectMapper = configureObjectMapper();
        this.httpClient = configureHttpClient();

        this.scheduler = Executors.newScheduledThreadPool(1);
        this.activeSubscriptions = new ConcurrentHashMap<>();
        this.receivedRates = new HashSet<>();
    }

    @Override
    public void connect(String platformName, String username, String password) {
        try {
            HttpRequest authRequest = buildAuthRequest(username, password);

            HttpResponse<String> authResponse = httpClient
                    .send(authRequest, HttpResponse.BodyHandlers.ofString());

            if (authResponse.statusCode() == 200) {
                ApiKeyResponse apiKeyResponse = objectMapper
                        .readValue(
                                authResponse.body(),
                                ApiKeyResponse.class
                        );
                this.API_KEY = apiKeyResponse.getApiKey();
                System.out.println(API_KEY);

                coordinator.onConnect(platformName, true);
            } else {
                System.out.println("Authentication failed with status: " +
                        authResponse.statusCode());
                coordinator.onConnect(platformName, false);
            }

        } catch (IOException | InterruptedException e) {
            System.err.printf("Failed to connect to Rest Platform. Message: %s", e.getMessage());
            coordinator.onConnect(platformName, false);
        }

    }

    @Override
    public void disConnect(String platformName, String username, String password) {
        activeSubscriptions.values()
                .forEach(scheduledJob -> scheduledJob.cancel(false));
        activeSubscriptions.clear();
        this.receivedRates.clear();
        this.API_KEY = null;
    }

    @Override
    public void subscribe(String platformName, String rateName) {
        if (API_KEY == null || activeSubscriptions.containsKey(rateName)) {
            return;
        }

        String rateUrl = String.format("%s/api/rates/%s_%s", BASE_URL, platformName, rateName); //localhost:8092/api/rates/REST_USDTRY

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(rateUrl))
                .header("Authorization", "Bearer " + API_KEY)
                .GET()
                .build();

        Runnable subscribeJob = createSubscribeJob(platformName, rateName, request);

        ScheduledFuture<?> scheduledJob = scheduler.scheduleWithFixedDelay(
                subscribeJob,
                0,
                SUBSCRIPTION_DELAY_MS,
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
        }
    }


    private Runnable createSubscribeJob(String platformName, String rateName, HttpRequest request) {
        return () -> {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    Rate rate = objectMapper.readValue(response.body(), Rate.class);
                    if (!receivedRates.contains(rateName)) {
                        receivedRates.add(rateName);
                        coordinator.onRateAvailable(platformName, rateName, rate);
                    } else {
                        coordinator.onRateUpdate(platformName, rateName, rate);
                    }
                } else {
                    System.err.printf("Failed to fetch rate '%s' from platform '%s': HTTP status code %d%n", rateName, platformName, response.statusCode());
                }
            } catch (JsonProcessingException e) {
                System.err.printf("Failed to parse authentication response for platform '%s': %s%n", platformName, e.getMessage());
            } catch (IOException e) {
                System.err.printf("I/O error while connecting to REST platform '%s': %s%n", platformName, e.getMessage());
            } catch (InterruptedException e) {
                System.err.printf("Connection to REST platform '%s' interrupted: %s%n", platformName, e.getMessage());
                Thread.currentThread().interrupt();
            }
        };
    }


    private HttpRequest buildAuthRequest(String username, String password) {
        String requestBody = """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password);
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/login"))
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