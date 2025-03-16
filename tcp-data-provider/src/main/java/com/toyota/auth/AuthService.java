package com.toyota.auth;

import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {

    private final Map<SocketChannel, String> authenticatedClients = new ConcurrentHashMap<>();
    private final Map<String, String> authRepository;

    public AuthService(Map<String, String> authRepository) {
        this.authRepository = authRepository;
    }

    public boolean authenticateUser(SocketChannel clientChannel, String username, String password) {
        if (authRepository.containsKey(username) && authRepository.get(username).equals(password)) {
            authenticatedClients.put(clientChannel, username);
            return true;
        }
        return false;
    }

    public boolean isAuthenticated(SocketChannel clientChannel) {
        return authenticatedClients.containsKey(clientChannel);
    }

    public boolean isClientAlreadyLoggedIn(String username) {
        return authenticatedClients.containsValue(username);
    }

    public boolean isAuthorizedToDisconnect(SocketChannel clientChannel, String username) {
        return authenticatedClients.get(clientChannel).equals(username);
    }

    public boolean isCredentialsValidForDisconnect(String username, String password) {
        return authRepository.containsKey(username) && authRepository.get(username).equals(password);
    }

    public void removeAuthenticatedClient(SocketChannel clientChannel) {
        authenticatedClients.remove(clientChannel);
    }


}