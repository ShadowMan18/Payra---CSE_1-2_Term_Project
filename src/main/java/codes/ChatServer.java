package codes;

import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.sql.*;

class ChatServer implements Runnable {
    private int port;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private Thread chatThread;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private final Connection databaseConnection;
    private String senderId;
    private String receiverId;
    private int lastSeenId = 0;

    // Initiating chat server with port, sender id and receiver id

    public ChatServer(int port, String senderId, String receiverId) {
        chatThread = new Thread(this);
        this.port = port;

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

                MessagePacket message = null;
                String filename;

                try {
                    message = (MessagePacket) input.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("Disconnected from chat server");
                    shutdown();
                }

                System.out.println("received:" + message.getSender() + " " + message.getMessage() + " " + message.getFilename());

                if (message != null && message.getFilename() != null) {
                    
                    if (message.getFilename().endsWith("emoji.png")) {
                        filename = message.getFilename();
                    }
                    else {
                        filename = message.getSender() + "_" + message.getFilename();
                    }

                    File mediaDirectory = new File("src/Media Database");

                    if (!mediaDirectory.exists()) {
                        mediaDirectory.mkdir();
                    }

                    File file= new File(mediaDirectory, filename);;

                    try {
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(message.getFiledata());
                        fos.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                else {
                    filename = null;
                }

                try (PreparedStatement addChat = databaseConnection.prepareStatement("INSERT INTO Chats (Sender, Receiver, Content, Media) VALUES (?, ?, ?, ?)")) {
                    addChat.setString(1, senderId);
                    addChat.setString(2, receiverId);
                    addChat.setString(3, message.getMessage());
                    if (filename != null) {
                        addChat.setString(4, filename);
                    }
                    else {
                        addChat.setString(4, null);
                    }

                    addChat.executeUpdate();
                } catch (SQLException e) {
                    break;
                }

                try (PreparedStatement deleteNotification = databaseConnection.prepareStatement("DELETE FROM Notification WHERE Sender = ? AND Receiver = ? AND TYPE = ?")) {
                    deleteNotification.setString(1, senderId);
                    deleteNotification.setString(2, receiverId);
                    deleteNotification.setString(3, "message");

                    deleteNotification.executeUpdate();
                } catch (SQLException e) {
                    break;
                }

                try (PreparedStatement addNotification = databaseConnection.prepareStatement("INSERT INTO Notification (Sender, Receiver, Type, Status) VALUES (?, ?, ?, ?)")) {
                    addNotification.setString(1, senderId);
                    addNotification.setString(2, receiverId);
                    addNotification.setString(3, "message");
                    addNotification.setString(4, "unseen");

                    if (Server.currentClients.get(receiverId) != null) {
                        if (Server.currentClients.get(receiverId).getChatServer() != null) {
                            if (Server.currentClients.get(receiverId).getChatServer().getReceiverId().equals(senderId)) {
                                addNotification.setString(4, "seen");
                            }
                        }
                    }

                    addNotification.executeUpdate();
                } catch (SQLException e) {
                    break;
                }
            }
        }).start();

        // Starting chat conveyor thread (reads chats from the database and sends it to the client)

        new Thread(() -> {
            while (true) {
                try (PreparedStatement fetchChats = databaseConnection.prepareStatement("SELECT * FROM Chats WHERE ((Sender = ? AND Receiver = ?) OR (Sender = ? AND Receiver = ?)) AND id > ? ORDER BY id ASC")) {
                    fetchChats.setString(1, senderId);
                    fetchChats.setString(2, receiverId);
                    fetchChats.setString(3, receiverId);
                    fetchChats.setString(4, senderId);
                    fetchChats.setInt(5, lastSeenId);

                    try (ResultSet chats = fetchChats.executeQuery()) {
                        while (chats.next()) {
                            int id = chats.getInt("id");
                            String sender = chats.getString("Sender");
                            String receiver = chats.getString("Receiver");
                            String content = chats.getString("Content");
                            String filename = chats.getString("Media");
                            Timestamp timestamp = chats.getTimestamp("Timestamp");

                            File mediaFile;
                            byte[] fileBytes = null;

                            if (filename != null) {
                                mediaFile = new File("src/Media Database", filename);
                                fileBytes = Files.readAllBytes(mediaFile.toPath());

                                if (!filename.endsWith("emoji.png")) {
                                    int i;
                                    for (i = 0; i < filename.length(); i++) {
                                        if (filename.charAt(i) == '_') {
                                            break;
                                        }
                                    }

                                    filename = filename.substring(i + 1);
                                }
                            }

                            output.writeObject(new MessagePacket(sender, receiver, content, filename, fileBytes, timestamp.toLocalDateTime()));
                            output.flush();
                            lastSeenId = id;
                        }
                    }

                    Thread.sleep(1000);
                } catch (Exception e) {
                    break;
                }
            }
        }).start();
    }

    public void shutdown() {
        try {
            ClientLocalRepositoryCleaner.clearChatMedia();

            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }

            if (output != null) {
                output.close();
            }

            if (input != null) {
                input.close();
            }

            Server.port.set(port - 1025, 0);
            System.out.println("ChatServer on port " + port + " has been shut down.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getReceiverId() { return receiverId; }
}

