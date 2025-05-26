package com.toyota.restdataprovider.service.concretes;

import com.toyota.restdataprovider.dtos.response.RateDto;
import com.toyota.restdataprovider.entity.Rate;
import com.toyota.restdataprovider.exception.CurrencyPairNotFoundException;
import com.toyota.restdataprovider.repository.RateRepository;
import com.toyota.restdataprovider.service.abstracts.RateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;


@Service
@Slf4j
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

        log.info("Fetching currency pair: {}", rateName);

        Rate rate = rateRepository.findByNameIgnoreCase(rateName)
                .orElseThrow(() -> {
                    log.warn("Currency code {} is invalid.",rateName);
                    return new CurrencyPairNotFoundException(String.format("Currency code is invalid: %s", rateName));
                });

        RateDto rateDto = RateDto.builder()
                .name(rate.getName())
                .bid(rate.getBid())
                .ask(rate.getAsk())
                .timestamp(rate.getTimestamp())
                .build();
        log.debug("Currency pair retrieved: {} with bid: {}, ask: {}", rateName, rate.getBid(), rate.getAsk());
        return rateDto;
    }




    @Scheduled(fixedDelayString = "${rate.update.interval}",initialDelay = 3000L)
    private void updateCurrencyPairs(){
        log.debug("Starting currency pairs update, spikeCounter: {}", spikeCounter);
        spikeCounter++;

        Iterable<Rate> allRatesInRepo = rateRepository.findAll();

        for(Rate rate : allRatesInRepo){
            if (rate == null) {
                log.warn("Encountered null rate during update. Please check if everything is okay in Redis.");
                continue;
            }

            String rateName = rate.getName();
            BigDecimal bid = rate.getBid();
            BigDecimal ask = rate.getAsk();
            BigDecimal spread = ask.subtract(bid);  // CONSTANT SPREAD FOR MY CASE.

            log.debug("Processing rate: {}, current bid: {}, ask: {}", rateName, bid, ask);

            BigDecimal changePercentage = determineChangePercentage();              // DETERMINE CHANGE AMOUNT

            log.debug("Change percentage for {}: {}", rateName, changePercentage);

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
            rate.setTimestamp(Instant.now());
            log.debug("Updated rate: {}, new bid: {}, new ask: {}", rateName, rate.getBid(), rate.getAsk());

            rateRepository.save(rate);
        }
        log.debug("Currency pairs update completed");
    }



    private BigDecimal determineChangePercentage(){
        BigDecimal changePercentage;
        if (spikeCounter % spikeInterval == 0) {
            changePercentage = spikePercentage;
            spikeCounter = 0;
            log.debug("Applying spike percentage: {}", changePercentage);
        } else {
            changePercentage = maximumRateChange
                    .subtract(minimumRateChange)
                    .multiply(BigDecimal.valueOf(Math.random()))
                    .add(minimumRateChange);
            log.debug("Calculated random change percentage: {}", changePercentage);
        }

        if(Math.random() < 0.5){
            changePercentage = changePercentage.negate();
            log.debug("Negated change percentage: {}", changePercentage);
        }
        return changePercentage;
    }


    private BigDecimal applyRateBounds(BigDecimal bidValue, BigDecimal minLimit, BigDecimal maxLimit) {
        if (bidValue.compareTo(minLimit) < 0) {
            log.debug("Bid value {} below min limit {}, adjusted to min limit", bidValue, minLimit);
            return minLimit;
        } else if (bidValue.compareTo(maxLimit) > 0) {
            log.debug("Bid value {} above max limit {}, adjusted to max limit", bidValue, maxLimit);
            return maxLimit;
        }
        return bidValue;
    }


}



