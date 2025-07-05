package codes;

import javafx.application.Platform;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

class ChatServer implements Runnable {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private Thread chatThread;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private String senderId;
    private String recipientId;

    private Object message;

    public ChatServer(int port, String senderId, String recipientId) {
        chatThread = new Thread(this);
        this.senderId = senderId;
        this.recipientId = recipientId;

        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Chat server activated");

        chatThread.start();
    }

    @Override
    public void run() {
        try {
            clientSocket = serverSocket.accept();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            output = new ObjectOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            input = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        new Thread(() -> {
            while (true){
                try {
                    message = input.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

                try (BufferedWriter writer = new BufferedWriter(new FileWriter("database/clients/" + senderId + "/chats/" + recipientId + "/texts.txt", true))) {
                    writer.write(senderId + ":" + message + "\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                try (BufferedWriter writer = new BufferedWriter(new FileWriter("database/clients/" + recipientId + "/chats/" + senderId + "/texts.txt", true))) {
                    writer.write(senderId + ":" + message + "\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        new Thread(() -> {
            BufferedReader reader;

            try {
                reader = new BufferedReader(new FileReader("database/clients/" + senderId + "/chats/" + recipientId +"/texts.txt"));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }

            while (true) {
                Object message;

                try {
                    message = reader.readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if (message != null) {
                    try {
                        output.writeObject(message);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }
}

