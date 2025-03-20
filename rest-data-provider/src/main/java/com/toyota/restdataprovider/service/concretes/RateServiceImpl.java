package com.toyota.restdataprovider.service.concretes;

import com.toyota.restdataprovider.entity.Rate;
import com.toyota.restdataprovider.entity.RateLimits;
import com.toyota.restdataprovider.exception.CurrencyPairNotFoundException;
import com.toyota.restdataprovider.service.abstracts.RateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class RateServiceImpl implements RateService {

    private static final BigDecimal MINIMUM_RATE_CHANGE = new BigDecimal("0.001");
    private static final BigDecimal MAXIMUM_RATE_CHANGE = new BigDecimal("0.004");


    private int spikeCounter = 0;
    private static final int SPIKE_INTERVAL = 10;
    private static final BigDecimal SPIKE_PERCENTAGE = new BigDecimal("0.011");  // % 1.1  FOR NOW. CHECK LATER


    private final Map<String, Rate> currencyPairRepository;
    private final Map<String, RateLimits> currencyRateLimits;


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
        this.currencyRateLimits = new HashMap<>();

        setUpInitialRates(usdTryBid,usdTryAsk, eurUsdBid, eurUsdAsk, gbpUsdBid, gbpUsdAsk);
        setUpRateLimits(usdTryMinLimit,usdTryMaxLimit,eurUsdMinLimit,eurUsdMaxLimit,gbpUsdMinLimit,gbpUsdMaxLimit);

    }





    public Rate getCurrencyPair(String rateName){
        if(!currencyPairRepository.containsKey(rateName)){
            throw new CurrencyPairNotFoundException(String.format("Currency pair: %s not found",rateName));
        }
        return currencyPairRepository.get(rateName);
    }




    @Scheduled(fixedDelay = 100L)
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


            RateLimits limitOfRate = currencyRateLimits.get(rateName);

            newBid = applyRateBounds(                                // CHECK IF NEW BID IS IN VALID INTERVAL.
                    newBid,                                          // IF NOT THEN MAKE IT EQUALS TO THE LIMIT.
                    limitOfRate.getMinLimit(),
                    limitOfRate.getMaxLimit()
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













    private void setUpInitialRates(
            String usdTryBid, String usdTryAsk,
            String eurUsdBid, String eurUsdAsk,
            String gbpUsdBid, String gbpUsdAsk
    ) {
        LocalDateTime localDateTime = LocalDateTime.now();

        currencyPairRepository.put("REST_USDTRY", new Rate(
                "REST_USDTRY",
                parseBigDecimal(usdTryBid),
                parseBigDecimal(usdTryAsk),
                localDateTime
        ));
        currencyPairRepository.put("REST_EURUSD", new Rate(
                "REST_EURUSD",
                parseBigDecimal(eurUsdBid),
                parseBigDecimal(eurUsdAsk),
                localDateTime
        ));
        currencyPairRepository.put("REST_GBPUSD", new Rate(
                "REST_GBPUSD",
                parseBigDecimal(gbpUsdBid),
                parseBigDecimal(gbpUsdAsk),
                localDateTime
        ));
    }




    private void setUpRateLimits(
            String usdTryMinLimit, String usdTryMaxLimit,
            String eurUsdMinLimit, String eurUsdMaxLimit,
            String gbpUsdMinLimit, String gbpUsdMaxLimit) {

        currencyRateLimits.put("REST_USDTRY", new RateLimits(parseBigDecimal(usdTryMinLimit), parseBigDecimal(usdTryMaxLimit)));
        currencyRateLimits.put("REST_EURUSD", new RateLimits(parseBigDecimal(eurUsdMinLimit), parseBigDecimal(eurUsdMaxLimit)));
        currencyRateLimits.put("REST_GBPUSD", new RateLimits(parseBigDecimal(gbpUsdMinLimit), parseBigDecimal(gbpUsdMaxLimit)));
    }





    private BigDecimal parseBigDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Exception while parsing String to BigDecimal. Please check the values inside the properties file.");
        }
    }


}



