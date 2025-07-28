package codes;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    static Vector<String> clients = new Vector<>();
    static final Map<String, ServerThread> currentClients = new ConcurrentHashMap<>();
    static Vector<Integer> port = new Vector<>();
    static final Map<String, NewsFeedServer> feedServers = new ConcurrentHashMap<>();
    static Vector<String> inCall = new Vector<>();

    public static void main(String[] args) {
        // Initializing port vector

        for (int i = 0; i < 45000; i++) {
            port.add(0);
        }

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

        try (Statement statement = databaseConnection.createStatement()){
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
                    Timestamp DATETIME DEFAULT (datetime('now', 'localtime'))
                    )""");

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS Notification (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    Sender TEXT,
                    Receiver TEXT,
                    Type TEXT,
                    Status TEXT,
                    Timestamp DATETIME DEFAULT (datetime('now', 'localtime'))
                    )""");

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS Posts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    Author TEXT,
                    Content TEXT,
                    Media TEXT,
                    FileName TEXT,
                    FileData BLOB,
                    Timestamp DATETIME DEFAULT (datetime('now', 'localtime')))
                    """);
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
                FOREIGN KEY (postId) REFERENCES Posts(id)
            );
            """);




        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        File mediaDirectory = new File("src/Media Database");

        if(!mediaDirectory.exists()) {
            mediaDirectory.mkdir();
        }

        System.out.println("Database created");

//        // Delete row (for debugging)
//
//        try (PreparedStatement deleteRow = databaseConnection.prepareStatement("DELETE FROM Users WHERE UserId = 'amartya'")) {
//            deleteRow.executeUpdate();
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }

        // Storing the clients' id in "clients" Vector from clients.txt file

        try (
                PreparedStatement loadClients = databaseConnection.prepareStatement("SELECT * FROM Users");
                ResultSet queryResult = loadClients.executeQuery();
        ) {
            while (queryResult.next()) {
                String userId = queryResult.getString("UserId");
                clients.add(userId);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Initiating the server

        ServerSocket serverSocket;
        Socket clientSocket;

        try {
            serverSocket = new ServerSocket(1024);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Accepting clients

        while(true)
        {
            try {
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Creating new server thread for the client

            new ServerThread(clientSocket);
        }
    }
}
