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

public class Main {
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

        FxDataServer fxDataServer = new FxDataServer(
                SERVER_PORT,
                CURRENCY_PAIRS,
                SUBSCRIPTIONS,
                AUTH_SERVICE
        );

        executorService.submit(fxDataServer::startServer);

















    }
}