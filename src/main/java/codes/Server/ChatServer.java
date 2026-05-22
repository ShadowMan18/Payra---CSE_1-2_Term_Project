package codes.Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

class ChatServer implements Runnable {
    static ConcurrentHashMap<String, String> receiver = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, ChatThread> chatThreads = new ConcurrentHashMap<>();
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private Thread chatServerThread;

    // Initiating chat server

    public ChatServer() {
        chatServerThread = new Thread(this);

        try {
            serverSocket = new ServerSocket(4350);
            System.out.println("Chat server is running at 4350");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        chatServerThread.start();
    }

    @Override
    public void run() {
        // Connecting the client

        while (true) {
            try {
                clientSocket = serverSocket.accept();
                System.out.println("connected to chat server");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Creating new chat thread for the client

            new ChatThread(clientSocket);
        }
    }
}

