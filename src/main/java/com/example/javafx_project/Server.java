package com.example.javafx_project;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class Server {
    static Vector<Client> currentClients;

    public static void main(String[] args) {
        ServerSocket serverSocket;
        Socket clientSocket;

        try {
            serverSocket = new ServerSocket(4349);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        while(true)
        {
            try {
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            new ServerThread(clientSocket);
        }
    }
}
