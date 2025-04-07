package com.toyota.server;

import com.toyota.auth.AuthService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class FxDataServer {

    private static final String ERROR_NOT_CONNECTED = "ERROR|Not connected";
    private static final String ERROR_INVALID_COMMAND = "ERROR|Invalid command. Please enter one of these: connect,disconnect,subscribe,unsubscribe";
    private static final String ERROR_INVALID_CREDENTIALS = "ERROR|Invalid credentials";
    private static final String ERROR_INVALID_CURRENCY_PAIR = "ERROR|Invalid currency pair: ";
    private static final String ERROR_INVALID_MESSAGE_FORMAT = "ERROR|Invalid message format";
    private static final String ERROR_UNAUTHORIZED_TO_DISCONNECT = "ERROR|You can only disconnect your own session";
    private static final String ERROR_CLIENT_ALREADY_HAS_A_SESSION = "ERROR|User already logged in from another session";
    private static final String INFO_NOT_SUBSCRIBED = "INFO|Not subscribed to currency pair: ";
    private static final String INFO_ALREADY_SUBSCRIBED = "INFO|Already subscribed to currency pair: ";
    private static final String INFO_CLIENT_ALREADY_CONNECTED = "INFO|User already connected";
    private static final String SUCCESS_SUBSCRIBED = "SUCCESS|Subscribed to currency pair: ";
    private static final String SUCCESS_UNSUBSCRIBED = "SUCCESS|Unsubscribed from currency pair: ";
    private static final String SUCCESS_CONNECTED = "SUCCESS|CONNECTED";


    private static final Logger logger = LogManager.getLogger(FxDataServer.class);

    private Selector selector;
    private final int SERVER_PORT;

    private final AuthService authService;
    private final List<String> currencyPairs;
    private final ConcurrentHashMap<String, Set<SocketChannel>> subscriptions;

    public FxDataServer(int server_port,
                        List<String> currency_pairs,
                        ConcurrentHashMap<String, Set<SocketChannel>> subscriptions,
                        AuthService authService) {
        this.SERVER_PORT = server_port;
        this.currencyPairs = currency_pairs;
        this.subscriptions = subscriptions;
        this.authService = authService;
    }


    public void startServer() {
        logger.trace("startServer method begins.");
        try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            this.selector = Selector.open();
            serverChannel.bind(new InetSocketAddress(SERVER_PORT));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (selector.isOpen() && serverChannel.isOpen()) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (!key.isValid()) {
                        logger.warn("Skipping invalid SelectionKey.");
                        continue;
                    }

                    if (key.isAcceptable()) {
                        logger.trace("Handling new connection request.");
                        handleConnectionRequest(serverChannel);
                    } else if (key.isReadable()) {
                        logger.trace("Handling incoming client message.");
                        handleClientMessage(key);     // Key keeps channel and its events
                    }
                }

            }

            if(selector != null && selector.isOpen()){
                logger.trace("Closing selector.");
                selector.close();
            }

        } catch (IOException e) {
            logger.error("IOException during server socket setup or main loop on port {}: {}", SERVER_PORT, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected exception during server socket setup or main loop on port {}: {}", SERVER_PORT, e.getMessage(), e);
        }
        logger.info("FX Data Server stopped.");
        logger.trace("startServer method finished.");
    }

    private void handleConnectionRequest(ServerSocketChannel serverChannel) {
        logger.trace("handleConnectionRequest method called.");
        try {
            SocketChannel clientChannel = serverChannel.accept();
            if (clientChannel != null) {
                clientChannel.configureBlocking(false);
                clientChannel.register(selector, SelectionKey.OP_READ);

                logger.info("Client connected: {}", clientChannel.getRemoteAddress());
            }
        } catch (IOException e) {
            logger.error("IOException while handling connection request: {}", e.getMessage(), e);
        }
        logger.trace("handleConnectionRequest method finished.");
    }


    private void handleClientMessage(SelectionKey key) {
        logger.trace("handleClientMessage method called.");
        try {
            SocketChannel clientChannel = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead = clientChannel.read(buffer);

            if (bytesRead == -1) {
                logger.info("Client {} disconnected gracefully (read returned -1).", clientChannel.getRemoteAddress());
                shutDownClient(key);
            } else if (bytesRead > 0) {
                buffer.flip();
                byte[] byteData = new byte[buffer.remaining()];
                buffer.get(byteData);

                String[] clientMessage = new String(byteData, StandardCharsets.UTF_8)
                        .split("\r\n");

                for (String message : clientMessage) {
                    logger.debug("Received message from client {}: {}", clientChannel.getRemoteAddress(), message);
                    validateMessageAndTakeAction(clientChannel, message);
                }

            }
        } catch (IOException e) {
            String clientAddr = getClientAddressSafe((SocketChannel)key.channel());
            logger.warn("IOException while handling client message from {}: {}. Closing connection.", clientAddr, e.getMessage());
            shutDownClient(key);
        }
        logger.trace("handleClientMessage method finished.");
    }

    private void validateMessageAndTakeAction(SocketChannel clientChannel, String message) {
        logger.trace("validateMessageAndTakeAction method called for message: {}", message);

        String[] messageParts = message.split("\\|");
        String command = messageParts[0];

        logger.debug("Processing command: {}", command);
        switch (command) {
            case "connect":
                handleConnect(clientChannel, messageParts);
                break;

            case "disconnect":
                handleDisconnect(clientChannel, messageParts);
                break;

            case "subscribe":
                handleSubscribe(clientChannel, messageParts);
                break;

            case "unsubscribe":
                handleUnsubscribe(clientChannel, messageParts);
                break;

            default:
                logger.warn("Invalid command received: {}", command);
                sendInfoMessageToClient(clientChannel, ERROR_INVALID_COMMAND);
                break;
        }
        logger.trace("validateMessageAndTakeAction method finished.");
    }


    private void handleSubscribe(SocketChannel clientChannel, String[] messageParts) {
        logger.trace("handleSubscribe method called.");

        if (!authService.isClientConnected(clientChannel)) {
            logger.warn("Subscribe attempt failed for {}: Not connected.", getClientAddressSafe(clientChannel));
            sendInfoMessageToClient(clientChannel, ERROR_NOT_CONNECTED);
            return;
        }


        if (messageParts.length != 2) {
            logger.warn("Subscribe attempt failed for {}: Invalid message format.", getClientAddressSafe(clientChannel));
            sendInfoMessageToClient(clientChannel, ERROR_INVALID_MESSAGE_FORMAT);
            return;
        }

        String currencyPair = messageParts[1].trim().toUpperCase();


        if (!currencyPairs.contains(currencyPair)) {
            logger.warn("Subscribe attempt failed for {}: Invalid currency pair '{}'.", getClientAddressSafe(clientChannel), currencyPair);
            sendInfoMessageToClient(clientChannel, ERROR_INVALID_CURRENCY_PAIR + currencyPair);
            return;
        }

        Set<SocketChannel> clients = subscriptions.get(currencyPair);
        if (clients.contains(clientChannel)) {
            logger.info("Client {} already subscribed to {}", getClientAddressSafe(clientChannel), currencyPair);
            sendInfoMessageToClient(clientChannel, INFO_ALREADY_SUBSCRIBED + currencyPair);
        } else {
            logger.info("Client {} successfully subscribed to {}", getClientAddressSafe(clientChannel), currencyPair);
            clients.add(clientChannel);
            sendInfoMessageToClient(clientChannel, SUCCESS_SUBSCRIBED + currencyPair);
        }
        logger.trace("handleSubscribe method finished.");
    }


    private void handleUnsubscribe(SocketChannel clientChannel, String[] messageParts) {
        logger.trace("handleUnsubscribe method called.");

        if (!authService.isClientConnected(clientChannel)) {
            logger.warn("Unsubscribe attempt failed for {}: Not connected.", getClientAddressSafe(clientChannel));
            sendInfoMessageToClient(clientChannel, ERROR_NOT_CONNECTED);
            return;
        }


        if (messageParts.length != 2) {
            logger.warn("Unsubscribe attempt failed for {}: Invalid message format.", getClientAddressSafe(clientChannel));
            sendInfoMessageToClient(clientChannel, ERROR_INVALID_MESSAGE_FORMAT);
            return;
        }
        String currencyPair = messageParts[1].trim().toUpperCase();

        if (!currencyPairs.contains(currencyPair)) {
            logger.warn("Unsubscribe attempt failed for {}: Invalid currency pair '{}'.", getClientAddressSafe(clientChannel), currencyPair);
            sendInfoMessageToClient(clientChannel, ERROR_INVALID_CURRENCY_PAIR + currencyPair);
            return;
        }

        Set<SocketChannel> clients = subscriptions.get(currencyPair);
        if (clients.contains(clientChannel)) {
            logger.info("Client {} successfully unsubscribed from {}", getClientAddressSafe(clientChannel), currencyPair);
            clients.remove(clientChannel);
            sendInfoMessageToClient(clientChannel, SUCCESS_UNSUBSCRIBED + currencyPair);
        } else {
            logger.info("Client {} was not subscribed to {} or pair not found.", getClientAddressSafe(clientChannel), currencyPair);
            sendInfoMessageToClient(clientChannel, INFO_NOT_SUBSCRIBED + currencyPair);
        }
        logger.trace("handleUnsubscribe method finished.");
    }


    private void handleConnect(SocketChannel clientChannel, String[] messageParts) {
        logger.trace("handleConnect method called.");

        if (messageParts.length != 3) {
            logger.warn("Connect attempt failed for {}: Invalid message format.", getClientAddressSafe(clientChannel));
            sendInfoMessageToClient(clientChannel, "ERROR|Invalid message format");
            return;
        }

        String username = messageParts[1].trim();
        String password = messageParts[2].trim();

        if (authService.isClientConnected(clientChannel)) {
            logger.warn("Connect attempt failed for {}: Already connected.", getClientAddressSafe(clientChannel));
            sendInfoMessageToClient(clientChannel, INFO_CLIENT_ALREADY_CONNECTED);
            return;
        }

        if (authService.isClientHasASession(username)) {
            logger.warn("Connect attempt failed for user '{}' from {}: User already has an active session.", username, getClientAddressSafe(clientChannel));
            sendInfoMessageToClient(clientChannel, ERROR_CLIENT_ALREADY_HAS_A_SESSION);
            return;
        }

        if (authService.authenticateUser(clientChannel, username, password)) {
            logger.info("Client {} successfully authenticated as user '{}'.", getClientAddressSafe(clientChannel), username);
            sendInfoMessageToClient(clientChannel, SUCCESS_CONNECTED);
        } else {
            logger.warn("Connect attempt failed for user '{}' from {}: Invalid credentials.", username, getClientAddressSafe(clientChannel));
            sendInfoMessageToClient(clientChannel, ERROR_INVALID_CREDENTIALS);
        }

        logger.trace("handleConnect method finished.");
    }


    private void handleDisconnect(SocketChannel clientChannel, String[] messageParts) {
        logger.trace("handleDisconnect method called.");

        if (messageParts.length != 3) {
            logger.warn("Disconnect attempt failed for {}: Invalid message format.", getClientAddressSafe(clientChannel));
            sendInfoMessageToClient(clientChannel, "ERROR|Invalid message format");
            return;
        }

        String username = messageParts[1].trim();
        String password = messageParts[2].trim();

        if (!authService.isClientConnected(clientChannel)) {
            logger.warn("Disconnect attempt failed for {}: Not connected.", getClientAddressSafe(clientChannel));
            sendInfoMessageToClient(clientChannel, ERROR_NOT_CONNECTED);
            return;
        }

        if (!authService.isAuthorizedToDisconnect(clientChannel, username)) {
            logger.warn("Unauthorized disconnect attempt from channel {} for user '{}'.", getClientAddressSafe(clientChannel), username);
            sendInfoMessageToClient(clientChannel, ERROR_UNAUTHORIZED_TO_DISCONNECT);
            return;
        }

        if (!authService.isCredentialsValidForDisconnect(username, password)) {
            logger.warn("Disconnect attempt failed for user '{}' from {}: Invalid credentials provided for disconnect.", username, getClientAddressSafe(clientChannel));
            sendInfoMessageToClient(clientChannel, ERROR_INVALID_CREDENTIALS);
            return;
        }

        authService.removeAuthenticatedClient(clientChannel);
        logger.info("Disconnect attempt was successful for {}.",getClientAddressSafe(clientChannel));
        shutDownClient(clientChannel.keyFor(selector));

        logger.trace("handleDisconnect method finished.");
    }


    private void shutDownClient(SelectionKey key) {
        logger.trace("shutDownClient method called.");

        try {
            SocketChannel clientChannel = (SocketChannel) key.channel();
            logger.info("Shutting down client: {}", getClientAddressSafe(clientChannel));

            subscriptions.values()
                    .forEach(clients -> clients.remove(clientChannel));
            key.cancel();
            clientChannel.close();

        } catch (IOException e) {
            logger.error("IOException while closing client resources: {}", e.getMessage(), e);
        }
        logger.trace("shutDownClient method finished.");
    }


    private void sendInfoMessageToClient(SocketChannel clientChannel, String message) {
        ByteBuffer buffer = ByteBuffer.wrap((message + "\r\n").getBytes(StandardCharsets.UTF_8));
        try {
            clientChannel.write(buffer);
        } catch (IOException e) {
            logger.error("IOException while sending message: {}", e.getMessage(), e);
        }
    }


    private String getClientAddressSafe(SocketChannel clientChannel) {
        try {
            if (clientChannel != null && clientChannel.isOpen()) {
                return clientChannel.getRemoteAddress().toString();
            }
        } catch (IOException e) {
            logger.warn("IOException when try to get client address.");
        }
        return "unknown address";
    }

}