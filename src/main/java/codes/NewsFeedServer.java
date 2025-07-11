package codes;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class NewsFeedServer implements Runnable {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private Thread postThread;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private final Connection databaseConnection;
    private String senderId;
    private String receiverId;
    private int lastSeenId = 0;
    private final int port;
    private final ExecutorService clientPool = Executors.newCachedThreadPool();

    public NewsFeedServer(int port) {
        this.port = port;
        this.senderId=senderId;
        this.postThread=new Thread(this);

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("NewsFeedServer started on port: " + port);
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

        postThread.start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                clientPool.submit(new FeedClientHandler(clientSocket));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class FeedClientHandler implements Runnable {
        private final Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        String thisClientId;

        public FeedClientHandler(Socket socket) {
            this.socket = socket;
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());

                thisClientId = (String) in.readObject();
                System.out.println("Connected client: " + thisClientId);

            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException("Error initializing client handler", e);
            }
        }

        @Override
        public void run() {
            try {
                Client.addClient(out);

                System.out.println("Clients connected and added to the array: " + Client.clientListSize());

                //out.writeObject("Welcome to the very very very very best Payra newsfeed!");
                out.flush();

                List<String> posts = new ArrayList<>();
                try (PreparedStatement getPosts = databaseConnection.prepareStatement(
                        "SELECT * FROM Posts ORDER BY Timestamp DESC LIMIT 100")) {
                    try (ResultSet rs = getPosts.executeQuery()) {
                        while (rs.next()) {
                            String author = rs.getString("Author");
                            String content = rs.getString("Content");
                            String time = rs.getString("Timestamp");

                            String pastPost = "[" + time + "] " + author + ": " + content;
                            posts.add(pastPost);
                        }
                    }
                }
                catch (SQLException e) {
                    e.printStackTrace();
                }
                for(int i=posts.size()-1;i>=0;i--){
                    out.writeObject(posts.get(i));
                }
                out.flush();


                //Write my posts in the database!
                while (true) {
                    Object post = in.readObject();
                    if (post instanceof String) {
                        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
                        String finalPost = "[" + timestamp + "] " + thisClientId + ": " + post;

                        // Save to DB
                        try (PreparedStatement insertPost = databaseConnection.prepareStatement(
                                "INSERT INTO Posts (Author, Content, Timestamp) VALUES (?, ?, datetime('now'))")) {
                            insertPost.setString(1, thisClientId);
                            insertPost.setString(2, (String) post);
                            insertPost.executeUpdate();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                        broadcast(finalPost);
                        System.out.println("New post: " + finalPost);
                    }
                }

            } catch (IOException | ClassNotFoundException e) {
                Client.removeClient(out);
                System.out.println("Client disconnected from feed.");
            }
        }
    }

    private void broadcast(String message) {
        Client.broadcast(message);
    }

}
