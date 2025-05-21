package com.toyota.service;

import com.toyota.entity.Rate;
/**
 * Defines the contract for a service responsible for managing incoming exchange rates.
 * This includes handling the initial reception of rates, processing subsequent updates,
 * validating them, triggering calculations, and interacting with caching and publishing mechanisms.
 */
public interface RateManager {

    /**
     * Handles the initial arrival of a specific exchange rate from a particular platform.
     * This is typically called when a rate is received for the first time after a connection
     * or after its previous cached entries have expired.
     * Implementations store this raw rate and publish.
     *
     * @param platformName The name of the platform from which the rate originated.
     * @param rateName     The identifier of the exchange rate (e.g., "USDTRY").
     * @param inComingRate The initially received {@link Rate} object.
     */
    void handleFirstInComingRate(String platformName, String rateName, Rate inComingRate);

    /**
     * Handles subsequent updates for a specific exchange rate from a particular platform.
     * This method is called after handleFirstInComingRate has been invoked at least once
     * for the given rate from any platform (and the cache hasn't expired).
     * Implementations typically validate the incoming rate against existing cached rates,
     * store the valid update, publish it, and potentially trigger recalculations
     * for the target rate or dependent rates.
     *
     * @param platformName The name of the platform providing the rate update.
     * @param rateName     The identifier of the exchange rate being updated (e.g., "USDTRY").
     * @param inComingRate The updated {@link Rate} object.
     */
    void handleRateUpdate(String platformName, String rateName, Rate inComingRate);

}
