package codes;

import java.io.*;
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

            if (fromClient instanceof String string && string.startsWith("signup:"))
            {
                Server.clients.add(((String) fromClient).substring("signup".length()));
                System.out.println(Server.clients.size());
            }

            if (fromClient instanceof String string && string.startsWith("login:"))
            {
                this.id = string.substring("login:".length());
                Server.currentClients.put(id, this);
                System.out.println(Server.currentClients.size());
            }

            if (fromClient instanceof String string && string.startsWith("connect_to:"))
            {
                String recipientId = string.substring("connect_to:".length());
                ServerThread sender = Server.currentClients.get(id);
                ServerThread recipient = Server.currentClients.get(recipientId);

                if (recipient == null){
                    System.out.println("Recipient is not active.");
                }

                new ChatServer(Server.port);

                try {
                    sender.output.writeObject("connect:" + Server.port + "," + recipientId);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                try {
                    recipient.output.writeObject("connect:" + Server.port + "," + id);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                Server.port++;

                File chat1 = new File("database/clients/" + id + "/chats/" + recipientId);
                File chat2 = new File("database/clients/" + recipientId + "/chats/" + id);

                if (!chat1.exists()) {
                    chat1.mkdir();

                    File texts = new File("database/clients/" + id + "/chats/" + recipientId + "/texts.txt");
                    File media = new File("database/clients/" + id + "/chats/" + recipientId + "/media");

                    try {
                        texts.createNewFile();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    media.mkdir();
                }

                if (!chat2.exists()) {
                    chat2.mkdir();

                    File texts = new File("database/clients/" + recipientId + "/chats/" + id + "/texts.txt");
                    File media = new File("database/clients/" + recipientId + "/chats/" + id + "/media");

                    try {
                        texts.createNewFile();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    media.mkdir();
                }
            }
        }
    }
}
