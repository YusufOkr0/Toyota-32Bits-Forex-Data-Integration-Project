package com.toyota.auth;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages user authentication and session state for {@link SocketChannel} based connections.
 * This service validates user credentials against a provided repository and tracks active,
 * authenticated sessions associated with specific communication channels.
 */
public class AuthService {

    private final Map<SocketChannel, String> connectedClients;
    private final Map<String, String> authRepository;

    public AuthService(Map<String, String> authRepository) {
        this.connectedClients = new HashMap<>();
        this.authRepository = authRepository;
    }

    /**
     * Authenticates a user based on the provided username and password.
     * Checks against the internal authentication repository.
     *
     * @param username The username to authenticate.
     * @param password The password provided for the username.
     * @return {@code true} if the credentials are valid according to the repository, {@code false} otherwise.
     */
    public boolean authenticateUser(String username, String password) {
        return authRepository.containsKey(username) && authRepository.get(username).equals(password);
    }

    /**
     * Creates an active session for a successfully authenticated client channel.
     * Associates the given SocketChannel with the authenticated username.
     *
     * @param clientChannel The communication channel of the authenticated client.
     * @param username      The authenticated username to associate with this channel.
     */
    public void createSession(SocketChannel clientChannel,String username){
        connectedClients.put(clientChannel, username);
    }

    /**
     * Checks if a specific client channel currently has an active, authenticated session.
     * Verifies if the channel exists as a key in the active sessions map.
     *
     * @param clientChannel The client channel to check.
     * @return {@code true} if this specific channel has an active session, {@code false} otherwise.
     */
    public boolean isClientAuthenticated(SocketChannel clientChannel) {
        return connectedClients.containsKey(clientChannel);
    }

    /**
     * Checks if the given username is currently associated with *any* active session
     * across all connected channels.
     * Helps determine if a username is already "logged in" via another connection.
     *
     * @param username The username to check for an active session.
     * @return {@code true} if any active channel is associated with this username, {@code false} otherwise.
     */
    public boolean isClientHasASession(String username) {
        return connectedClients.containsValue(username);
    }

    /**
     * Removes the session associated with the specified client channel.
     * Should be called when a client disconnects or its session ends.
     *
     * @param clientChannel The client channel whose session should be terminated.
     */
    public void disconnect(SocketChannel clientChannel) {
        connectedClients.remove(clientChannel);
    }


}