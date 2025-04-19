package com.toyota.cache;

import com.toyota.entity.CalculatedRate;
import com.toyota.entity.Rate;
import java.util.List;


public interface CacheService {

    void saveRawRate(String platformName, String rateName, Rate rate);

    void saveCalculatedRate(String rateName, CalculatedRate rate);

    List<Rate> getAllRawRatesByRateName(String rateName);

}