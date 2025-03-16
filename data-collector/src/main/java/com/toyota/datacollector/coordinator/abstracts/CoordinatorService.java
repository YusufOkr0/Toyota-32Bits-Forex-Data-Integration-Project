package com.toyota.datacollector.coordinator.abstracts;


import com.toyota.datacollector.entity.Rate;
import com.toyota.datacollector.entity.RateFields;
import com.toyota.datacollector.entity.RateStatus;

public interface CoordinatorService {


    void onConnect(String platformName, Boolean status) ;

    void onDisConnect(String platformName, Boolean status);

    void onRateAvailable(String platformName, String rateName, Rate rate);

    void onRateUpdate(String platformName, String rateName, RateFields rateFields);

    void onRateStatus(String platformName, String rateName, RateStatus rateStatus);

}
