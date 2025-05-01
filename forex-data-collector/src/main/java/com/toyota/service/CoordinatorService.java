package com.toyota.service;

import com.toyota.entity.Rate;

public interface CoordinatorService {

    void onConnect(String platformName, Boolean status) ;

    void onDisConnect(String platformName);

    void onRateAvailable(String platformName, String rateName, Rate rate);

    void onRateUpdate(String platformName, String rateName, Rate rate);

}
