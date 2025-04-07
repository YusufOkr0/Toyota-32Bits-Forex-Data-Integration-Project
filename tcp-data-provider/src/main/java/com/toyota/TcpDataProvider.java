package com.toyota;

import com.toyota.auth.AuthService;
import com.toyota.broadcast.FxDataPublisher;
import com.toyota.config.ConfigUtil;
import com.toyota.entity.Rate;
import com.toyota.server.FxDataServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.math.BigDecimal;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpDataProvider {

    private static final Logger logger = LogManager.getLogger(TcpDataProvider.class);

    public static void main(String[] args) {

        final ExecutorService executorService = Executors.newFixedThreadPool(2);

        final ConfigUtil config = new ConfigUtil();

        final int SERVER_PORT = config.getIntValue("server.port");
        final int PUBLISH_FREQUENCY = config.getIntValue("publish.frequency");
        final List<String> CURRENCY_PAIRS = Arrays.stream(config.getStringValue("currency.pairs").split(",")).toList();
        final List<String> USER_CREDENTIALS = Arrays.stream(config.getStringValue("user.credentials").split(",")).toList();


        final ConcurrentHashMap<String, Set<SocketChannel>> SUBSCRIPTIONS = new ConcurrentHashMap<>();
        for (String currencyPair : CURRENCY_PAIRS) {
            SUBSCRIPTIONS.put(currencyPair, ConcurrentHashMap.newKeySet());
        }


        final Map<String, String> AUTH_REPOSITORY = new HashMap<>();

        for (String nameAndPassword : USER_CREDENTIALS) {
            String[] credentials = nameAndPassword.split("\\|");
            AUTH_REPOSITORY.put(credentials[0], credentials[1]);
        }

        final AuthService AUTH_SERVICE = new AuthService(AUTH_REPOSITORY);



        final List<Rate> INITIAL_RATES = new ArrayList<>();

        for (String currencyPair : CURRENCY_PAIRS) {
            try{
                String lowerCasePair = currencyPair.toLowerCase();
                String bidKey = lowerCasePair + ".bid";
                String askKey = lowerCasePair + ".ask";
                String minLimitKey = lowerCasePair + ".min.limit";
                String maxLimitKey = lowerCasePair + ".max.limit";

                BigDecimal bid = new BigDecimal(config.getStringValue(bidKey));
                BigDecimal ask = new BigDecimal(config.getStringValue(askKey));
                BigDecimal minLimit = new BigDecimal(config.getStringValue(minLimitKey));
                BigDecimal maxLimit = new BigDecimal(config.getStringValue(maxLimitKey));

                Rate rate = new Rate(currencyPair, bid, ask, LocalDateTime.now(), minLimit, maxLimit);
                INITIAL_RATES.add(rate);

            }catch (RuntimeException e){
                System.out.println("ERROR: Unexpected error loading initial rate for '" + currencyPair + "': " + e.getMessage());
            }
        }


        final int SPIKE_INTERVAL = config.getIntValue("spike.interval");
        final BigDecimal SPIKE_PERCENTAGE = new BigDecimal(config.getStringValue("spike.percentage"));
        final BigDecimal MINIMUM_RATE_CHANGE = new BigDecimal(config.getStringValue("minimum.rate.change"));
        final BigDecimal MAXIMUM_RATE_CHANGE = new BigDecimal(config.getStringValue("maximum.rate.change"));



        FxDataServer fxDataServer = new FxDataServer(
                SERVER_PORT,
                CURRENCY_PAIRS,
                SUBSCRIPTIONS,
                AUTH_SERVICE
        );

        FxDataPublisher publisher = new FxDataPublisher(
                SUBSCRIPTIONS,
                INITIAL_RATES,
                PUBLISH_FREQUENCY,
                SPIKE_INTERVAL,
                SPIKE_PERCENTAGE,
                MINIMUM_RATE_CHANGE,
                MAXIMUM_RATE_CHANGE
        );

        executorService.submit(fxDataServer::startServer);
        executorService.submit(publisher::startBroadcast);

        logger.info("============================================================");
        logger.info("             TCP FX DATA PROVIDER - BOOTING UP              ");
        logger.info("============================================================");
        logger.info("------------------------------------------------------------");

        logger.info(">> Server Configuration");
        logger.info(" - Listening Port           : {}", SERVER_PORT);
        logger.info(" - Broadcast Frequency (ms) : {}", PUBLISH_FREQUENCY);

        logger.info(">> Currency Pairs Supported [{}]:", CURRENCY_PAIRS.size());
        for (String pair : CURRENCY_PAIRS) {
            logger.info("   • {}", pair);
        }

        logger.info(">> Authorized Users [{}]:", AUTH_REPOSITORY.size());
        for (String username : AUTH_REPOSITORY.keySet()) {
            logger.info("   • {}", username);
        }

        logger.info(">> Initial Rate Data [{}]:", INITIAL_RATES.size());
        for (Rate rate : INITIAL_RATES) {
            logger.info("   • {} | Bid: {} | Ask: {} | Limits: [{} - {}]",
                    rate.getRateName(),
                    rate.getBid(),
                    rate.getAsk(),
                    rate.getMinLimit(),
                    rate.getMaxLimit());
        }

        logger.info(">> Volatility & Spike Configuration");
        logger.info(" - Spike Interval            : {}", SPIKE_INTERVAL);
        logger.info(" - Spike Percentage          : {}", SPIKE_PERCENTAGE);
        logger.info(" - Min Rate Change           : {}", MINIMUM_RATE_CHANGE);
        logger.info(" - Max Rate Change           : {}", MAXIMUM_RATE_CHANGE);

        logger.info(">> Background Services");
        logger.info(" - FxDataServer              : READY");
        logger.info(" - FxDataPublisher           : READY");

        logger.info("------------------------------------------------------------");
        logger.info("Ready to broadcast FX data to subscribers.");
        logger.info("============================================================");


    }
}