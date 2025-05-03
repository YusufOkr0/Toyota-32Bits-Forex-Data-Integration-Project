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

    public void warmUpCalculationService() {
        calculationService.getContextHolder().get();
    }



    public void handleFirstInComingRate(String platformName, String rateName, Rate inComingRate) {
        log.info("RateManagerImpl: Handling first incoming rate for {}/{}: {}", platformName, rateName, inComingRate);

        redisService.saveRawRate(
                platformName,
                rateName,
                inComingRate
        );
        kafkaService.sendRawRate(inComingRate);
    }



    public void handleRateUpdate(String platformName, String rateName, Rate inComingRate) {
        log.info("RateManagerImpl: Handling rate update for {}/{}: {}", platformName, rateName, inComingRate);

        List<Rate> existsRates = redisService.getAllRawRatesByRateName(rateName);     // GET RATE FROM REDIS FOR ALL PLATFORMS

        /*
         * Eger Redis'te gelen rateName icin hic veri yoksa ,
         * ( TTL süresi boyunca Platform baglantisi kesildigi icin veya gelen deger valid sinirda olmadigi icin )
         * bu rateyi ilk kez gelmis gibi degerlerndirip sistemin kitlenmesini önlemeliyiz.
         */
        if (existsRates.isEmpty()) {
            log.warn("RateManagerImpl: Cache is empty for rateName '{}'. Treating incoming rate from platform '{}' as the new baseline: {}",
                    rateName, platformName, inComingRate);
            handleFirstInComingRate(platformName, rateName, inComingRate);
            return;
        }
        List<String> cachedBids = existsRates
                .stream()
                .map(rate -> rate.getBid().toPlainString())
                .toList();                                      // GET BIDS AND ASKS SEPARATELY AND CONVERT STRING FORMAT TO EASE CALCULATION
        List<String> cachedAsks = existsRates
                .stream()
                .map(rate -> rate.getAsk().toPlainString())
                .toList();

        log.debug("RateManagerImpl: Prepared {} cached bids and {} cached asks for validation of rate {}",
                cachedBids.size(), cachedAsks.size(), rateName);

        String newBid = inComingRate.getBid().toPlainString();
        String newAsk = inComingRate.getAsk().toPlainString();


        if (calculationService.isInComingRateValid(newBid, newAsk, cachedBids, cachedAsks)) {
        log.debug("RateManagerImpl: Rate: {} is valid. Saving Redis and sending to Kafka.", rateName);
            redisService.saveRawRate(platformName, rateName, inComingRate);
            kafkaService.sendRawRate(inComingRate);

            if (rateName.equals("USDTRY")) {
                calculateAndSaveUsdTry();  // CHECK LATER.  DO I NEED TO UPDATE DEPENDENT RATES WHEN USD/TRY UPDATE ???
            } else {
                calculateAndSaveRatesDependentOnUsdTry(rateName);
            }

        } else {
            log.warn("RateManagerImpl: Invalid incoming rate detected and ignored: {}", inComingRate);
        }
    }


    private void calculateAndSaveUsdTry() {
        final String rateName = "USDTRY";

        log.info("RateManagerImpl: Attempting calculation for {}.", rateName);

        List<Rate> cachedRates = redisService.getAllRawRatesByRateName(rateName);

        if (cachedRates.isEmpty()) {
            log.warn("RateManagerImpl: Calculation skipped for {}: No rates found in cache.", rateName);
            return;
        }

        List<String> cachedUsdTryBids = cachedRates.stream().map(rate -> rate.getBid().toPlainString()).toList();
        List<String> cachedUsdTryAsks = cachedRates.stream().map(rate -> rate.getAsk().toPlainString()).toList();

        CalculatedRate calculatedRate = calculationService.calculateUsdTry(
                cachedUsdTryBids,
                cachedUsdTryAsks
        );
        log.debug("RateManagerImpl: Calculated USDTRY rate: {}", calculatedRate);
        redisService.saveCalculatedRate(
                rateName,
                calculatedRate
        );
        kafkaService.sendCalculatedRate(calculatedRate);
    }



    private void calculateAndSaveRatesDependentOnUsdTry(String updatedRateName) {
        log.info("RateManagerImpl: Attempting calculation for '{}' dependent on USD/TRY.", updatedRateName);

        List<Rate> existsRates = redisService.getAllRawRatesByRateName(updatedRateName);
        List<Rate> existsUsdTryRates = redisService.getAllRawRatesByRateName("USDTRY");

        if (existsRates.isEmpty() || existsUsdTryRates.isEmpty()) {
            log.warn("RateManagerImpl: Missing required rates for calculation. RateName: {}, USDTRY available: {}",
                    updatedRateName, !existsUsdTryRates.isEmpty());
            return;
        }

        List<String> cachedBids = existsRates.stream().map(rate -> rate.getBid().toString()).toList();
        List<String> cachedAsks = existsRates.stream().map(rate -> rate.getAsk().toString()).toList();

        List<String> cachedUsdTryBids = existsUsdTryRates.stream().map(rate -> rate.getBid().toPlainString()).toList();
        List<String> cachedUsdTryAsks = existsUsdTryRates.stream().map(rate -> rate.getAsk().toPlainString()).toList();

        BigDecimal usdTryMid = calculationService.calculateUsdTryMidValue(
                cachedUsdTryBids,
                cachedUsdTryAsks
        );

        CalculatedRate calculatedRate = calculationService.calculateRateDependentOnUsdTry(
                updatedRateName,
                usdTryMid.toPlainString(),
                cachedBids,
                cachedAsks
        );
        log.debug("RateManagerImpl: Calculated dependent rate: {}", calculatedRate);

        redisService.saveCalculatedRate(
                updatedRateName,
                calculatedRate
        );
        kafkaService.sendCalculatedRate(calculatedRate);
    }


}
