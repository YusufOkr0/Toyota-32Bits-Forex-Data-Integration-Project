package com.toyota.service.Impl;

import com.toyota.service.CoordinatorService;
import com.toyota.config.ApplicationConfig;
import com.toyota.entity.Rate;
import com.toyota.service.SubscriberService;

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
        try {
            socket = new Socket(serverHost, serverPort);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(socket.getOutputStream(), true);


            sendMessageToServer(String.format("connect|%s|%s", username, password));

            String serverMessage = reader.readLine();

            if (serverMessage == null) {
                System.err.printf("Connection attempt to %s failed: Server closed connection without response.%n", platformName);
                closeResources();
                coordinator.onConnect(platformName, false);
            } else if (serverMessage.startsWith("ERROR")) {
                System.err.printf("Connection attempt to %s failed: Server returned error: %s%n", platformName, serverMessage);
                closeResources();
                coordinator.onConnect(platformName, false);
            } else if (serverMessage.startsWith("SUCCESS")) {
                System.out.printf("Connection to %s successful.%n", platformName);
                executorService.execute(() -> listenToIncomingRates(platformName));
                coordinator.onConnect(platformName, true);
            } else {
                System.err.printf("Connection attempt to %s failed: Unexpected server response: %s%n", platformName, serverMessage);
                closeResources();
                coordinator.onConnect(platformName, false); // Bağlantı başarısız
            }

        } catch (IOException e) {
            System.err.println("TCP bağlantı hatası: " + e.getMessage());
            closeResources();
            coordinator.onConnect(platformName, false);
        }
    }

    @Override
    public void disConnect() {
        sendMessageToServer(String.format("disconnect|%s|%s", username, password));
        closeResources();
    }

    @Override
    public void subscribe(String platformName, String rateName) {
        sendMessageToServer(String.format("subscribe|%s_%s", platformName, rateName));
    }

    @Override
    public void unSubscribe(String platformName, String rateName) {
        sendMessageToServer(String.format("unsubscribe|%s_%s", platformName, rateName));
    }

    private void listenToIncomingRates(String platformName) {
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
            System.err.println("Sunucu dinleme hatası: " + e.getMessage());
        } finally {
            closeResources();
            System.out.println("Stopped listening to the server.");
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
        } else {
            System.err.printf("Error while sending message: %s%n", message);
        }
    }

    private void closeResources() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Bağlantıyı kapatma hatası: " + e.getMessage());
        } finally {
            socket = null;
            reader = null;
            writer = null;
        }
    }
}