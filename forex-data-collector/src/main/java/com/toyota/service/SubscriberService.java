package com.toyota.service;

import com.toyota.config.SubscriberConfig;

/**
 * Defines the contract for services that subscribe to real-time or periodic data updates (e.g., currency rates) from external platforms.
 * Implementations handle the specific details of communication protocols (e.g., REST, TCP) and data retrieval.
 */
public interface SubscriberService  {

    /**
     * Establishes a connection to the specified external platform.
     * This typically involves authentication and session setup.
     * Implementations should notify the {@code CoordinatorService} about the connection status.
     *
     * @param platformName The unique identifier or name of the platform to connect to.
     */
    void connect(String platformName);

    /**
     * Disconnects from the currently connected platform and release resources.
     */
    void disConnect();

    /**
     * Subscribes to a specific data feed (e.g., a currency rate) on the specified platform.
     * Once subscribed, the implementation should start receiving updates for this feed and
     * forward them (e.g., via the {@code CoordinatorService}).
     *
     * @param platformName The name of the platform hosting the data feed.
     * @param rateName     The identifier of the specific data feed to subscribe to (e.g., "USDTRY").
     */
    void subscribe(String platformName, String rateName);

    /**
     * Unsubscribes from a specific data feed on the specified platform.
     * This stops the reception of updates for the given feed and may release
     * associated resources.
     *
     * @param platformName The name of the platform hosting the data feed.
     * @param rateName     The identifier of the specific data feed to unsubscribe from (e.g., "USDTRY").
     */
    void unSubscribe(String platformName, String rateName);


    SubscriberConfig getConfig();


}
