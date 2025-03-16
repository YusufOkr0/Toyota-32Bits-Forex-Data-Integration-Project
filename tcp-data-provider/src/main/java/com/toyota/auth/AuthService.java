package com.toyota.auth;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class AuthService {

    private final Map<SocketChannel, String> connectedClients;
    private final Map<String, String> authRepository;

    public AuthService(Map<String, String> authRepository) {
        this.connectedClients = new HashMap<>();
        this.authRepository = authRepository;
    }

    public boolean authenticateUser(SocketChannel clientChannel, String username, String password) {
        if (authRepository.containsKey(username) && authRepository.get(username).equals(password)) {
            connectedClients.put(clientChannel, username);
            return true;
        }
        return false;
    }

    public boolean isClientConnected(SocketChannel clientChannel) {
        return connectedClients.containsKey(clientChannel);
    }

    public boolean isClientHasASession(String username) {
        return connectedClients.containsValue(username);
    }

    public boolean isAuthorizedToDisconnect(SocketChannel clientChannel, String username) {
        return connectedClients.get(clientChannel).equals(username);
    }

    public boolean isCredentialsValidForDisconnect(String username, String password) {
        return authRepository.containsKey(username) && authRepository.get(username).equals(password);
    }

    public void removeAuthenticatedClient(SocketChannel clientChannel) {
        connectedClients.remove(clientChannel);
    }


}