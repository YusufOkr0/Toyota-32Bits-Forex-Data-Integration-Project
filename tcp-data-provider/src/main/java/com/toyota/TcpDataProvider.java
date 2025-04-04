package com.toyota;

import com.toyota.auth.AuthService;
import com.toyota.broadcast.FxDataPublisher;
import com.toyota.config.ServerConfig;
import com.toyota.entity.Rate;
import com.toyota.server.FxDataServer;
import org.aeonbits.owner.ConfigFactory;

import java.math.BigDecimal;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpDataProvider {
    public static void main(String[] args) {

        final ExecutorService executorService = Executors.newFixedThreadPool(2);
        final ServerConfig configLoader = ConfigFactory.create(ServerConfig.class);

        final int SERVER_PORT = configLoader.serverPort();
        final List<String> CURRENCY_PAIRS = configLoader.currencyPairs();
        final List<String> USER_CREDENTIALS = configLoader.userCredentials();


        final ConcurrentHashMap<String, Set<SocketChannel>> SUBSCRIPTIONS = new ConcurrentHashMap<>();
        for(String currencyPair : CURRENCY_PAIRS){
            SUBSCRIPTIONS.put(currencyPair,ConcurrentHashMap.newKeySet());
        }



        final Map<String,String> AUTH_REPOSITORY = new HashMap<>();

        for(String nameAndPassword : USER_CREDENTIALS){
            String[] credentials = nameAndPassword.split("\\|");
            AUTH_REPOSITORY.put(credentials[0],credentials[1]);
        }

        final AuthService AUTH_SERVICE = new AuthService(AUTH_REPOSITORY);





        BigDecimal usdTryBid = BigDecimal.valueOf(configLoader.usdTryBid());
        BigDecimal usdTryAsk = BigDecimal.valueOf(configLoader.usdTryAsk());
        BigDecimal usdTryMinLimit = BigDecimal.valueOf(configLoader.usdTryMinLimit());
        BigDecimal usdTryMaxLimit = BigDecimal.valueOf(configLoader.usdTryMaxLimit());

        BigDecimal eurUsdBid = BigDecimal.valueOf(configLoader.eurUsdBid());
        BigDecimal eurUsdAsk = BigDecimal.valueOf(configLoader.eurUsdAsk());
        BigDecimal eurUsdMinLimit = BigDecimal.valueOf(configLoader.eurUsdMinLimit());
        BigDecimal eurUsdMaxLimit = BigDecimal.valueOf(configLoader.eurUsdMaxLimit());

        BigDecimal gbpUsdBid = BigDecimal.valueOf(configLoader.gbpUsdBid());
        BigDecimal gbpUsdAsk = BigDecimal.valueOf(configLoader.gbpUsdAsk());
        BigDecimal gbpUsdMinLimit = BigDecimal.valueOf(configLoader.gbpUsdMinLimit());
        BigDecimal gbpUsdMaxLimit = BigDecimal.valueOf(configLoader.gbpUsdMaxLimit());

        Rate USD_TRY = new Rate("TCP_USDTRY", usdTryBid, usdTryAsk, LocalDateTime.now(),usdTryMinLimit,usdTryMaxLimit);
        Rate EUR_USD = new Rate("TCP_EURUSD", eurUsdBid, eurUsdAsk, LocalDateTime.now(),eurUsdMinLimit,eurUsdMaxLimit);
        Rate GBP_USD = new Rate("TCP_GBPUSD", gbpUsdBid, gbpUsdAsk, LocalDateTime.now(),gbpUsdMinLimit,gbpUsdMaxLimit);

        final List<Rate> INITIAL_RATES = new ArrayList<>();
        INITIAL_RATES.add(USD_TRY);
        INITIAL_RATES.add(EUR_USD);
        INITIAL_RATES.add(GBP_USD);

        final int PUBLISH_FREQUENCY = configLoader.publishFrequency();



        FxDataServer fxDataServer = new FxDataServer(
                SERVER_PORT,
                CURRENCY_PAIRS,
                SUBSCRIPTIONS,
                AUTH_SERVICE
        );

        FxDataPublisher publisher = new FxDataPublisher(
                SUBSCRIPTIONS,
                INITIAL_RATES,
                PUBLISH_FREQUENCY
        );

        executorService.submit(fxDataServer::startServer);
        executorService.submit(publisher::startBroadcast);


    }
}