package codes;

import javafx.application.Platform;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;

class ChatServer implements Runnable {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private Thread chatThread;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private final Connection databaseConnection;
    private String senderId;
    private String receiverId;
    private int lastSeenId = 0;

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

        try {
            databaseConnection = DriverManager.getConnection("jdbc:sqlite:src/database.db");

            try (Statement config = databaseConnection.createStatement()) {
                config.execute("PRAGMA busy_timeout = 5000");
                config.execute("PRAGMA journal_mode=WAL");
            }
        } catch (SQLException e) {
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

                try (PreparedStatement addChat = databaseConnection.prepareStatement("INSERT INTO Chats (Sender, Receiver, Content) VALUES (?, ?, ?)")) {
                    addChat.setString(1, senderId);
                    addChat.setString(2, receiverId);
                    addChat.setString(3, (String) message);

                    addChat.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        // Starting chat conveyor thread (reads chats from the chat file and sends it to the client)

        new Thread(() -> {
            while (true) {
                try (PreparedStatement fetchChats = databaseConnection.prepareStatement("SELECT * FROM Chats WHERE ((sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?)) AND id > ? ORDER BY id ASC")) {
                    fetchChats.setString(1, senderId);
                    fetchChats.setString(2, receiverId);
                    fetchChats.setString(3, receiverId);
                    fetchChats.setString(4, senderId);
                    fetchChats.setInt(5, lastSeenId);

                    try (ResultSet chats = fetchChats.executeQuery()) {
                        while (chats.next()) {
                            int id = chats.getInt("id");
                            String sender = chats.getString("sender");
                            String content = chats.getString("content");
                            String timestamp = chats.getString("timestamp");

                            String messageInfo = sender + "," + timestamp + "," + content;

                            output.writeObject(messageInfo);
                            lastSeenId = id;
                        }
                    }

                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}

