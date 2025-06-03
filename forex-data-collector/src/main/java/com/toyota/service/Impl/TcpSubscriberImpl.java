package com.toyota.service.Impl;

import com.toyota.config.SubscriberConfig;
import com.toyota.entity.Rate;
import com.toyota.service.CoordinatorService;
import com.toyota.service.SubscriberService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpSubscriberImpl implements SubscriberService {

    public static final Logger log = LogManager.getLogger(TcpSubscriberImpl.class);

    private final int serverPort;
    private final String serverHost;

    private final String username;
    private final String password;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    private final CoordinatorService coordinator;
    private final SubscriberConfig subscriberConfig;
    private final ExecutorService executorService;
    private final Set<String> receivedRates;

    public TcpSubscriberImpl(CoordinatorService coordinator,SubscriberConfig subscriberConfig) {
        this.coordinator = coordinator;
        this.subscriberConfig = subscriberConfig;
        this.executorService = Executors.newFixedThreadPool(1);
        this.receivedRates = ConcurrentHashMap.newKeySet();


        this.serverHost = subscriberConfig.getProperty("host",String.class);
        this.serverPort = subscriberConfig.getProperty("port",Integer.class);
        this.username = subscriberConfig.getProperty("username",String.class);
        this.password = subscriberConfig.getProperty("password",String.class);
    }

    @Override
    public void connect(String platformName) {
        log.info("connect: Attempting to connect to platform: {}", platformName);
        try {
            socket = new Socket(serverHost, serverPort);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(socket.getOutputStream(), true);

            sendMessageToServer(String.format("connect|%s|%s", username, password));

            String serverMessage = reader.readLine();

            if (serverMessage == null) {
                log.error("connect: Connection attempt to {} failed: Server closed connection without response.", platformName);
                closeResources();
                coordinator.onConnect(platformName, false);
            } else if (serverMessage.startsWith("ERROR")) {
                log.error("connect: Connection attempt to {} failed: Server returned error: {}.", platformName, serverMessage);
                closeResources();
                coordinator.onConnect(platformName, false);
            } else if (serverMessage.startsWith("SUCCESS")) {
                log.info("connect: Connection to platform: {} successfully.",platformName);
                executorService.execute(() -> listenToIncomingRates(platformName));
                coordinator.onConnect(platformName, true);
            }

        } catch (IOException e) {
            log.error("connect: Connection attempt to {} failed. Exception Message: {}.",platformName,e.getMessage(),e);
            closeResources();
            coordinator.onConnect(platformName, false);
        }
    }

    @Override
    public void disConnect() {
        sendMessageToServer("disconnect");
        closeResources();
        log.info("disConnect: Disconnected from platform: TCP successfully.");
    }

    @Override
    public void subscribe(String platformName, String rateName) {
        log.info("subscribe: Subscribing to rate: {} on platform: {}", rateName, platformName);
        sendMessageToServer(String.format("subscribe|%s_%s", platformName, rateName));
    }

    @Override
    public void unSubscribe(String platformName, String rateName) {
        log.info("unSubscribe: Unsubscribing from rate: {} on platform: {}", rateName, platformName);
        sendMessageToServer(String.format("unsubscribe|%s_%s", platformName, rateName));
    }

    @Override
    public SubscriberConfig getConfig() {
        return this.subscriberConfig;
    }



    private void listenToIncomingRates(String platformName) {
        log.info("listenToIncomingRates: Start to listen to incoming rates for platform: {}",platformName);
        try {
            String serverMessage;
            while (!socket.isClosed() && (serverMessage = reader.readLine()) != null) {
                if (serverMessage.startsWith("TCP_")) {
                    String rateName = serverMessage.substring(4, 10);
                    Rate rate = convertMessageToRate(serverMessage);
                    if(rate == null) return;

                    if (receivedRates.contains(rateName)) {
                        coordinator.onRateUpdate(platformName, rateName, rate);
                    } else {
                        receivedRates.add(rateName);
                        coordinator.onRateAvailable(platformName, rateName, rate);
                    }
                } else if (serverMessage.contains("INFO|Not subscribed to currency pair")) {
                    log.warn("listenToIncomingRates: Not subscribed to currency pair.");
                } else if (serverMessage.contains("INFO|Already subscribed to currency pair")) {
                    log.warn("listenToIncomingRates: Already subscribed to currency pair.");
                } else if (serverMessage.contains("ERROR|Invalid currency pair")) {
                    log.warn("listenToIncomingRates: Invalid currency pair. Cannot subscribe.");
                }
            }

        } catch (IOException e) {
            log.error("listenToIncomingRates: Server listening error for platform: {}",platformName,e);
        } finally {
            closeResources();
            coordinator.onDisConnect(platformName);
        }
    }

    private Rate convertMessageToRate(String message) {
        try {
            String[] messageParts = message.split("\\|");

            String rateName = messageParts[0];
            BigDecimal bid = new BigDecimal(messageParts[1].split(":")[1]);
            BigDecimal ask = new BigDecimal(messageParts[2].split(":")[1]);
            String timeStampStr = messageParts[3].split(":", 2)[1];

            Instant timeStamp = Instant.parse(timeStampStr);
            return new Rate(rateName, bid, ask, timeStamp);

        } catch (Exception e) {
            log.error("convertMessageToRate: Error when parsing incoming message to rate object: {}.",e.getMessage(),e);
            return null;
        }
    }

    private void sendMessageToServer(String message) {
        if (writer != null) {
            writer.print(message + "\n");
            writer.flush();
        }
    }

    private void closeResources() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            log.error("closeResources: Error closing connection: {} ",e.getMessage(),e);
        } finally {
            socket = null;
            reader = null;
            writer = null;
        }
    }
}