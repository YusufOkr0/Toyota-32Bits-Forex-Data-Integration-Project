package com.toyota.calculation.Impl;

import com.toyota.calculation.CalculationService;
import com.toyota.entity.CalculatedRate;
import com.toyota.exception.ConfigFileLoadingException;
import com.toyota.exception.ConfigFileNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PythonCalculator implements CalculationService {

    private static final Logger logger = LogManager.getLogger(PythonCalculator.class);

    private static final String LANGUAGE_NAME = "python";
    private static final String FORMULA_FILE = "scripts/formulas.py";

    private Source source;
    private final ThreadLocal<Context> contextHolder = ThreadLocal.withInitial(this::createContext);

    public PythonCalculator() {
        loadTheSourceCode();
    }

    public boolean isInComingRateValid(String inComingBid, String inComingAsk, List<String> cachedBids, List<String> cachedAsks) {
        logger.debug("isInComingRateValid: Validating incoming rate. Incoming bid: {}, ask: {} | Cached bids size: {}, asks size: {}",
                inComingBid, inComingAsk, cachedBids.size(), cachedAsks.size());
        try {
            Context context = contextHolder.get();

            Value function = context
                    .getBindings(LANGUAGE_NAME)
                    .getMember("is_rate_valid");

            Value result = function.execute(
                    inComingBid,
                    inComingAsk,
                    cachedBids,
                    cachedAsks
            );

            boolean validationResult = result.asBoolean();
            logger.debug("isInComingRateValid: Validation result: {}", validationResult);

            return validationResult;
        } catch (Exception e) {
            logger.error("isInComingRateValid: Error during rate validation. Details: {}", e.getMessage(), e);
        }
        return false;
    }

    public CalculatedRate calculateUsdTry(List<String> cachedBids, List<String> cachedAsks) {
        logger.debug("calculateUsdTry: Starting USD/TRY calculation. Cached bids: {}, Cached asks: {}",
                cachedBids.size(), cachedAsks.size());
        try {
            Context context = contextHolder.get();

            Value function = context
                    .getBindings(LANGUAGE_NAME)
                    .getMember("calculate_usd_try");

            Value result = function.execute(
                    cachedBids,
                    cachedAsks
            );


            String usd_try_bid = result.getArrayElement(0).asString();
            String usd_try_ask = result.getArrayElement(1).asString();

            BigDecimal bid = new BigDecimal(usd_try_bid);
            BigDecimal ask = new BigDecimal(usd_try_ask);

            logger.debug("calculateUsdTry: USD/TRY calculated successfully. Bid: {}, Ask: {}", bid, ask);

            return new CalculatedRate(
                    "USDTRY",
                    bid,
                    ask,
                    LocalDateTime.now()
            );

        } catch (Exception e) {
            logger.error("calculateUsdTry: Failed to calculate USD/TRY. Details: {}", e.getMessage(), e);
            logger.warn("calculateUsdTry: USD/TRY calculation returning null due to calculation error!");
            return null;
        }
    }

    @Override
    public CalculatedRate calculateRateDependentOnUsdTry(String rateName, String usdMid, List<String> cachedBids, List<String> cachedAsk) {
        logger.debug("calculateRateDependentOnUsdTry: Calculating rate dependent on USD/TRY. Rate name: {}, USD mid: {}", rateName, usdMid);
        try {
            Context context = contextHolder.get();

            Value function = context
                    .getBindings(LANGUAGE_NAME)
                    .getMember("calculate_rate_dependent_on_usd_try");

            Value result = function.execute(
                    usdMid,
                    cachedBids,
                    cachedAsk
            );

            String rate_bid = result.getArrayElement(0).asString();
            String rate_ask = result.getArrayElement(1).asString();

            BigDecimal bid = new BigDecimal(rate_bid);
            BigDecimal ask = new BigDecimal(rate_ask);

            logger.debug("calculateRateDependentOnUsdTry: Dependent rate calculated successfully. Bid: {}, Ask: {}", bid, ask);

            return buildCalculatedRate(rateName,bid,ask);

        } catch (Exception e) {
            logger.error("calculateRateDependentOnUsdTry: Failed to calculate rate dependent on USD/TRY. Details: {}", e.getMessage(), e);
            logger.warn("calculateRateDependentOnUsdTry: Derived Rate returning null due to calculation error!");
            return null;
        }
    }

    @Override
    public BigDecimal calculateUsdTryMidValue(List<String> cachedUsdTryBids, List<String> cachedUsdTryAsks) {
        logger.debug("calculateUsdTryMidValue: Calculating USD/TRY mid value. Cached bids: {}, Cached asks: {}",
                cachedUsdTryBids.size(), cachedUsdTryAsks.size());
        try {
            Context context = contextHolder.get();

            Value function = context
                    .getBindings(LANGUAGE_NAME)
                    .getMember("calculate_usd_try_mid_value");

            Value result = function.execute(
                    cachedUsdTryBids,
                    cachedUsdTryAsks
            );
            String midValue = result.asString();

            logger.debug("calculateUsdTryMidValue: USD/TRY mid value calculated: {}", midValue);
            return new BigDecimal(midValue);

        } catch (Exception e) {
            logger.error("calculateUsdTryMidValue: Failed to calculate USD/TRY mid value. Details: {}", e.getMessage(), e);
            logger.warn("calculateUsdTryMidValue: USD/TRY mid value returning null due to calculation error!");
            return null;
        }
    }


    private CalculatedRate buildCalculatedRate(String rateName,BigDecimal bid,BigDecimal ask){

        String derivedRateName = switch (rateName) {
            case "EURUSD" -> "EURTRY";
            case "GBPUSD" -> "GBPTRY";
            default -> "unknown_rate";
        };

        logger.debug("buildCalculatedRate: Building CalculatedRate. Base rate: {}, Derived rate: {}, Bid: {}, Ask: {}",
                rateName, derivedRateName, bid, ask);

        return new CalculatedRate(
                derivedRateName,
                bid,
                ask,
                LocalDateTime.now()
        );
    }




    private Context createContext() {
        logger.trace("Creating new GraalVM context for Python execution.");

        Context context = Context.newBuilder(LANGUAGE_NAME)
                .allowAllAccess(true)
                .build();
        context.eval(source);

        logger.trace("Python source code evaluated successfully.");
        return context;
    }

    private void loadTheSourceCode() {
        logger.info("loadTheSourceCode: Loading Python source code from file: {}", FORMULA_FILE);

        try (InputStream scriptFile = PythonCalculator.class.getClassLoader().getResourceAsStream(FORMULA_FILE);
             InputStreamReader reader = (scriptFile != null) ? new InputStreamReader(scriptFile) : null) {

            if (reader == null) {
                logger.error("loadTheSourceCode: Formula file '{}' not found in classpath.", FORMULA_FILE);
                throw new ConfigFileNotFoundException("Formula file cannot found in the classpath: " + FORMULA_FILE);
            }

            source = Source.newBuilder(
                    LANGUAGE_NAME,
                    reader,
                    "python"
            ).build();

            logger.trace("loadTheSourceCode: Python source code loaded and built successfully.");

        } catch (IOException e) {
            logger.error("loadTheSourceCode: I/O Exception while loading Python source file: {}", e.getMessage(), e);
            throw new ConfigFileLoadingException("Error while loading Python file: " + e.getMessage());
        }
    }


    public ThreadLocal<Context> getContextHolder() {
        return this.contextHolder;
    }


}