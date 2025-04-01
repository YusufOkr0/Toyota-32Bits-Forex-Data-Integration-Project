package com.toyota.restdataprovider.service.concretes;

import com.toyota.restdataprovider.dtos.response.RateDto;
import com.toyota.restdataprovider.entity.Rate;
import com.toyota.restdataprovider.exception.CurrencyPairNotFoundException;
import com.toyota.restdataprovider.service.abstracts.RateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class RateServiceImpl implements RateService {

    private static final BigDecimal MINIMUM_RATE_CHANGE = new BigDecimal("0.001");
    private static final BigDecimal MAXIMUM_RATE_CHANGE = new BigDecimal("0.004");


    private static final BigDecimal SPIKE_PERCENTAGE = new BigDecimal("0.011");  // % 1.1  FOR NOW. CHECK LATER
    private static final int SPIKE_INTERVAL = 10;
    private int spikeCounter = 0;


    private final Map<String, Rate> currencyPairRepository;



    public RateServiceImpl(
            @Value("${rest.usdtry.bid}") String usdTryBid,
            @Value("${rest.usdtry.ask}") String usdTryAsk,
            @Value("${rest.eurusd.bid}") String eurUsdBid,
            @Value("${rest.eurusd.ask}") String eurUsdAsk,
            @Value("${rest.gbpusd.bid}") String gbpUsdBid,
            @Value("${rest.gbpusd.ask}") String gbpUsdAsk,
            @Value("${rest.usdtry.min-limit}") String usdTryMinLimit,
            @Value("${rest.usdtry.max-limit}") String usdTryMaxLimit,
            @Value("${rest.eurusd.min-limit}") String eurUsdMinLimit,
            @Value("${rest.eurusd.max-limit}") String eurUsdMaxLimit,
            @Value("${rest.gbpusd.min-limit}") String gbpUsdMinLimit,
            @Value("${rest.gbpusd.max-limit}") String gbpUsdMaxLimit
    ) {
        this.currencyPairRepository = new ConcurrentHashMap<>();

        setUpInitialRates(
                usdTryBid, usdTryAsk, usdTryMinLimit, usdTryMaxLimit,
                eurUsdBid, eurUsdAsk, eurUsdMinLimit, eurUsdMaxLimit,
                gbpUsdBid, gbpUsdAsk, gbpUsdMinLimit, gbpUsdMaxLimit
        );

    }





    public RateDto getCurrencyPair(String rateName){
        if(!currencyPairRepository.containsKey(rateName)){
            throw new CurrencyPairNotFoundException(String.format("Currency code is invalid: %s",rateName));
        }
        Rate rate = currencyPairRepository.get(rateName);
        return RateDto.builder()
                .name(rate.getName())
                .bid(rate.getBid())
                .ask(rate.getAsk())
                .timestamp(rate.getTimestamp())
                .build();
    }




    @Scheduled(fixedDelay = 2000L)
    private void updateCurrencyPairs(){
        spikeCounter++;

        for(Rate rate : currencyPairRepository.values()){     // search the currency pairs.

            String rateName = rate.getName();
            BigDecimal bid = rate.getBid();
            BigDecimal ask = rate.getAsk();
            BigDecimal spread = ask.subtract(bid);  // CONSTANT SPREAD FOR MY CASE.

            BigDecimal changePercentage = determineChangePercentage();  // DETERMINE CHANGE AMOUNT

            BigDecimal newBid;
            BigDecimal newAsk;

            newBid = BigDecimal.ONE
                    .add(changePercentage)  //  bid * (1 + %/100)
                    .multiply(bid);


            newBid = applyRateBounds(                     // CHECK IF NEW BID IS IN VALID INTERVAL.
                    newBid,                               // IF NOT THEN MAKE IT EQUALS TO THE LIMIT.
                    rate.getMinLimit(),
                    rate.getMaxLimit()
            );
            newAsk = newBid.add(spread);


            rate.setBid(newBid.setScale(16, RoundingMode.HALF_UP));        // UPDATE RATES WITH THE NEW VARIABLES
            rate.setAsk(newAsk.setScale(16, RoundingMode.HALF_UP));
            rate.setTimestamp(LocalDateTime.now());

            if (rateName.equals("REST_USDTRY"))System.out.println(rate.toString() + "  " + spikeCounter);
        }

    }



    private BigDecimal determineChangePercentage(){
        BigDecimal changePercentage;

        if (spikeCounter % SPIKE_INTERVAL == 0) {
            changePercentage = SPIKE_PERCENTAGE;
        } else {
            changePercentage = MAXIMUM_RATE_CHANGE
                    .subtract(MINIMUM_RATE_CHANGE)
                    .multiply(BigDecimal.valueOf(Math.random()))
                    .add(MINIMUM_RATE_CHANGE);
        }

        if(Math.random() < 0.5){
            changePercentage = changePercentage.negate();
        }

        return changePercentage;
    }


    private BigDecimal applyRateBounds(BigDecimal rate, BigDecimal minLimit, BigDecimal maxLimit) {
        if (rate.compareTo(minLimit) < 0) {
            return minLimit;
        } else if (rate.compareTo(maxLimit) > 0) {
            return maxLimit;
        }
        return rate;
    }






    private BigDecimal parseBigDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Exception while parsing String to BigDecimal. Please check the values inside the properties file.");
        }
    }






    private void setUpInitialRates(
            String usdTryBid, String usdTryAsk, String usdTryMinLimit, String usdTryMaxLimit,
            String eurUsdBid, String eurUsdAsk, String eurUsdMinLimit, String eurUsdMaxLimit,
            String gbpUsdBid, String gbpUsdAsk, String gbpUsdMinLimit, String gbpUsdMaxLimit
    ) {
        LocalDateTime timeStamp = LocalDateTime.now();

        currencyPairRepository.put("REST_USDTRY", new Rate(
                "REST_USDTRY",
                parseBigDecimal(usdTryBid),
                parseBigDecimal(usdTryAsk),
                timeStamp,
                parseBigDecimal(usdTryMinLimit),
                parseBigDecimal(usdTryMaxLimit)
        ));
        currencyPairRepository.put("REST_EURUSD", new Rate(
                "REST_EURUSD",
                parseBigDecimal(eurUsdBid),
                parseBigDecimal(eurUsdAsk),
                timeStamp,
                parseBigDecimal(eurUsdMinLimit),
                parseBigDecimal(eurUsdMaxLimit)
        ));
        currencyPairRepository.put("REST_GBPUSD", new Rate(
                "REST_GBPUSD",
                parseBigDecimal(gbpUsdBid),
                parseBigDecimal(gbpUsdAsk),
                timeStamp,
                parseBigDecimal(gbpUsdMinLimit),
                parseBigDecimal(gbpUsdMaxLimit)
        ));
    }

}



