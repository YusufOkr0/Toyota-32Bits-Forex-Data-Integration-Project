package com.toyota.service.Impl;

import com.toyota.service.CoordinatorService;
import com.toyota.config.ApplicationConfig;
import com.toyota.entity.Rate;
import com.toyota.service.SubscriberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpSubscriberImpl implements SubscriberService {

    public static final Logger log = LoggerFactory.getLogger(TcpSubscriberImpl.class);

    private final int serverPort;
    private final String serverHost;

    private final String username;
    private final String password;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    private final CoordinatorService coordinator;
    private final ExecutorService executorService;

    public TcpSubscriberImpl(CoordinatorService coordinator,ApplicationConfig applicationConfig) {
        this.coordinator = coordinator;
        this.executorService = Executors.newFixedThreadPool(2);


        this.serverHost = applicationConfig.getValue("tcp.platform.host");
        this.serverPort = applicationConfig.getIntValue("tcp.platform.port");
        this.username = applicationConfig.getValue("tcp.platform.username");
        this.password = applicationConfig.getValue("tcp.platform.password");
    }

    @Override
    public void connect(String platformName) {
        log.info("Tcp Subscriber: Attempting to connect to platform: {}", platformName);
        try {
            socket = new Socket(serverHost, serverPort);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(socket.getOutputStream(), true);

            sendMessageToServer(String.format("connect|%s|%s", username, password));

            String serverMessage = reader.readLine();

            if (serverMessage == null) {
                log.warn("Tcp Subscriber: Connection attempt to {} failed: Server closed connection without response.", platformName);
                closeResources();
                coordinator.onConnect(platformName, false);
            } else if (serverMessage.startsWith("ERROR")) {
                log.warn("Tcp Subscriber: Connection attempt to {} failed: Server returned error: {}.", platformName, serverMessage);
                closeResources();
                coordinator.onConnect(platformName, false);
            } else if (serverMessage.startsWith("SUCCESS")) {
                log.info("Tcp Subscriber: Connection to platform: {} successfully.",platformName);
                executorService.execute(() -> listenToIncomingRates(platformName));
                coordinator.onConnect(platformName, true);
            }

        } catch (IOException e) {
            log.warn("Tcp Subscriber: Connection attempt to {} failed.",platformName,e);
            closeResources();
            coordinator.onConnect(platformName, false);
        }
    }

    @Override
    public void disConnect() {
        sendMessageToServer("disconnect");
        closeResources();
        log.info("Tcp Subscriber: Disconnected from platform: TCP successfully.");
    }

    @Override
    public void subscribe(String platformName, String rateName) {
        sendMessageToServer(String.format("subscribe|%s_%s", platformName, rateName));
        log.info("Tcp Subscriber: Subscribed to rate: {} on platform: {}", rateName, platformName);
    }

    @Override
    public void unSubscribe(String platformName, String rateName) {
        sendMessageToServer(String.format("unsubscribe|%s_%s", platformName, rateName));
        log.info("Tcp Subscriber: Unsubscribed to rate: {} on platform: {}", rateName, platformName);
    }

    private void listenToIncomingRates(String platformName) {
        log.info("Tcp Subscriber: Start to listen to incoming rates for platform: {}",platformName);
        Set<String> receivedRates = new HashSet<>();

        try {
            String serverMessage;
            while (!socket.isClosed() && (serverMessage = reader.readLine()) != null) {
                if (serverMessage.startsWith("TCP_")) {
                    String rateName = serverMessage.substring(4, 10);
                    Rate rate = convertMessageToRate(serverMessage);

                    if (receivedRates.contains(rateName)) {
                        coordinator.onRateUpdate(platformName, rateName, rate);
                    } else {
                        receivedRates.add(rateName);
                        coordinator.onRateAvailable(platformName, rateName, rate);
                    }
                }

            }
        } catch (IOException e) {
            log.warn("Tcp Subscriber: Server listening error for platform: {}",platformName);
        } finally {
            receivedRates.clear();
            closeResources();
            coordinator.onDisConnect(platformName);
        }
    }

    private Rate convertMessageToRate(String message) {
        String[] messageParts = message.split("\\|");

        String rateName = messageParts[0];
        BigDecimal bid = new BigDecimal(messageParts[1].split(":")[1]);
        BigDecimal ask = new BigDecimal(messageParts[2].split(":")[1]);
        String timeStampStr = messageParts[3].split(":", 2)[1];

        LocalDateTime timeStamp = LocalDateTime.parse(timeStampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return new Rate(rateName, bid, ask, timeStamp);
    }

    private void sendMessageToServer(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    private void closeResources() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            log.error("Tcp Subscriber: Error closing connection:{} ",e.getMessage(),e);
        } finally {
            socket = null;
            reader = null;
            writer = null;
        }
    }
}