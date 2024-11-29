package com.thanhtuanle.chatclient;

public class Main {

    public static AuthScreen authScreen;
    public static ChatScreen chatScreen;
    public static Client client;
    public static Thread clientThread;

    public static void main(String[] args) {
        authScreen = new AuthScreen();
        client = new Client();
        clientThread = new Thread(client);
        clientThread.start();
    }
}
