package codes;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    static Vector<Client> currentClients = new Vector<>();

    //Maps are better at handling this
    //String is for now we'll make it clients later
    public static ConcurrentHashMap<Client, ServerThread> activeUsers = new ConcurrentHashMap<>();

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

            new Thread(new ServerThread(clientSocket)).start();
        }
    }
}
