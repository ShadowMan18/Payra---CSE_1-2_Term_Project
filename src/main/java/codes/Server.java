package codes;

import javax.annotation.processing.Filer;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class Server {
    static Vector<String> clients = new Vector<>();
    static Map<String, ServerThread> currentClients = new HashMap<>();
    static int port = 1025;

    public static void main(String[] args) {
        BufferedReader reader;

        try {
             reader = new BufferedReader(new FileReader("database/client_list.txt"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        while (true) {
            String client;

            try {
                 client = reader.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (client == null) break;

            clients.add(client);
        }

        ServerSocket serverSocket;
        Socket clientSocket;

        try {
            serverSocket = new ServerSocket(1024);
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
