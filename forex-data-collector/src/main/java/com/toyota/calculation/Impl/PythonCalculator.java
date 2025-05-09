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

    public PythonCalculator() {
        loadTheSourceCode();
    }

    public boolean isInComingRateValid(String inComingBid, String inComingAsk, List<String> cachedBids, List<String> cachedAsks) {
        try (Context context = createContext()) {
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
            logger.debug("PythonCalculator: Validation result for incoming rate: {}", validationResult);

            return validationResult;
        } catch (Exception e) {
            logger.error("PythonCalculator: Error during rate validation. Validation result returning false. Details: {}", e.getMessage(), e);
        }
        return false;
    }

    public CalculatedRate calculateUsdTry(List<String> cachedBids, List<String> cachedAsks) {
        try (Context context = createContext()) {
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

            logger.debug("PythonCalculator: USD/TRY calculated successfully. Bid: {}, Ask: {}", bid, ask);

            return new CalculatedRate(
                    "USDTRY",
                    bid,
                    ask,
                    LocalDateTime.now()
            );

        } catch (Exception e) {
            logger.error("PythonCalculator: Failed to calculate USD/TRY." +
                    " USD/TRY calculation returning null due to calculation error!. Details: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public CalculatedRate calculateRateDependentOnUsdTry(String rateName, String usdMid, List<String> cachedBids, List<String> cachedAsks) {
        try (Context context = createContext()) {
            Value function = context
                    .getBindings(LANGUAGE_NAME)
                    .getMember("calculate_rate_dependent_on_usd_try");

            Value result = function.execute(
                    usdMid,
                    cachedBids,
                    cachedAsks
            );

            String rate_bid = result.getArrayElement(0).asString();
            String rate_ask = result.getArrayElement(1).asString();

            BigDecimal bid = new BigDecimal(rate_bid);
            BigDecimal ask = new BigDecimal(rate_ask);

            logger.debug("PythonCalculator: Dependent rate: {} calculated successfully. Bid: {}, Ask: {}", rateName, bid, ask);

            return new CalculatedRate(
                    rateName,
                    bid,
                    ask,
                    LocalDateTime.now()
            );

        } catch (Exception e) {
            logger.error("PythonCalculator: Failed to calculate rate dependent on USD/TRY. " +
                    "Derived Rate returning null due to calculation error!. Details: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public BigDecimal calculateUsdTryMidValue(List<String> cachedUsdTryBids, List<String> cachedUsdTryAsks) {
        try (Context context = createContext()) {
            Value function = context
                    .getBindings(LANGUAGE_NAME)
                    .getMember("calculate_usd_try_mid_value");

            Value result = function.execute(
                    cachedUsdTryBids,
                    cachedUsdTryAsks
            );
            String midValue = result.asString();

            logger.debug("PythonCalculator: USD/TRY mid value calculated: {}", midValue);
            return new BigDecimal(midValue);

        } catch (Exception e) {
            logger.error("PythonCalculator: Failed to calculate USD/TRY mid value. " +
                    "USD/TRY mid value returning null due to calculation error!. Details: {}", e.getMessage(), e);
            return null;
        }
    }


    private Context createContext() {
        Context context = Context.newBuilder(LANGUAGE_NAME)
                .allowAllAccess(true)
                .build();
        context.eval(source);
        return context;
    }

    private void loadTheSourceCode() {
        try (InputStream scriptFile = PythonCalculator.class.getClassLoader().getResourceAsStream(FORMULA_FILE);
             InputStreamReader reader = (scriptFile != null) ? new InputStreamReader(scriptFile) : null) {

            if (reader == null) {
                logger.error("PythonCalculator: Formula file '{}' not found in classpath.", FORMULA_FILE);
                throw new ConfigFileNotFoundException("Formula file cannot found in the classpath: " + FORMULA_FILE);
            }

            source = Source.newBuilder(
                    LANGUAGE_NAME,
                    reader,
                    "python"
            ).build();

            logger.trace("PythonCalculator: Python source code loaded and built successfully.");
        } catch (IOException e) {
            logger.error("PythonCalculator: I/O Exception while loading Python source file: {}", e.getMessage(), e);
            throw new ConfigFileLoadingException("Error while loading Python file: " + e.getMessage());
        }
    }
}
