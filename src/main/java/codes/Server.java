package codes;

import java.io.IOException;
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
