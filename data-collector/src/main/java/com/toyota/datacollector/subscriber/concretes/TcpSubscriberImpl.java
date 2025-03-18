package com.toyota.datacollector.subscriber.concretes;


import com.toyota.datacollector.config.ConfigLoader;
import com.toyota.datacollector.config.SpringContextUtil;
import com.toyota.datacollector.coordinator.abstracts.CoordinatorService;
import com.toyota.datacollector.subscriber.abstracts.SubscriberService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TcpSubscriberImpl implements SubscriberService {

    private final int SERVER_PORT;
    private final String SERVER_HOST;
    private final List<String> SUBSCRIBED_RATES;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    private final CoordinatorService coordinator;

    public TcpSubscriberImpl(CoordinatorService coordinator) {
        ConfigLoader config = SpringContextUtil
                .getApplicationContext()
                .getBean(ConfigLoader.class);
        this.coordinator = coordinator;
        this.SERVER_HOST = config.getTcpServerHost();
        this.SERVER_PORT = config.getTcpServerPort();
        this.SUBSCRIBED_RATES = config.getTcpServerRates();

    }


    @Override
    public void connect(String platformName, String username, String password) {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(socket.getOutputStream(), true);

            sendMessageToServer(String.format("connect|%s|%s",username,password));

            String serverMessage = reader.readLine();

            if(serverMessage.startsWith("ERROR")){
                coordinator.onConnect(platformName,false);  // CHECK LATER.
            }
            else if(serverMessage.startsWith("SUCCESS")){
                System.out.println("baglanti basarili.");
                coordinator.onConnect(platformName,true);

                for (String rate : SUBSCRIBED_RATES) {
                    subscribe(platformName, rate);
                }
                listenToIncomingRates(platformName);

            }

        } catch (IOException e) {
            // CHECK LATER.
            coordinator.onConnect(platformName,false);
            closeResources();
        }
    }



    @Override
    public void disConnect(String platformName, String username, String password) {

    }

    @Override
    public void subscribe(String platformName, String rateName) {
        sendMessageToServer(String.format("subscribe|%s_%s",platformName,rateName));
    }

    @Override
    public void unSubscribe(String platformName, String rateName) {

    }





    private void listenToIncomingRates(String platformName) {
        try {
            String serverMessage;

            while (!socket.isClosed() && (serverMessage = reader.readLine()) != null) {

                System.out.println(serverMessage);
            }
        } catch (IOException e) {
            System.err.println("Sunucu dinleme hatası: " + e.getMessage());
            coordinator.onDisConnect(platformName,true);
            closeResources();
        }
    }



    private void sendMessageToServer(String message) {
        if (writer != null) {
            writer.println(message);
        } else {
            System.err.println("Mesaj gönderilemedi.");
        }
    }


    private void closeResources() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
            System.out.println("Bağlantı kapatıldı.");
        } catch (IOException e) {
            System.err.println("Bağlantıyı kapatma hatası: " + e.getMessage());
        }
    }

}
