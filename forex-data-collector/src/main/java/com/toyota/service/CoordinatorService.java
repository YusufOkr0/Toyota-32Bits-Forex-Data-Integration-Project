package com.toyota.service;

import com.toyota.entity.Rate;
import com.toyota.entity.RateStatus;

public interface CoordinatorService {
    void onConnect(String platformName, Boolean status) ;

    void onDisConnect(String platformName, Boolean status);

    void onRateAvailable(String platformName, String rateName, Rate rate);

    void onRateUpdate(String platformName, String rateName, Rate rate);

}
