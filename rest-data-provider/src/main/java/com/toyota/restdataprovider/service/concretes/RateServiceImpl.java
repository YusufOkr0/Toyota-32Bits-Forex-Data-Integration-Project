package com.toyota.restdataprovider.service.concretes;

import com.toyota.restdataprovider.entity.Rate;
import com.toyota.restdataprovider.service.abstracts.RateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class RateServiceImpl implements RateService {


    private static final String MINIMUM_CHANGE = "0.003";
    private static final String MAXIMUM_CHANGE = "0.007";


    private final Map<String, Rate> currencyPairRepository;


    public RateServiceImpl(
            @Value("${REST_USDTRY.BID}") String usdTryBid,
            @Value("${REST_USDTRY.ASK}") String usdTryAsk,
            @Value("${REST_EURUSD.BID}") String eurUsdBid,
            @Value("${REST_EURUSD.ASK}") String eurUsdAsk,
            @Value("${REST_GBPUSD.BID}") String gbpUsdBid,
            @Value("${REST_GBPUSD.ASK}") String gbpUsdAsk
    ) {
        this.currencyPairRepository = new ConcurrentHashMap<>();

        setUpInitialRates(
                usdTryBid,
                usdTryAsk,
                eurUsdBid,
                eurUsdAsk,
                gbpUsdBid,
                gbpUsdAsk
        );

    }




    public Rate getCurrencyPair(String rateName){
        if(currencyPairRepository.containsKey(rateName)){
            // CURRENCY PAIR NOT FOUND.
        }
        return currencyPairRepository.get(rateName);
    }


    /***
     *    TODO: ADJUST THE VARIABLE AFTER COMMA
     *    TODO: PRODUCE INCORRECT DATA [ CHANGE PERCENTAGE = 1 % ]
     *
     */
    @Scheduled(fixedDelay = 2000L)
    private void updateCurrencyPairs(){

        BigDecimal minChange = new BigDecimal(MINIMUM_CHANGE);  // 0.3%    // Change interval. CHECK LATER.
        BigDecimal maxChange = new BigDecimal(MAXIMUM_CHANGE);  // 0.7%

        for(String currencyPair : currencyPairRepository.keySet()){     // search the currency pairs.

            Rate rate = currencyPairRepository.get(currencyPair);

            BigDecimal bid = rate.getBid();
            BigDecimal ask = rate.getAsk();
            BigDecimal spread = ask.subtract(bid);  // CONSTANT SPREAD FOR MY CASE.

            BigDecimal changePercentage = maxChange.subtract(minChange)     // ( max - min ) * rnd + min
                    .multiply(BigDecimal.valueOf(Math.random()))
                    .add(minChange);


            BigDecimal newBid = BigDecimal.ONE
                    .add(changePercentage)          //  bid * (1 + %)/100
                    .multiply(bid);

            BigDecimal newAsk = newBid.add(spread);


            rate.setBid(newBid);        // UPDATE RATES WITH THE NEW VARIABLES
            rate.setAsk(newAsk);
            rate.setTimestamp(LocalDateTime.now());

        }


    }






    private void setUpInitialRates(
            String usdTryBid, String usdTryAsk,
            String eurUsdBid, String eurUsdAsk,
            String gbpUsdBid, String gbpUsdAsk
    ) {
        LocalDateTime localDateTime = LocalDateTime.now();

        Rate USD_TRY = new Rate(
                "REST_USDTRY",
                parseBigDecimal(usdTryBid),
                parseBigDecimal(usdTryAsk),
                localDateTime
        );
        Rate EUR_USD = new Rate(
                "REST_EURUSD",
                parseBigDecimal(eurUsdBid),
                parseBigDecimal(eurUsdAsk),
                localDateTime
        );
        Rate GBP_USD = new Rate(
                "REST_GBPUSD",
                parseBigDecimal(gbpUsdBid),
                parseBigDecimal(gbpUsdAsk),
                localDateTime
        );

        currencyPairRepository.put("REST_USDTRY", USD_TRY);
        currencyPairRepository.put("REST_EURUSD", EUR_USD);
        currencyPairRepository.put("REST_GBPUSD", GBP_USD);
    }



    private BigDecimal parseBigDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Exception while parsing String to BigDecimal. Please check the values inside the properties file.");
        }
    }


}



