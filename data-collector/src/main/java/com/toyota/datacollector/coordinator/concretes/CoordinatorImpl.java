package com.toyota.datacollector.coordinator.concretes;


import com.toyota.datacollector.coordinator.abstracts.CoordinatorService;
import com.toyota.datacollector.entity.Rate;
import com.toyota.datacollector.entity.RateFields;
import com.toyota.datacollector.entity.RateStatus;
import org.springframework.stereotype.Service;

@Service
public class CoordinatorImpl implements CoordinatorService {


    public CoordinatorImpl(){
    }


    @Override
    public void onConnect(String platformName, Boolean status) {

    }

    @Override
    public void onDisConnect(String platformName, Boolean status) {
    }

    @Override
    public void onRateAvailable(String platformName, String rateName, Rate rate) {


    }

    @Override
    public void onRateUpdate(String platformName, String rateName, RateFields rateFields) {

    }


    @Override
    public void onRateStatus(String platformName, String rateName, RateStatus rateStatus) {

    }





}
