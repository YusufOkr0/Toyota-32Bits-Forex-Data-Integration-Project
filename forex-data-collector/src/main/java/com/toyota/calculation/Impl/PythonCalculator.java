package com.toyota.calculation.Impl;

import com.toyota.calculation.CalculationService;
import com.toyota.exception.ConfigFileLoadingException;
import com.toyota.exception.ConfigFileNotFoundException;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.*;
import java.util.List;

public class PythonCalculator implements CalculationService {

    private static final String LANGUAGE_NAME = "python";
    private static final String FORMULA_FILE = "scripts/formulas.py";

    private final ThreadLocal<Context> contextHolder = ThreadLocal.withInitial(this::createContext);

    public PythonCalculator() {
    }


    public boolean isInComingRateValid(String inComingBid, String inComingAsk, List<String> cachedBids, List<String> cachedAsks) {
        try{
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

            return result.asBoolean();


        } catch (Exception e) {
            System.out.printf("Exception. %s \n" ,e.getMessage());
        }

        return false;
    }


    private Context createContext() {
        try (InputStream scriptFile = PythonCalculator.class.getClassLoader().getResourceAsStream(FORMULA_FILE);
             InputStreamReader reader = (scriptFile != null) ? new InputStreamReader(scriptFile) : null) {

            if (reader == null) {
                throw new ConfigFileNotFoundException("Formula file cannot found in the classpath: " + FORMULA_FILE);
            }

            Source source = Source.newBuilder(
                            LANGUAGE_NAME,
                            reader,
                            FORMULA_FILE)
                    .build();

            Context context = Context.newBuilder("python")
                    .option("engine.WarnInterpreterOnly", "false")  // To ignore the
                    .allowAllAccess(true)
                    .build();

            context.eval(source);

            return context;
        } catch (Exception e) {
            throw new ConfigFileLoadingException("Error while loading Python file: " + e.getMessage());
        }
    }


}
