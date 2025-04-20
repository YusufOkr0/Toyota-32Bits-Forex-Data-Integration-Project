package com.toyota.calculation.Impl;

import com.toyota.calculation.CalculationService;
import com.toyota.entity.CalculatedRate;
import com.toyota.exception.ConfigFileLoadingException;
import com.toyota.exception.ConfigFileNotFoundException;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PythonCalculator implements CalculationService {

    private static final String LANGUAGE_NAME = "python";
    private static final String FORMULA_FILE = "scripts/formulas.py";

    private Source source;
    private final ThreadLocal<Context> contextHolder = ThreadLocal.withInitial(this::createContext);

    public PythonCalculator() {
        loadTheSourceCode();
    }

    public boolean isInComingRateValid(String inComingBid, String inComingAsk, List<String> cachedBids, List<String> cachedAsks) {
        try {
            System.out.println("function is called");
            System.out.println(System.currentTimeMillis());
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
            System.out.println("function is end");
            System.out.println(System.currentTimeMillis());
            return result.asBoolean();


        } catch (Exception e) {
            System.err.printf("Exception. %s \n", e.getMessage());
        }

        return false;
    }

    public CalculatedRate calculateUsdTry(List<String> cachedBids, List<String> cachedAsks) {
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

            return new CalculatedRate(
                    "USDTRY",
                    bid,
                    ask,
                    LocalDateTime.now()
            );

        } catch (Exception e) {
            System.err.printf("Exception in calculateUsdTry. %s \n", e.getMessage());
        }

        System.err.println("CALCULATE NULL DÖNÜYOR");
        return null;
    }

    @Override
    public CalculatedRate calculateRateDependentOnUsdTry(String rateName, String usdMid, List<String> cachedBids, List<String> cachedAsk) {
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

            return new CalculatedRate(
                    rateName,
                    bid,
                    ask,
                    LocalDateTime.now()
            );

        } catch (Exception e) {
            System.err.printf("Exception in calculateRateDependentOnUsdTry. %s \n", e.getMessage());
        }
        return null;
    }

    @Override
    public BigDecimal calculateUsdTryMidValue(List<String> cachedUsdTryBids, List<String> cachedUsdTryAsks) {
        try {
            Context context = contextHolder.get();

            Value function = context
                    .getBindings(LANGUAGE_NAME)
                    .getMember("calculate_usd_try_mid_value");

            Value result = function.execute(
                    cachedUsdTryBids,
                    cachedUsdTryAsks
            );
            String usd_try_mid = result.asString();

            return new BigDecimal(usd_try_mid);

        } catch (Exception e) {
            System.err.printf("Exception in calculateUsdTryMidValue. %s \n", e.getMessage());
        }
        return null;
    }


    private Context createContext() {
        Context context = Context.newBuilder(LANGUAGE_NAME)
                .option("engine.WarnInterpreterOnly", "false")
                .allowAllAccess(true)
                .build();
        context.eval(source);
        return context;
    }

    private void loadTheSourceCode() {
        try (InputStream scriptFile = PythonCalculator.class.getClassLoader().getResourceAsStream(FORMULA_FILE);
             InputStreamReader reader = (scriptFile != null) ? new InputStreamReader(scriptFile) : null) {

            if (reader == null) {
                throw new ConfigFileNotFoundException("Formula file cannot found in the classpath: " + FORMULA_FILE);
            }

            source = Source.newBuilder(
                    LANGUAGE_NAME,
                    reader,
                    "python"
            ).build();


        } catch (IOException e) {
            throw new ConfigFileLoadingException("Error while loading Python file: " + e.getMessage());
        }
    }


    public ThreadLocal<Context> getContextHolder() {
        return this.contextHolder;
    }


}