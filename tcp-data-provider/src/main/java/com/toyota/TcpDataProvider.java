package com.toyota;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.auth.AuthService;
import com.toyota.broadcast.FxDataPublisher;
import com.toyota.config.ConfigUtil;
import com.toyota.config.RateInfo;
import com.toyota.entity.Rate;
import com.toyota.server.FxDataServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class TcpDataProvider {

    private static final Logger logger = LogManager.getLogger(TcpDataProvider.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String INITIAL_RATES_EXTERNAL_CONFIG_PATH = "/conf/initial-rates.json";

    public static void main(String[] args) {

        final ConfigUtil config = new ConfigUtil();

        final int SERVER_PORT = config.getIntValue("server.port");
        final int PUBLISH_FREQUENCY = config.getIntValue("publish.frequency");
        final int SPIKE_INTERVAL = config.getIntValue("spike.interval");
        final BigDecimal SPIKE_PERCENTAGE = config.getBigDecimalValue("spike.percentage");
        final BigDecimal MINIMUM_RATE_CHANGE = config.getBigDecimalValue("minimum.rate.change");
        final BigDecimal MAXIMUM_RATE_CHANGE = config.getBigDecimalValue("maximum.rate.change");
        final List<String> USER_CREDENTIALS_LIST = Arrays.stream(config.getStringValue("user.credentials").split(",")).toList();





        final List<RateInfo> initialRatesFromJson = loadInitialRates();

        if (initialRatesFromJson == null || initialRatesFromJson.isEmpty()) {
            logger.error("Application start failed. Initial rate data could not be loaded. Exiting...");
            throw new RuntimeException("Initial rates could not be loaded. Please check the config file.");
        }



        final List<Rate> INITIAL_RATES = new ArrayList<>();
        final List<String> CURRENCY_PAIRS = new ArrayList<>();

        for (RateInfo rateInfo : initialRatesFromJson) {
            try {
                Rate rate = new Rate(
                        rateInfo.getRateName(),
                        rateInfo.getBid(),
                        rateInfo.getAsk(),
                        LocalDateTime.now(),
                        rateInfo.getMinLimit(),
                        rateInfo.getMaxLimit()
                );
                INITIAL_RATES.add(rate);
                CURRENCY_PAIRS.add(rateInfo.getRateName());
            } catch (RuntimeException e) {
                logger.error("Unexpected error loading initial rate for '{}': {}", rateInfo.getRateName(), e.getMessage(), e);
            }
        }


        final ConcurrentHashMap<String, Set<SocketChannel>> SUBSCRIPTIONS = new ConcurrentHashMap<>();
        for (String currencyPair : CURRENCY_PAIRS) {
            SUBSCRIPTIONS.put(
                    currencyPair,
                    ConcurrentHashMap.newKeySet()
            );
        }


        final Map<String, String> AUTH_REPOSITORY = USER_CREDENTIALS_LIST.stream()
                .map(nameAndPassword -> nameAndPassword.split("\\|"))
                .filter(credentials -> credentials.length == 2)
                .collect(Collectors.toMap(
                        credentials -> credentials[0],
                        credentials -> credentials[1]
                ));

        final AuthService AUTH_SERVICE = new AuthService(AUTH_REPOSITORY);





        final ExecutorService executorService = Executors.newFixedThreadPool(2);
        final FxDataServer fxDataServer = new FxDataServer(
                SERVER_PORT,
                CURRENCY_PAIRS,
                SUBSCRIPTIONS,
                AUTH_SERVICE
        );

        final FxDataPublisher publisher = new FxDataPublisher(
                SUBSCRIPTIONS,
                INITIAL_RATES,
                PUBLISH_FREQUENCY,
                SPIKE_INTERVAL,
                SPIKE_PERCENTAGE,
                MINIMUM_RATE_CHANGE,
                MAXIMUM_RATE_CHANGE
        );


        logStartupInfo(
                SERVER_PORT, PUBLISH_FREQUENCY, CURRENCY_PAIRS, AUTH_REPOSITORY, INITIAL_RATES,
                SPIKE_INTERVAL, SPIKE_PERCENTAGE, MINIMUM_RATE_CHANGE, MAXIMUM_RATE_CHANGE
        );


        executorService.execute(fxDataServer::startServer);
        executorService.execute(publisher::startBroadcast);

    }



    private static List<RateInfo> loadInitialRates() {
        try {
            File jsonFile = new File(INITIAL_RATES_EXTERNAL_CONFIG_PATH);              // LOOK UP ENV FOR JSON FILE. IF EXISTS THEN CREATE

            logger.info("Reading initial-rates.json from file: {}", jsonFile.getAbsolutePath());
            return MAPPER.readValue(jsonFile, new TypeReference<List<RateInfo>>() {});

        } catch (IOException e) {
            logger.warn("Failed to read initial-rates.json from {}: {}", INITIAL_RATES_EXTERNAL_CONFIG_PATH, e.getMessage());
            // ANY EXCEPTION IN EXTERNAL FILE, THEN KEEP GOING WITH DEFAULT ONE
        }


        try (InputStream jsonFileStream = TcpDataProvider.class
                .getClassLoader()
                .getResourceAsStream("initial-rates.json")
        ) {
            if (jsonFileStream == null) {
                logger.error("initial-rates.json not found in resources");
                return null;
            }
            logger.info("Reading default initial-rates.json file from classpath.");
            return MAPPER.readValue(jsonFileStream, new TypeReference<List<RateInfo>>() {});
        } catch (IOException e) {
            logger.error("Failed to read initial-rates.json from resources: {}", e.getMessage(), e);
            return null;
        }
    }



    private static void logStartupInfo(int serverPort, int publishFrequency, List<String> currencyPairs,
                                       Map<String, String> authRepository, List<Rate> initialRates,
                                       int spikeInterval, BigDecimal spikePercentage, BigDecimal minRateChange, BigDecimal maxRateChange) {

        logger.info("============================================================");
        logger.info("             TCP FX DATA PROVIDER - SERVER STARTING         ");
        logger.info("============================================================");
        logger.info("------------------------------------------------------------");

        logger.info(">> Server Configuration");
        logger.info(" - Listening Port           : {}", serverPort);
        logger.info(" - Broadcast Frequency (ms) : {}", publishFrequency);

        logger.info(">> Currency Pairs Supported [{}]:", currencyPairs.size());
        currencyPairs.forEach(pair -> logger.info("   • {}", pair));

        logger.info(">> Available Usernames [{}]:", authRepository.size());
        authRepository.keySet().forEach(username -> logger.info("   • {}", username));

        logger.info(">> Initial Rate Data [{}]:", initialRates.size());
        initialRates.forEach(rate -> logger.info("   • {} | Bid: {} | Ask: {} | Limits: [{} - {}]",
                rate.getRateName(),
                rate.getBid(),
                rate.getAsk(),
                rate.getMinLimit(),
                rate.getMaxLimit()));

        logger.info(">> Volatility & Spike Configuration");
        logger.info(" - Spike Interval            : {}", spikeInterval);
        logger.info(" - Spike Percentage          : {}", spikePercentage);
        logger.info(" - Min Rate Change           : {}", minRateChange);
        logger.info(" - Max Rate Change           : {}", maxRateChange);
    }
}