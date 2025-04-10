package com.toyota.calculation;

import java.util.List;

public interface CalculationService {

    boolean isInComingRateValid(String inComingBid, String inComingAsk, List<String> cachedBids, List<String> cachedAsks);


}
