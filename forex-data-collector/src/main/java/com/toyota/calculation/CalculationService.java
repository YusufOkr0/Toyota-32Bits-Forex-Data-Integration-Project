package com.toyota.calculation;

import com.toyota.entity.CalculatedRate;
import org.graalvm.polyglot.Context;

import java.math.BigDecimal;
import java.util.List;


/**
 * A general interface for performing currency rate calculations.
 * <p>
 * Provides methods for validating incoming rates and computing both direct and derived exchange rates.
 * </p>
 */
public interface CalculationService {

    /**
     * Validates an incoming rate based on current cached bid and ask values.
     *
     * @param inComingBid  the incoming bid value to validate
     * @param inComingAsk  the incoming ask value to validate
     * @param cachedBids   the list of recently cached bid values
     * @param cachedAsks   the list of recently cached ask values
     * @return true if the incoming rate is considered valid; false otherwise
     */
    boolean isInComingRateValid(String inComingBid, String inComingAsk, List<String> cachedBids, List<String> cachedAsks);

    /**
     * Calculates the mid value of USD/TRY using cached bid and ask values.
     *
     * @param cachedUsdTryBids  the list of cached USD/TRY bid values
     * @param cachedUsdTryAsks  the list of cached USD/TRY ask values
     * @return the computed mid value as a {@link BigDecimal}, or null if calculation fails
     */
    BigDecimal calculateUsdTryMidValue(List<String> cachedUsdTryBids, List<String> cachedUsdTryAsks);

    /**
     * Calculates the current USD/TRY rate based on cached data.
     *
     * @param cachedBids the list of cached bid values
     * @param cachedAsks the list of cached ask values
     * @return the calculated {@link CalculatedRate} object, or null if calculation fails
     */
    CalculatedRate calculateUsdTry(List<String> cachedBids, List<String> cachedAsks);

    /**
     * Calculates a derived currency rate using a base currency pair and the USD/TRY mid value.
     * <p>
     * For example, if the base rate is EUR/USD and the USD/TRY mid value is known,
     * this method calculates the derived rate EUR/TRY.
     * </p>
     *
     * @param rateName the name of the base rate (e.g., "EURUSD" or "GBPUSD")
     * @param usdTryMid    the mid value of USD/TRY used in the calculation
     * @param cachedBids   the list of cached bid values for the base currency pair
     * @param cachedAsks   the list of cached ask values for the base currency pair
     * @return the calculated {@link CalculatedRate} object representing the derived rate (e.g., EURTRY),
     *         or null if the calculation fails
     */
    CalculatedRate calculateRateDependentOnUsdTry(String rateName,String usdTryMid, List<String> cachedBids, List<String> cachedAsks);


}
