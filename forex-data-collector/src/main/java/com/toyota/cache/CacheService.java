package com.toyota.cache;

import com.toyota.entity.CalculatedRate;
import com.toyota.entity.Rate;

import java.math.BigDecimal;
import java.util.List;

/**
 * A generic caching service interface for storing and retrieving exchange rate data.
 * <p>
 * This interface defines methods for saving and accessing both raw and calculated
 * exchange rates, typically used for performance optimization and quick data access.
 * </p>
 */
public interface CacheService {

    /**
     * Stores a raw exchange rate entry in the cache system.
     *
     * @param platformName the name of the source platform for the rate (e.g., an external API or service)
     * @param rateName     the identifier for the exchange rate (e.g., currency pair)
     * @param rate         the raw exchange rate data to be cached
     */
    void saveRawRate(String platformName, String rateName, Rate rate);


    /**
     * Stores a calculated exchange rate entry in the cache system.
     *
     * @param rateName the identifier for the exchange rate (e.g., currency pair)
     * @param rate     the calculated exchange rate data to be cached
     */
    void saveCalculatedRate(String rateName, CalculatedRate rate);


    /**
     * Retrieves all cached raw exchange rate entries that match the given rate name.
     * <p>
     * The returned list may include data from multiple sources or platforms.
     * </p>
     *
     * @param rateName the identifier for the exchange rate to retrieve
     * @return a list of matching raw exchange rate entries
     */
    List<Rate> getAllRawRatesByRateName(String rateName);


    /**
     * Stores the USD/TRY mid value in the cache.
     *
     * @param usdMidValue the USD/TRY mid exchange rate value to be cached
     */
    void saveUsdTryMidValue(BigDecimal usdMidValue);

    /**
     * Retrieves the cached USD/TRY mid value.
     *
     * @return the cached USD/TRY mid value, or null if not present
     */
    BigDecimal getUsdTryMidValue();

}