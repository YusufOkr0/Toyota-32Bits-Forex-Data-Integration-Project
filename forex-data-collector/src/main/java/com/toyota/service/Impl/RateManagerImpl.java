package com.toyota.service.Impl;

import com.toyota.cache.CacheService;
import com.toyota.calculation.CalculationService;
import com.toyota.entity.CalculatedRate;
import com.toyota.entity.Rate;
import com.toyota.service.RateManager;

import java.math.BigDecimal;
import java.util.List;


public class RateManagerImpl implements RateManager {

    private final CacheService redisService;
    private final CalculationService calculationService;

    public RateManagerImpl(CacheService redisService, CalculationService calculationService) {
        this.redisService = redisService;
        this.calculationService = calculationService;
    }

    public void warmUpCalculationService() {
        calculationService.getContextHolder().get();
    }

    public void handleFirstInComingRate(String platformName, String rateName, Rate inComingRate) {
        redisService.saveRawRate(
                platformName,
                rateName,
                inComingRate
        );
    }

    public void handleRateUpdate(String platformName, String rateName, Rate inComingRate) {

        List<Rate> existsRates = redisService.getAllRawRatesByRateName(rateName);     // TÜM PLATFORMLARDAN USD TRY ALINDI.

        if (existsRates.isEmpty()) {
            // EXCHANGE RATES MUST BE AVAILABLE IN REDIS.
            return;
        }
        List<String> cachedBids = existsRates
                .stream()
                .map(rate -> rate.getBid().toPlainString())
                .toList();                                      // BID VE ASK DEGERLERI AYRI OLARAK ALINDI.
        List<String> cachedAsks = existsRates
                .stream()
                .map(rate -> rate.getAsk().toPlainString())
                .toList();

        String newBid = inComingRate.getBid().toPlainString();
        String newAsk = inComingRate.getAsk().toPlainString();


        if (calculationService.isInComingRateValid(newBid, newAsk, cachedBids, cachedAsks)) {

            redisService.saveRawRate(platformName, rateName, inComingRate); // VERILER GÜNCELLENDI.

            if (rateName.equals("USDTRY")) {
                calculateAndSaveUsdTry();  // CHECK LATER.  USDTRY UPDATE OLUNCA DIGER KURLARI UPDATE ETMELI MIYIM??
            } else {
                calculateAndSaveRatesDependentOnUsdTry(rateName);
            }

        } else {
            System.out.printf("RATE IS INVALID: %s\n", inComingRate);
        }

    }


    private void calculateAndSaveUsdTry() {
        final String RATE_NAME = "USDTRY";

        List<Rate> cachedRates = redisService.getAllRawRatesByRateName(RATE_NAME);  // tüm usd try entrylerini redisten al

        if (cachedRates.isEmpty()) {
            // NO USD/TRY IN THE CACHE
            return;
        }
        // BID VE ASK DEGERLERINI AYIR. ( TÜM PLATFORMLARDAN VERI GELMEMIS DE OLABILIR BU DURUMDA)
        List<String> cachedUsdTryBids = cachedRates.stream().map(rate -> rate.getBid().toPlainString()).toList();
        List<String> cachedUsdTryAsks = cachedRates.stream().map(rate -> rate.getAsk().toPlainString()).toList();

        CalculatedRate calculatedRate = calculationService.calculateUsdTry(
                cachedUsdTryBids,
                cachedUsdTryAsks
        );
        redisService.saveCalculatedRate(
                RATE_NAME,
                calculatedRate
        );
    }


    /**
     * GELEN VERI ICIN REDISTEN TÜM RATELERI AL.
     * SADECE BIR PLATFORMDA VERI VAR ISE NE OLACAK???
     * SADECE RESTDEN GELDI DIYELIM.
     * REDISTEN PLATFORM BAZINDA KAC TANE VERI ALDIGIM FARKETMEKSIZIN ALDIGIM TÜM VERILERIN BID VE ASK DEGERLERINI AYIRIYORUM.
     * EGER TEK BI PLATFORMDAN VERI GELMIS ISE SADECE BUNUN BID VE ASK DEGERLERI ELIMDE OLUYOR.
     * BID VE ASKLERIN AYRI AYRI ORTALAMALARINI ALIP BUNU CALCULATED__[RATENAME] OLARAK ALMALIYIM.
     * YANI REDISTE TEK BIR PLATFROMDAN VERI GELMIS OLSA BILE ELIMDE OLAN BID VE ASK DEGERLERINI AYRI LISTELER SEKLINDE
     * CALCULATION SERVISE VERIP ORTLAMALARINI ALIP
     * ( BU DURUMDA EGER TEK BIR PLATFORMDA VERI VAR ISE HESAPLAMA SONUCU DIREK O VERIYI TEKRAR BANA VERECEK.)
     * HESAPLANAN YENI BID VE ASK DEGERLERI ILE YENI BIR KUR HESAPLAYIP DÖNECEK.
     *
     * @param updatedRateName
     */
    private void calculateAndSaveRatesDependentOnUsdTry(String updatedRateName) {

        List<Rate> existsRates = redisService.getAllRawRatesByRateName(updatedRateName);     // TÜM PLATFORMLARDAN EUR/USD VEYA GBP/USD ALINDI.
        List<Rate> existsUsdTryRates = redisService.getAllRawRatesByRateName("USDTRY");

        if (existsRates.isEmpty() || existsUsdTryRates.isEmpty()) {
            // ALL NECESSARY EXCHANGE RATES MUST BE AVAILABLE IN REDIS.
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

        redisService.saveCalculatedRate(
                updatedRateName,
                calculatedRate
        );
    }


}
