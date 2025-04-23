package com.toyota.restdataprovider.service.concretes;

import com.toyota.restdataprovider.config.InitialRateConfig;
import com.toyota.restdataprovider.dtos.response.RateDto;
import com.toyota.restdataprovider.entity.Rate;
import com.toyota.restdataprovider.exception.CurrencyPairNotFoundException;
import com.toyota.restdataprovider.repository.RateRepository;
import com.toyota.restdataprovider.service.abstracts.RateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class RateServiceImpl implements RateService {

    @Value("${minimum.rate.change}")
    private BigDecimal minimumRateChange;

    @Value("${maximum.rate.change}")
    private BigDecimal maximumRateChange;

    @Value("${spike.percentage}")
    private BigDecimal spikePercentage;

    @Value("${spike.interval}")
    private int spikeInterval;
    private int spikeCounter = 0;

    private final RateRepository rateRepository;

    public RateServiceImpl(RateRepository rateRepository) {
        this.rateRepository = rateRepository;
    }



    public RateDto getCurrencyPair(String rateName){

        Rate rate = rateRepository.findByNameIgnoreCase(rateName)
                .orElseThrow(() -> new CurrencyPairNotFoundException(String.format("Currency code is invalid: %s",rateName)));

        return RateDto.builder()
                .name(rate.getName())
                .bid(rate.getBid())
                .ask(rate.getAsk())
                .timestamp(rate.getTimestamp())
                .build();
    }




    @Scheduled(fixedDelayString = "${rate.update.interval}",initialDelay = 3000L)
    private void updateCurrencyPairs(){
        spikeCounter++;

        Iterable<Rate> allRatesInRepo = rateRepository.findAll();

        for(Rate rate : allRatesInRepo){
            if(rate == null){
                System.err.println("Rate is null.");
                return;
            }

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
            if (rateName.equals("REST_EURUSD"))System.out.println(rate.toString() + "  " + spikeCounter);
            if (rateName.equals("REST_GBPUSD"))System.out.println(rate.toString() + "  " + spikeCounter);
        }

    }



    private BigDecimal determineChangePercentage(){
        BigDecimal changePercentage;

        if (spikeCounter % spikeInterval == 0) {
            changePercentage = spikePercentage;
            spikeCounter = 0;
        } else {
            changePercentage = maximumRateChange
                    .subtract(minimumRateChange)
                    .multiply(BigDecimal.valueOf(Math.random()))
                    .add(maximumRateChange);
        }

        if(Math.random() < 0.5){
            changePercentage = changePercentage.negate();
        }

        return changePercentage;
    }


    private BigDecimal applyRateBounds(BigDecimal bidValue, BigDecimal minLimit, BigDecimal maxLimit) {
        if (bidValue.compareTo(minLimit) < 0) {
            return minLimit;
        } else if (bidValue.compareTo(maxLimit) > 0) {
            return maxLimit;
        }
        return bidValue;
    }


}



