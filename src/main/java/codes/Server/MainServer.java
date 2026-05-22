package codes.Server;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class MainServer {
    static Vector<String> clients = new Vector<>();
    static final Map<String, MainServerThread> currentClients = new ConcurrentHashMap<>();
    static Vector<String> inCall = new Vector<>();

    public static void main(String[] args) {

        // Creating database if it isn't created

        Connection databaseConnection;

        try {
            databaseConnection = DriverManager.getConnection("jdbc:sqlite:src/database.db");
            try (Statement config = databaseConnection.createStatement()) {
                config.execute("PRAGMA busy_timeout = 5000");
                config.execute("PRAGMA journal_mode=WAL");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        try (Statement statement = databaseConnection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS Users (
                    UserId TEXT PRIMARY KEY,
                    First_Name TEXT,
                    Last_Name TEXT,
                    Password TEXT,
                    Question TEXT,
                    Answer TEXT,
                    Profile_Picture TEXT
                )""");

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS Chats (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    Sender TEXT,
                    Receiver TEXT,
                    Content TEXT,
                    Media TEXT,
                    Timestamp DATETIME DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (Sender) REFERENCES Users (UserId) ON DELETE CASCADE,
                    FOREIGN KEY (Receiver) REFERENCES Users (UserId) ON DELETE CASCADE
                    )""");

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS Notification (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    Sender TEXT,
                    Receiver TEXT,
                    Type TEXT,
                    Status TEXT,
                    Timestamp DATETIME DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (Sender) REFERENCES Users (UserId) ON DELETE CASCADE,
                    FOREIGN KEY (Receiver) REFERENCES Users (UserId) ON DELETE CASCADE
                    )""");

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS Posts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    Author TEXT,
                    Content TEXT,
                    Media TEXT,
                    FileName TEXT,
                    FileData BLOB,
                    Timestamp DATETIME DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (Author) REFERENCES Users (UserId) ON DELETE CASCADE
                    )""");

            statement.execute("""
            CREATE TABLE IF NOT EXISTS Reacts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                PostId INTEGER,
                Reactor TEXT,
                ReactType TEXT CHECK(ReactType IN ('like','love','sad')),
                Timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(PostId, Reactor),
                FOREIGN KEY (PostId) REFERENCES Posts(id) ON DELETE CASCADE
            )
            """);

            statement.execute("""
            CREATE TABLE IF NOT EXISTS FriendRequests (
                sender TEXT,
                receiver TEXT,
                status TEXT CHECK(status IN ('pending', 'accepted', 'rejected')) DEFAULT 'pending',
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (sender, receiver),
                FOREIGN KEY (sender) REFERENCES Users(UserId) ON DELETE CASCADE,
                FOREIGN KEY (receiver) REFERENCES Users(UserId) ON DELETE CASCADE
                )
            """);

            statement.execute("""
            CREATE TABLE IF NOT EXISTS Friends (
                user1 TEXT,
                user2 TEXT,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (user1, user2),
                FOREIGN KEY (user1) REFERENCES Users(UserId) ON DELETE CASCADE,
                FOREIGN KEY (user2) REFERENCES Users(UserId) ON DELETE CASCADE
                )
            """);

            statement.execute("""
            CREATE TABLE IF NOT EXISTS Comments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                postId INTEGER NOT NULL,
                commenter TEXT NOT NULL,
                comment TEXT NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (postId) REFERENCES Posts(id) ON DELETE CASCADE
            );
            """);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        File mediaDirectory = new File("src/Media Database");
        if (!mediaDirectory.exists()) {
            mediaDirectory.mkdir();
        }

        System.out.println("Database created");

        // Load existing clients from DB

        try (
                PreparedStatement loadClients = databaseConnection.prepareStatement("SELECT * FROM Users");
                ResultSet queryResult = loadClients.executeQuery()
        ) {
            while (queryResult.next()) {
                clients.add(queryResult.getString("UserId"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Start the main TCP server

        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(4349);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Start the chat server (port 4350)
        new ChatServer();

        // Start the news-feed server (port 4351) — single instance, spawns threads per client
        new NewsFeedServer();

        // Accept main-channel connections
        new Thread(() -> {
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    new MainServerThread(clientSocket);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        // UDP IP discovery

        DatagramSocket serverDatagramSocket;
        try {
            serverDatagramSocket = new DatagramSocket(22222);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        new Thread(() -> {
            byte[] buffer = new byte[256];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    serverDatagramSocket.receive(packet);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                String message = new String(packet.getData(), 0, packet.getLength());
                if (message.equals("DISCOVER_SERVER")) {
                    String response;
                    try {
                        response = "SERVER:" + InetAddress.getLocalHost().getHostAddress();
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                    byte[] responseBytes = response.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(
                            responseBytes, responseBytes.length, packet.getAddress(), packet.getPort());
                    try {
                        serverDatagramSocket.send(responsePacket);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }
}