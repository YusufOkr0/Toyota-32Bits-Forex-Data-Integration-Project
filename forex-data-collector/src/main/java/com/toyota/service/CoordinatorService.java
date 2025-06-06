package com.toyota.service;

import com.toyota.entity.Rate;
/**
 * Defines the contract for a central coordination service.
 * This service acts as a mediator, receiving events (connection status, rate updates)
 * from various {@link SubscriberService} implementations and orchestrating the
 * application's response, often by interacting with other services like {@link RateManager}.
 */
public interface CoordinatorService {

    /**
     * Callback method invoked by a {@link SubscriberService} after a connection attempt.
     * Used to report the success or failure of the connection establishment.
     *
     * @param platformName The name of the platform that attempted to connect.
     * @param status       {@code true} if the connection was successful, {@code false} otherwise.
     */
    void onConnect(String platformName, Boolean status);

    /**
     * Callback method invoked when a connection to a platform is lost or intentionally closed.
     * This signals the coordinator to handle the disconnection event, potentially triggering retries.
     *
     * @param platformName The name of the platform that disconnected.
     */
    void onDisConnect(String platformName);

    /**
     * Callback method invoked by a {@link SubscriberService} when the *first* rate update
     * for a specific subscription is received after connecting or subscribing.
     *
     * @param platformName The name of the platform providing the rate.
     * @param rateName     The specific rate identifier (e.g., "USDTRY").
     * @param rate         The initial {@link Rate} object received.
     */
    void onRateAvailable(String platformName, String rateName, Rate rate);

    /**
     * Callback method invoked by a {@link SubscriberService} for *subsequent* rate updates
     * after the initial {@code onRateAvailable} call for a subscription.
     *
     * @param platformName The name of the platform providing the rate update.
     * @param rateName     The specific rate identifier (e.g., "USDTRY").
     * @param rate         The updated {@link Rate} object received.
     */
    void onRateUpdate(String platformName, String rateName, Rate rate);
}
