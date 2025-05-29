package com.toyota.calculation;

import com.toyota.calculation.Impl.PythonCalculator;
import com.toyota.entity.CalculatedRate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;


class PythonCalculatorTest {

    private CalculationService calculationService;


    @BeforeEach
    void setUp(){
        calculationService = new PythonCalculator();
    }


    @Test
    void whenInComingRateIsValid_ThenReturnTrue(){

        List<String> cachedBids = List.of("99.0", "100.0");
        List<String> cachedAsks = List.of("100.0", "101.0");
        String incomingBid = "100.0";
        String incomingAsk = "101.0";

        boolean isValid = calculationService.isInComingRateValid(
                incomingBid,
                incomingAsk,
                cachedBids,
                cachedAsks
        );

        Assertions.assertTrue(isValid, "Incoming rate should be valid as it is within 1% of the reference mid.");

    }


    @Test
    void whenInComingRateIsInvalid_ThenReturnFalse(){

        List<String> cachedBids = List.of("99.0", "100.0");
        List<String> cachedAsks = List.of("100.0", "101.0");
        String incomingBid = "101.0";
        String incomingAsk = "102.0";

        boolean isValid = calculationService.isInComingRateValid(
                incomingBid,
                incomingAsk,
                cachedBids,
                cachedAsks
        );

        Assertions.assertFalse(isValid);

    }

    @Test
    void testCalculateUsdTry_Success(){
        List<String> cachedUsdTryBids = List.of("32.0", "32.2");
        List<String> cachedUsdTryAsks = List.of("32.4", "32.6");

        BigDecimal expectedBid = new BigDecimal("32.1");
        BigDecimal expectedAsk = new BigDecimal("32.5");

        CalculatedRate rate = calculationService.calculateUsdTry(
                cachedUsdTryBids,
                cachedUsdTryAsks
        );

        Assertions.assertNotNull(rate, "CalculatedRate should not be null.");
        Assertions.assertEquals("USDTRY", rate.getName());
        Assertions.assertEquals(expectedBid, rate.getBid());
        Assertions.assertEquals(expectedAsk, rate.getAsk());
        Assertions.assertNotNull(rate.getTimestamp());

    }

    @Test
    void testCalculateRateDependentOnUsdTry_Success() {

        String rateName = "EURTRY";
        String usdMid = "32.0";
        List<String> cachedBids = List.of("1.1", "1.12");
        List<String> cachedAsks = List.of("1.13", "1.15");

        BigDecimal expectedBid = new BigDecimal("35.520");
        BigDecimal expectedAsk = new BigDecimal("36.480");

        CalculatedRate rate = calculationService.calculateRateDependentOnUsdTry(
                rateName,
                usdMid,
                cachedBids,
                cachedAsks
        );

        Assertions.assertNotNull(rate, "CalculatedRate should not be null.");
        Assertions.assertEquals(rateName, rate.getName());
        Assertions.assertEquals(expectedBid,rate.getBid());
        Assertions.assertEquals(expectedAsk, rate.getAsk());
        Assertions.assertNotNull(rate.getTimestamp());
    }


    @Test
    void testCalculateUsdTryMidValue_Success(){

        List<String> cachedUsdTryBids = List.of("32.0", "32.2");
        List<String> cachedUsdTryAsks = List.of("32.4", "32.6");

        BigDecimal expectedMidValue = new BigDecimal("32.3");

        BigDecimal calculatedMidValue = calculationService.calculateUsdTryMidValue(
                cachedUsdTryBids,
                cachedUsdTryAsks
        );

        Assertions.assertNotNull(calculatedMidValue);
        Assertions.assertEquals(expectedMidValue,calculatedMidValue);

    }




}