package com.toyota.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FxDataServer {

    private static final String ERROR_INVALID_COMMAND = "ERROR|Invalid command. Please enter one of these: connect,disconnect,subscribe,unsubscribe";
    private static final String ERROR_INVALID_MESSAGE_FORMAT = "ERROR|Invalid message format";
    private static final String ERROR_INVALID_CURRENCY_PAIR = "ERROR|Invalid currency pair: ";
    private static final String INFO_ALREADY_SUBSCRIBED = "INFO|Already subscribed to currency pair: ";
    private static final String SUCCESS_SUBSCRIBED = "SUCCESS|Subscribed to currency pair: ";
    private static final String SUCCESS_UNSUBSCRIBED = "SUCCESS|Unsubscribed from currency pair: ";
    private static final String INFO_NOT_SUBSCRIBED = "INFO|Not subscribed to currency pair: ";

    private final int SERVER_PORT;
    private Selector selector;

    private final Set<String> currencyPairs;
    private final ConcurrentHashMap<String,Set<SocketChannel>> subscriptions;

    public FxDataServer(int server_port,
                        Set<String> currency_pairs,
                        ConcurrentHashMap<String, Set<SocketChannel>> subscriptions) {
        this.SERVER_PORT = server_port;
        this.currencyPairs = currency_pairs;
        this.subscriptions = subscriptions;

    }



    public void startServer() {
        try(ServerSocketChannel serverChannel = ServerSocketChannel.open())
        {
            this.selector = Selector.open();
            serverChannel.bind(new InetSocketAddress(SERVER_PORT));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (selector.isOpen() && serverChannel.isOpen()){
                selector.select(); // Listening for events
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while(iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    try {
                        if (key.isAcceptable()) {
                            handleConnectionRequest(serverChannel);
                        } else if (key.isReadable()) {
                            handleClientMessage(key);     // Key keeps channel and its events
                        }
                    } catch (IOException e) {
                        System.err.println("IOException during event handling: " + e.getMessage());
                    }
                    iterator.remove();
                }
            }
            selector.close();
        } catch (IOException e) {
            System.err.println("IOException occurred in startServer: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected exception in startServer: " + e.getMessage());
        }
    }




    private void handleConnectionRequest(ServerSocketChannel serverChannel) throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        if (clientChannel != null) {

            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ);

            System.out.println("Client connected: " + clientChannel.getRemoteAddress());
        }
    }


    private void handleClientMessage(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = clientChannel.read(buffer);

        if(bytesRead == -1){
            shutDownClient(key);
        }

        if (bytesRead > 0) {
            buffer.flip();
            byte[] byteData = new byte[buffer.remaining()];
            buffer.get(byteData);

            String[] clientMessage = new String(byteData, StandardCharsets.UTF_8)
                    .split("\r\n");

            for(String message : clientMessage){
                validateMessageAndTakeAction(clientChannel,message);
            }

        }

    }

    private void validateMessageAndTakeAction(SocketChannel clientChannel, String message) {

        String[] messageParts = message.split("\\|");
        String command = messageParts[0];

        switch (command) {
            case "connect":
                // handle connection
                break;

            case "disconnect":
                // handle disconnection
                break;

            case "subscribe":
                handleSubscribe(clientChannel, messageParts);
                break;

            case "unsubscribe":
                handleUnsubscribe(clientChannel, messageParts);
                break;

            default:
                sendInfoMessageToClient(clientChannel, ERROR_INVALID_COMMAND);
                break;
        }
    }



    private void handleSubscribe(SocketChannel clientChannel, String[] messageParts) {
        if (messageParts.length != 2) {
            sendInfoMessageToClient(clientChannel, ERROR_INVALID_MESSAGE_FORMAT);
            return;
        }

        String currencyPair = messageParts[1].trim().toUpperCase();

        if (!currencyPairs.contains(currencyPair)) {
            sendInfoMessageToClient(clientChannel, ERROR_INVALID_CURRENCY_PAIR + currencyPair);
            return;
        }

        Set<SocketChannel> clients = subscriptions.get(currencyPair);   // Get clients which subscribe a particular rate.
        if (clients.contains(clientChannel)) {
            sendInfoMessageToClient(clientChannel, INFO_ALREADY_SUBSCRIBED + currencyPair);
        } else {
            clients.add(clientChannel);
            sendInfoMessageToClient(clientChannel, SUCCESS_SUBSCRIBED + currencyPair);
        }
    }


    private void handleUnsubscribe(SocketChannel clientChannel, String[] messageParts) {
        if (messageParts.length != 2) {
            sendInfoMessageToClient(clientChannel, ERROR_INVALID_MESSAGE_FORMAT);
            return;
        }
        String currencyPair = messageParts[1].trim().toUpperCase();

        if (!currencyPairs.contains(currencyPair)) {
            sendInfoMessageToClient(clientChannel, ERROR_INVALID_CURRENCY_PAIR + currencyPair);
            return;
        }

        Set<SocketChannel> clients = subscriptions.get(currencyPair);
        if (clients.contains(clientChannel)) {
            clients.remove(clientChannel);
            sendInfoMessageToClient(clientChannel, SUCCESS_UNSUBSCRIBED + currencyPair);
        } else {
            sendInfoMessageToClient(clientChannel, INFO_NOT_SUBSCRIBED + currencyPair);
        }
    }







    private void shutDownClient(SelectionKey key){
        try {

            SocketChannel clientChannel = (SocketChannel) key.channel();
            System.out.println("Shutting down client: " + clientChannel.getRemoteAddress());


            subscriptions.values()
                    .forEach(clients -> clients.remove(clientChannel));

            key.cancel();

            clientChannel.close();

        } catch (IOException e) {
            System.err.println("IOException while closing client resources: " + e.getMessage());
        }
    }



    private void sendInfoMessageToClient(SocketChannel clientChannel, String message) {
        ByteBuffer buffer = ByteBuffer.wrap((message + "\r\n").getBytes(StandardCharsets.UTF_8));
        try {
            clientChannel.write(buffer);
        } catch (IOException e) {
            System.err.println("IOException while sending message: " + e.getMessage());
        }
    }





}