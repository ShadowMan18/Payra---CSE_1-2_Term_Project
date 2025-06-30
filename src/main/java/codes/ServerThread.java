package codes;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerThread implements Runnable{
    private Thread serverThread;
    private final Socket clientSocket;
    private final ObjectOutputStream output;
    private final ObjectInputStream input;
    private String id;

    // Constructor

    public ServerThread(Socket clientSocket) {
        serverThread = new Thread(this);
        this.clientSocket = clientSocket;

        try {
            this.output = new ObjectOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            this.input = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        serverThread.start();
    }

    @Override
    public void run() {
        while(true)
        {
            Object fromClient = null;

            try {
                fromClient = input.readObject();
            } catch (IOException e) {
                System.out.println("Client connection lost.");
                Server.currentClients.remove(id);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            if (fromClient instanceof String string && string.substring(0,2).equals("1_"))
            {
                Server.clients.add((String) fromClient);
                System.out.println(Server.clients.size());
            }

            if (fromClient instanceof String string && string.substring(0,2).equals("2_"))
            {
                this.id = string.substring(2);
                Server.currentClients.put(id, this);
                System.out.println(Server.currentClients.size());
            }

            if (fromClient instanceof String string && string.substring(0,2).equals("3_"))
            {
                ServerThread sender = Server.currentClients.get(id);
                ServerThread recipient = Server.currentClients.get(string.substring(2));

                if (recipient == null){
                    System.out.println("Recipient is not active.");
                }

                new ChatServer(Server.port);

                try {
                    sender.output.writeObject("Connect:"+Server.port);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                try {
                    recipient.output.writeObject("Connect:"+Server.port);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                Server.port++;
            }
        }
    }
}
