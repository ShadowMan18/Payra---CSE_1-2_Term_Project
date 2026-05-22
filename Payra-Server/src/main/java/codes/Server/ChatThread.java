package codes.Server;

import codes.Wrappers.MessagePacket;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.sql.*;

class ChatThread implements Runnable {
    private Thread chatThread;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private final Connection databaseConnection;
    private String senderId;
    private String receiverId;
    private int lastSeenId = 0;
    private volatile boolean running = true;

    // Initiating chat server with port, sender id and receiver id

    public ChatThread(Socket clientSocket) {
        chatThread = new Thread(this);

        // Initiating the output and input stream to communicate with the client

        try {
            output = new ObjectOutputStream(clientSocket.getOutputStream());
            input = new ObjectInputStream(clientSocket.getInputStream());
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

        try {
            senderId = (String) input.readObject();
            receiverId = (String) input.readObject();
            ChatServer.receiver.put(senderId, receiverId);
            System.out.println("I got the senderId:" + senderId);
            System.out.println("I got the receiverId:" + receiverId);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        ChatServer.chatThreads.put(senderId, this);

        chatThread.start();
    }

    public synchronized void sendToClient(Object obj) {
        try {
            output.writeObject(obj);
            output.flush();
        } catch (IOException e) {

        }
    }

    public synchronized void stop() {
        running = false;
    }

    @Override
    public void run() {
        // Starting message writer thread (receives message from the client and sends it to the receiver and writes it in the database)

        new Thread(() -> {
            while (running){
                // Receiving message

                MessagePacket message = null;
                String filename;

                try {
                    message = (MessagePacket) input.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    running = false;
                    System.out.println("Disconnected from chat server and marked running false");
                    break;
                }

                if(message == null) {
                    continue;
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
                    if (message.getMessage() != null) {
                        addChat.setString(3, EncryptionProcessor.encrypt(message.getMessage()));
                    }
                    else {
                        addChat.setString(3, message.getMessage());
                    }
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

                    if (MainServer.currentClients.get(receiverId) != null) {
                        if (ChatServer.receiver.get(receiverId) != null && ChatServer.receiver.get(receiverId).equals(senderId)) {
                            addNotification.setString(4, "seen");
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
            while (running) {
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
                            if (content != null) {
                                content = EncryptionProcessor.decrypt(content);
                            }
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

                            sendToClient(new MessagePacket(sender, receiver, content, filename, fileBytes, timestamp.toLocalDateTime()));
                            lastSeenId = id;
                        }
                    }

                    Thread.sleep(1000);
                } catch (Exception e) {
                    break;
                }
            }
        }).start();

        // Starting active status checker thread

        new Thread(() -> {
            while(running) {
                if (MainServer.currentClients.get(receiverId) != null) {
                    sendToClient("active");
                }
                else {
                    sendToClient("not_active");
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }
}