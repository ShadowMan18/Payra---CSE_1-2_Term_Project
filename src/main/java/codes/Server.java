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
    static Map<String, ServerThread> currentClients = new HashMap<>();
    public static int[] port = new int[45000];
    public static final Map<String, NewsFeedServer> feedServers = new ConcurrentHashMap<>();

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

        try (Statement statement = databaseConnection.createStatement()){
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS Users (
                    UserId TEXT PRIMARY KEY,
                    First_Name TEXT,
                    Last_Name TEXT,
                    Password TEXT,
                    Question TEXT,
                    Answer TEXT
                )""");

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS Chats (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    Sender TEXT,
                    Receiver TEXT,
                    Content TEXT,
                    Media TEXT,
                    Timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                    )""");

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS Posts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    Author TEXT,
                    Content TEXT,
                    Media TEXT,
                    Timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                    )""");

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS Comments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    PostId INTEGER,
                    Commenter TEXT,
                    Comment TEXT,
                    Timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (PostId) REFERENCES Posts(id) ON DELETE CASCADE
                    )""");

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS Reacts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    PostId INTEGER,
                    Reactor TEXT,
                    ReactType TEXT CHECK(ReactType IN ('like','love','haha','wow','sad','angry')),
                    Timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (PostId) REFERENCES Posts(id) ON DELETE CASCADE
                    )""");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Database created");

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
