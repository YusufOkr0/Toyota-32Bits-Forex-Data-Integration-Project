package com.toyota.service.Impl;

import com.toyota.calculation.CalculationService;
import com.toyota.entity.Rate;
import com.toyota.service.RateManager;
import com.toyota.service.RedisService;

import java.util.List;


public class RateManagerImpl implements RateManager {

    private final RedisService redisService;
    private final CalculationService calculationService;

    public RateManagerImpl(RedisService redisService, CalculationService calculationService){
        this.redisService = redisService;
        this.calculationService = calculationService;
    }


    public void handleFirstInComingRate(String platformName, String rateName, Rate inComingRate){
        redisService.saveRawRate(
                platformName,
                rateName,
                inComingRate
        );
    }

    public void handleRateUpdate(String platformName, String rateName, Rate inComingRate){

        List<Rate> rates = redisService.getAllRawRatesByRateName(rateName);     // TÜM PLATFORMLARDAN USD TRY ALINDI.

        List<String> cachedBids = rates.stream().map(rate -> rate.getBid().toString()).toList();      // BID VE ASK DEGERLERI AYRI OLARAK ALINDI.
        List<String> cachedAsks = rates.stream().map(rate -> rate.getAsk().toString()).toList();

        String newBid = inComingRate.getBid().toString();
        String newAsk = inComingRate.getAsk().toString();

        if(calculationService.isInComingRateValid(newBid,newAsk,cachedBids,cachedAsks)){
            System.out.println("YEPPP IT IS WORKING..");
        }




        // WRITE TO LOGS IF IT IS NOT VALID.


    }



}
