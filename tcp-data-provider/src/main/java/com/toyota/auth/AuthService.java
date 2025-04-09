package com.toyota.auth;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

/***
 * isClientAuthenticated ve isClientHasASession fonksiyonlari benzer olabilir.
 * Eger iki farkli ip adresinden (channel'dan) ayni username ve password girilir ise
 * bu ayirt edilmeli ve handle edilmeli.
 */
public class AuthService {

    private final Map<SocketChannel, String> connectedClients;
    private final Map<String, String> authRepository;

    public AuthService(Map<String, String> authRepository) {
        this.connectedClients = new HashMap<>();
        this.authRepository = authRepository;
    }

    public boolean authenticateUser(String username, String password) {
        return authRepository.containsKey(username) && authRepository.get(username).equals(password);
    }

    public void createSession(SocketChannel clientChannel,String username){
        connectedClients.put(clientChannel, username);
    }

    public boolean isClientAuthenticated(SocketChannel clientChannel) {
        return connectedClients.containsKey(clientChannel);
    }

    /***
     *  THIS IS GONNA CHECK IF USERNAME IS TAKEN OR NOT?
     * @param username
     * @return
     */
    public boolean isClientHasASession(String username) {
        return connectedClients.containsValue(username);
    }


    public void disconnect(SocketChannel clientChannel) {
        connectedClients.remove(clientChannel);
    }


}