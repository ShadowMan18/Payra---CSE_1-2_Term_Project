package codes;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    static Vector<String> clients = new Vector<>();
    static Map<String, ServerThread> currentClients = new HashMap<>();
    static int port = 1025;

    //Maps are better at handling this
    //String is for now we'll make it clients later
    public static ConcurrentHashMap<Client, ServerThread> activeUsers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
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

            new Thread(new ServerThread(clientSocket)).start();
        }
    }
}
