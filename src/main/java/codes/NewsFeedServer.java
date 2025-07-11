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
import java.util.concurrent.CountDownLatch;
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
    private volatile boolean running = true;
    private final CountDownLatch serverReadyLatch;

    public NewsFeedServer(int port,CountDownLatch latch) {
        this.port = port;
        this.senderId=senderId;
        this.postThread=new Thread(this);
        this.serverReadyLatch = latch;

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("NewsFeedServer started on port: " + port);

            serverReadyLatch.countDown();
        } catch (IOException e) {
            throw new RuntimeException("Could not start server on port " + port, e);
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


    public int getPort() { return port; }

    @Override
    public void run() {
        try {
            while (running) {
                Socket clientSocket = serverSocket.accept();
                clientPool.submit(new FeedClientHandler(clientSocket));
            }
        } catch (IOException e) {
            if (running) {
                e.printStackTrace();
            } else {
                System.out.println("Server on port " + port + " stopped.");
            }
        } finally {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                    System.out.println("ServerSocket closed on port " + port);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void shutdown() {
        running = false;
        try {
            Server.port[port - 1025] = 0;
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();  // This will interrupt the accept()
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        clientPool.shutdownNow();
        System.out.println("NewsFeedServer on port " + port + " has been shut down.");
    }

    private class FeedClientHandler implements Runnable {
        private final Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        String thisClientId;

        public FeedClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());

                thisClientId = (String) in.readObject();
                System.out.println("Connected client: " + thisClientId);

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
            }finally {
                try {
                    if (out != null) Client.removeClient(out);
                    if (in != null) in.close();
                    if (out != null) out.close();
                    if (socket != null && !socket.isClosed()) socket.close();
                    System.out.println("Closed resources for client: " + thisClientId);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void broadcast(String message) {
        Client.broadcast(message);
    }

}
