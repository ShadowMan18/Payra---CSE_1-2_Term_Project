package codes;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

public class ServerThread implements Runnable{
    private Thread serverThread;
    private final ObjectOutputStream output;
    private final ObjectInputStream input;
    private String id;

    // Constructor

    public ServerThread(Socket clientSocket) {
        serverThread = new Thread(this);

        // Initiating the output and input stream to communicate with the client

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
        // Receiving instructions from the client and sending feedbacks

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

            if (fromClient instanceof String string && string.startsWith("m:"))
            {
                System.out.println((String) fromClient);
            }

            if (fromClient instanceof String string && string.startsWith("signup:"))
            {
                String clientInfo = string.substring("signup:".length());

                this.id = clientInfo.split(",")[0];
                Server.clients.add(id);

                File clientDirectory = new File("database/clients/" + id);
                clientDirectory.mkdir();

                File info = new File("database/clients/" + id +"/info.txt");

                try {
                    info.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                File friends = new File("database/clients/" + id +"/friends.txt");

                try {
                    friends.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                File uploads = new File("database/clients/" + id +"/uploads");
                uploads.mkdir();

                File chats = new File("database/clients/" + id +"/chats");
                chats.mkdir();

                try (BufferedWriter writer = new BufferedWriter(new FileWriter("database/clients/" + id +"/info.txt"))) {
                    writer.write(clientInfo);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                try (BufferedWriter writer = new BufferedWriter(new FileWriter("database/client_list.txt", true))) {
                    writer.write(id + "\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

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
