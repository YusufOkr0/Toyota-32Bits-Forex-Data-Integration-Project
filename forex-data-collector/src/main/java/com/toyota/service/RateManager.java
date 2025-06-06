package com.toyota.service;

import com.toyota.entity.Rate;
/**
 * Defines the contract for a service responsible for managing incoming exchange rates.
 * This includes handling the initial reception of rates, processing subsequent updates,
 * validating them, triggering calculations, and interacting with caching and publishing mechanisms.
 */
public interface RateManager {


    /**
     * Handles the first incoming exchange rate for a specific currency pair
     * received from a particular platform.
     * <p>
     * This method is invoked when a rate for a given {@code rateName} from
     * a specific {@code platformName} is received for the first time.
     * Implementations should typically store this initial rate in the cache
     * and potentially trigger initial calculations or publishing actions.
     * </p>
     *
     * @param platformName The name of the platform from which the rate was received (e.g., "REST", "TCP").
     * @param rateName     The name of the currency pair (e.g., "USDTRY", "EURUSD").
     * @param inComingRate The {@link Rate} object containing the bid and ask values and other details.
     */
    void handleFirstInComingRate(String platformName, String rateName, Rate inComingRate);

    /**
     * Handles subsequent updates for an existing exchange rate for a specific
     * currency pair from a particular platform.
     * <p>
     * This method is called when a rate for an already tracked {@code rateName}
     * from a specific {@code platformName} is received. Implementations should
     * typically validate the {@code inComingRate} against cached data, update
     * the cache if the rate is valid, and trigger any necessary recalculations
     * for this rate or other rates dependent on it.
     * </p>
     *
     * @param platformName The name of the platform from which the rate update was received (e.g., "REST", "TCP").
     * @param rateName     The name of the currency pair being updated.
     * @param inComingRate The updated {@link Rate} object.
     */
    void handleRateUpdate(String platformName, String rateName, Rate inComingRate);


}
