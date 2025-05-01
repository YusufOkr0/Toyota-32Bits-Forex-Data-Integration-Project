package com.toyota.service;

import com.toyota.entity.Rate;

public interface RateManager {

    void handleFirstInComingRate(String platformName, String rateName, Rate inComingRate);

    void handleRateUpdate(String platformName, String rateName, Rate inComingRate);

    void warmUpCalculationService();
}
