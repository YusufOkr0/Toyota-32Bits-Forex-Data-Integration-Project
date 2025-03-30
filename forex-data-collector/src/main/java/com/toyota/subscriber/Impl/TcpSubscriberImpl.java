package com.toyota.subscriber.Impl;

import com.toyota.coordinator.CoordinatorService;
import com.toyota.config.ConfigUtil;
import com.toyota.entity.Rate;
import com.toyota.subscriber.SubscriberService;

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

    private final int SERVER_PORT;
    private final String SERVER_HOST;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    private final CoordinatorService coordinator;
    private final ExecutorService executorService;

    public TcpSubscriberImpl(CoordinatorService coordinator) {
        this.coordinator = coordinator;
        this.executorService = Executors.newFixedThreadPool(2);


        this.SERVER_HOST = ConfigUtil.getValue("tcp.platform.host");
        this.SERVER_PORT = Integer.parseInt(ConfigUtil.getValue("tcp.platform.port"));
    }

    @Override
    public void connect(String platformName, String username, String password) {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(socket.getOutputStream(), true);

            sendMessageToServer(String.format("connect|%s|%s", username, password));

            String serverMessage = reader.readLine();

            if (serverMessage.startsWith("ERROR")) {
                closeResources();
                coordinator.onConnect(platformName, false);
            } else if (serverMessage.startsWith("SUCCESS")) {
                System.out.println("Bağlantı başarılı.");
                listenToIncomingRates(platformName);
                coordinator.onConnect(platformName, true);
            }

        } catch (IOException e) {
            System.err.println("TCP bağlantı hatası: " + e.getMessage());
            closeResources();
            coordinator.onConnect(platformName, false);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid TCP server port: " + ConfigUtil.getValue("tcp.server.port"), e);
        }
    }

    @Override
    public void disConnect(String platformName, String username, String password) {
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
        executorService.submit(() -> {
            try {
                Set<String> receivedRates = new HashSet<>();

                String serverMessage;
                while (!socket.isClosed() && (serverMessage = reader.readLine()) != null) {
                    if (serverMessage.startsWith("TCP_")) {
                        String rateName = serverMessage.substring(4, 10);

                        Rate rate = getRateObjectFromMessage(serverMessage);
                        if (!receivedRates.contains(rateName)) {
                            receivedRates.add(rateName);
                            coordinator.onRateAvailable(platformName, rateName, rate);
                        } else {
                            coordinator.onRateUpdate(platformName, rateName, rate);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Sunucu dinleme hatası: " + e.getMessage());
                closeResources();
                coordinator.onDisConnect(platformName, true);
            }
        });
    }

    private Rate getRateObjectFromMessage(String message) {
        String[] messageParts = message.split("\\|");

        String rateName = messageParts[0].replace("TCP_", "");
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
        }
    }
}