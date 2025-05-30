package com.toyota.service.Impl;

import com.toyota.cache.CacheService;
import com.toyota.calculation.CalculationService;
import com.toyota.entity.CalculatedRate;
import com.toyota.entity.Rate;
import com.toyota.publisher.KafkaService;
import com.toyota.service.RateManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.List;


public class RateManagerImpl implements RateManager {

    private static final Logger log = LogManager.getLogger(RateManagerImpl.class);

    private final KafkaService kafkaService;
    private final CacheService redisService;
    private final CalculationService calculationService;

    public RateManagerImpl(KafkaService kafkaService, CacheService redisService, CalculationService calculationService) {
        this.kafkaService = kafkaService;
        this.redisService = redisService;
        this.calculationService = calculationService;
    }


    public void handleFirstInComingRate(String platformName, String rateName, Rate inComingRate) {
        log.info("handleFirstInComingRate: Handling first incoming rate for {}/{}: {}", platformName, rateName, inComingRate);

        redisService.saveRawRate(platformName, rateName, inComingRate);
        kafkaService.sendRawRate(inComingRate);

        if (rateName.equals("USDTRY")) {
            calculateAndSaveUsdTryMidValue();
        }
    }


    public void handleRateUpdate(String platformName, String rateName, Rate inComingRate) {
        log.info("handleRateUpdate: Handling rate update for {}/{}: {}", platformName, rateName, inComingRate);

        List<Rate> cachedRates = redisService.getAllRawRatesByRateName(rateName);

        if (cachedRates.isEmpty()) {
            log.warn("handleRateUpdate: Cache is empty for rateName '{}'. Treating incoming rate from platform '{}' as the new baseline: {}",
                    rateName, platformName, inComingRate);
            handleFirstInComingRate(platformName, rateName, inComingRate);
            return;
        }

        List<String> cachedBids = cachedRates.stream().map(rate -> rate.getBid().toPlainString()).toList();
        List<String> cachedAsks = cachedRates.stream().map(rate -> rate.getAsk().toPlainString()).toList();

        String newBid = inComingRate.getBid().toPlainString();
        String newAsk = inComingRate.getAsk().toPlainString();


        if (calculationService.isInComingRateValid(newBid, newAsk, cachedBids, cachedAsks)) {
            log.info("handleRateUpdate: Incoming rate: {} from platform: {} is valid. Saving Redis and sending to Kafka.", rateName, platformName);
            redisService.saveRawRate(platformName, rateName, inComingRate);
            kafkaService.sendRawRate(inComingRate);

            if (rateName.equals("USDTRY")) {
                calculateAndSaveUsdTry();           // CHECK LATER.  DO I NEED TO UPDATE DEPENDENT RATES WHEN USD/TRY UPDATE ???
                calculateAndSaveUsdTryMidValue();
            } else {
                calculateAndSaveRatesDependentOnUsdTry(rateName);
            }

        } else {
            log.warn("handleRateUpdate: Invalid rate detected and ignored: {}", inComingRate);
        }
    }


    private void calculateAndSaveUsdTry() {
        final String rateName = "USDTRY";

        log.debug("calculateAndSaveUsdTry: Attempting calculation for {}.", rateName);

        List<Rate> cachedRates = redisService.getAllRawRatesByRateName(rateName);

        if (cachedRates.isEmpty()) {
            log.warn("calculateAndSaveUsdTry: Calculation skipped for {}: No rates found in cache.", rateName);
            return;
        }

        List<String> cachedUsdTryBids = cachedRates.stream().map(rate -> rate.getBid().toPlainString()).toList();
        List<String> cachedUsdTryAsks = cachedRates.stream().map(rate -> rate.getAsk().toPlainString()).toList();

        CalculatedRate calculatedRate = calculationService.calculateUsdTry(
                cachedUsdTryBids,
                cachedUsdTryAsks
        );

        if(calculatedRate != null){
            log.info("calculateAndSaveUsdTry: Calculated USDTRY rate: {}", calculatedRate);

            redisService.saveCalculatedRate(rateName,calculatedRate);
            kafkaService.sendCalculatedRate(calculatedRate);
        }
    }


    private void calculateAndSaveUsdTryMidValue() {

        List<Rate> existsUsdTryRates = redisService.getAllRawRatesByRateName("USDTRY");

        if (existsUsdTryRates.isEmpty()) {
            log.warn("calculateAndSaveUsdTryMidValue: USD/TRY Mid value calculation skipped. No rates found in cache.");
            return;
        }

        List<String> cachedUsdTryBids = existsUsdTryRates.stream().map(rate -> rate.getBid().toPlainString()).toList();
        List<String> cachedUsdTryAsks = existsUsdTryRates.stream().map(rate -> rate.getAsk().toPlainString()).toList();

        BigDecimal usdTryMid = calculationService.calculateUsdTryMidValue(
                cachedUsdTryBids,
                cachedUsdTryAsks
        );

        if (usdTryMid != null) {
            redisService.saveUsdTryMidValue(usdTryMid);
        }

    }


    private void calculateAndSaveRatesDependentOnUsdTry(String updatedRateName) {
        log.debug("calculateAndSaveRatesDependentOnUsdTry: Attempting calculation for '{}'.", updatedRateName);

        List<Rate> cachedRates = redisService.getAllRawRatesByRateName(updatedRateName);
        BigDecimal usdTryMid = redisService.getUsdTryMidValue();

        if (cachedRates.isEmpty() || usdTryMid == null) {
            log.warn("calculateAndSaveRatesDependentOnUsdTry: Missing required rates for {} calculation. ",updatedRateName);
            return;
        }

        List<String> cachedBids = cachedRates.stream().map(rate -> rate.getBid().toString()).toList();
        List<String> cachedAsks = cachedRates.stream().map(rate -> rate.getAsk().toString()).toList();


        String derivedRate = updatedRateName.replace("USD", "TRY"); // GBPUSD -> GBPTRY or any other usd based currency pair.

        CalculatedRate calculatedRate = calculationService.calculateRateDependentOnUsdTry(
                derivedRate,
                usdTryMid.toPlainString(),
                cachedBids,
                cachedAsks
        );

        if(calculatedRate != null){
            log.info("calculateAndSaveRatesDependentOnUsdTry: Calculated dependent rate: {}", calculatedRate);

            redisService.saveCalculatedRate(derivedRate,calculatedRate);
            kafkaService.sendCalculatedRate(calculatedRate);
        }

    }


}
