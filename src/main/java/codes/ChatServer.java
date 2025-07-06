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
    private String receiverId;

    private Object message;
    
    // Initiating chat server with port, sender id and receiver id

    public ChatServer(int port, String senderId, String receiverId) {
        chatThread = new Thread(this);
        this.senderId = senderId;
        this.receiverId = receiverId;

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
        // Connecting the client

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

        // Starting message writer thread (receives message from the client and writes it in the chat files of both the sender and receiver)

        new Thread(() -> {
            while (true){
                // Receiving message

                try {
                    message = input.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

                // Writing message in sender's chat file with the receiver

                try (BufferedWriter writer = new BufferedWriter(new FileWriter("database/clients/" + senderId + "/chats/" + receiverId + "/texts.txt", true))) {
                    writer.write(senderId + ":" + message + "\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                // Writes message in the receiver's chat file with the sender

                try (BufferedWriter writer = new BufferedWriter(new FileWriter("database/clients/" + receiverId + "/chats/" + senderId + "/texts.txt", true))) {
                    writer.write(senderId + ":" + message + "\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        // Starting chat conveyor thread (reads chats from the chat file and sends it to the client)

        new Thread(() -> {
            BufferedReader reader;

            try {
                reader = new BufferedReader(new FileReader("database/clients/" + senderId + "/chats/" + receiverId +"/texts.txt"));
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

