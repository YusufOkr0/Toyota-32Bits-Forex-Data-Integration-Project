package com.toyota.calculation;

import com.toyota.entity.CalculatedRate;
import org.graalvm.polyglot.Context;

import java.math.BigDecimal;
import java.util.List;

public interface CalculationService {

    boolean isInComingRateValid(String inComingBid, String inComingAsk, List<String> cachedBids, List<String> cachedAsks);

    BigDecimal calculateUsdTryMidValue(List<String> cachedUsdTryBids, List<String> cachedUsdTryAsks);

    CalculatedRate calculateUsdTry(List<String> cachedBids, List<String> cachedAsks);

    CalculatedRate calculateRateDependentOnUsdTry(String rateName,String usdTryMid, List<String> cachedBids, List<String> cachedAsk);

    ThreadLocal<Context> getContextHolder();
}
