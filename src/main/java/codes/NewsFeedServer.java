package codes;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class NewsFeedServer implements Runnable {
    private int port;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private Thread postThread;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private final Connection databaseConnection;
    private String senderId;
    private String receiverId;
    private int lastSeenId = 0;
    private final ExecutorService clientPool = Executors.newCachedThreadPool();
    private volatile boolean running = true;
    private final CountDownLatch serverReadyLatch;
    private final Map<Integer, String> userReactionsByPostId = new HashMap<>();

    public NewsFeedServer(int port,CountDownLatch latch) {
        this.postThread=new Thread(this);
        this.port = port;

        this.senderId=senderId;
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
            System.out.println("Shut down NewsFeedServer at port " + port);
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

                //System.out.println("This is me trying to read the posts on start up from db");
                List<PostPacket> posts = new ArrayList<>();

                try (PreparedStatement getPosts = databaseConnection.prepareStatement(
                        """
                        SELECT * FROM Posts
                        WHERE Author = ?
                           OR Author IN (
                               SELECT user2 FROM Friends WHERE user1 = ?
                               UNION
                               SELECT user1 FROM Friends WHERE user2 = ?
                           )
                        ORDER BY Timestamp DESC
                        LIMIT 100
                        """
                )) {
                    getPosts.setString(1, thisClientId); // own posts
                    getPosts.setString(2, thisClientId); // friends (forward)
                    getPosts.setString(3, thisClientId); // friends (reverse)

                    try (ResultSet rs = getPosts.executeQuery()) {
                        while (rs.next()) {
                            int postId = rs.getInt("id");
                            String author = rs.getString("Author");
                            String content = rs.getString("Content");
                            String time = rs.getString("Timestamp");

                            String fileName = rs.getString("FileName");
                            byte[] fileData = rs.getBytes("FileData");

                            Map<String, Integer> reactionMap = new HashMap<>();
                            String userReacted = "none";


                            try (PreparedStatement getReacts = databaseConnection.prepareStatement(
                                    "SELECT ReactType, COUNT(*) as count FROM Reacts WHERE PostId = ? GROUP BY ReactType")) {
                                getReacts.setInt(1, postId);
                                try (ResultSet reactSet = getReacts.executeQuery()) {
                                    while (reactSet.next()) {
                                        String type = reactSet.getString("ReactType");
                                        int count = reactSet.getInt("count");
                                        reactionMap.put(type, count);
                                    }
                                }
                            }

                            try (PreparedStatement getUserReaction = databaseConnection.prepareStatement(
                                    "SELECT ReactType FROM Reacts WHERE PostId = ? AND Reactor = ?")) {
                                getUserReaction.setInt(1, postId);
                                getUserReaction.setString(2, thisClientId);
                                try (ResultSet rsUser = getUserReaction.executeQuery()) {
                                    if (rsUser.next()) {
                                        userReacted = rsUser.getString("ReactType");
                                    }
                                }
                            }

                            List<String> comments = new ArrayList<>();
                            try (PreparedStatement commentStmt = databaseConnection.prepareStatement(
                                    "SELECT commenter, comment FROM Comments WHERE postId = ? ORDER BY timestamp ASC")) {
                                commentStmt.setInt(1, postId);
                                try (ResultSet commentRs = commentStmt.executeQuery()) {
                                    while (commentRs.next()) {
                                        String commenter = commentRs.getString("commenter");
                                        String commentText = commentRs.getString("comment");
                                        comments.add(commenter + ": " + commentText);
                                    }
                                }
                            }

                            PostPacket packet = new PostPacket(
                                    postId,
                                    author,
                                    content,
                                    fileName,
                                    fileData,
                                    Timestamp.valueOf(time).toLocalDateTime(),
                                    reactionMap,
                                    userReacted,
                                    comments
                            );

                            posts.add(packet);


                           // System.out.println("Post packet has been created and added");
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }


                for (int i = posts.size() - 1; i >= 0; i--) {
                    out.writeObject(posts.get(i));
                }
                out.flush();

                //System.out.println("We are done with loading the posts");


                while (true) {
                    Object incoming = in.readObject();
                    if (incoming instanceof PostPacket packet) {
                        handleIncomingPostPacket(packet);
                    }
                    if (incoming instanceof String message) {

                        if (message.startsWith("REACT:")) {
                            String[] parts = message.split(":", 3);
                            if (parts.length == 3) {
                                int postId = Integer.parseInt(parts[1]);
                                String newType = parts[2];

                                String oldType = getOldReactionType(postId, thisClientId);

                                try {

                                    try (PreparedStatement deleteStmt = databaseConnection.prepareStatement(
                                            "DELETE FROM Reacts WHERE PostId = ? AND Reactor = ?")) {
                                        deleteStmt.setInt(1, postId);
                                        deleteStmt.setString(2, thisClientId);
                                        deleteStmt.executeUpdate();
                                    }

                                    if (!newType.equals(oldType)) {

                                        try (PreparedStatement insertStmt = databaseConnection.prepareStatement(
                                                "INSERT INTO Reacts (PostId, Reactor, ReactType) VALUES (?, ?, ?)")) {
                                            insertStmt.setInt(1, postId);
                                            insertStmt.setString(2, thisClientId);
                                            insertStmt.setString(3, newType);
                                            insertStmt.executeUpdate();
                                        }

                                        System.out.println("Reaction updated: " + thisClientId + " -> " + newType);
                                    } else {
                                        System.out.println("Reaction removed (toggle off): " + thisClientId);
                                        newType = "none";
                                    }


                                    broadcastReaction(postId, thisClientId, oldType, newType);


                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        else {
                            try (PreparedStatement insertPost = databaseConnection.prepareStatement(
                                    "INSERT INTO Posts (Author, Content, Timestamp) VALUES (?, ?, datetime('now'))",
                                    Statement.RETURN_GENERATED_KEYS)) {

                                insertPost.setString(1, thisClientId);
                                insertPost.setString(2, message);
                                insertPost.executeUpdate();

                                try (ResultSet generatedKeys = insertPost.getGeneratedKeys()) {
                                    if (generatedKeys.next()) {
                                        int postId = generatedKeys.getInt(1);
                                        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
                                        String finalPost = postId + "|" + timestamp + "|" + thisClientId + "|" + message;

                                        broadcast(finalPost);
                                        System.out.println("New post (ID: " + postId + "): " + finalPost);
                                    }
                                }

                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                    if (incoming instanceof CommentPacket comment) {
                        System.out.println("Yes I got it for the db");
                        try (PreparedStatement stmt = databaseConnection.prepareStatement(
                                "INSERT INTO Comments (postId, commenter, comment) VALUES (?, ?, ?)")) {
                            stmt.setInt(1, comment.getPostId());
                            stmt.setString(2, comment.getCommenter());
                            stmt.setString(3, comment.getCommentText());
                            stmt.executeUpdate();

                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }


                    }


                }

            } catch (IOException | ClassNotFoundException e) {
                Client.removeClient(out);
                System.out.println("Client disconnected from feed.");
            } finally {
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

    private void broadcastPost(PostPacket post) {
        Client.broadcast(post);
    }


    public void broadcastReaction(int postId, String reactor, String oldType, String newType) {
        String message = "REACTION|" + postId + "|" + reactor + "|" + oldType + "|" + newType;
        broadcast(message);
    }

    public static void broadcastToAll(int postId, String reactor, String oldType, String newType) {
        String message = "REACTION|" + postId + "|" + reactor + "|" + oldType + "|" + newType;
        Client.broadcast(message);
    }



    private String getOldReactionType(int postId, String reactor) {
        try (PreparedStatement stmt = databaseConnection.prepareStatement(
                "SELECT ReactType FROM Reacts WHERE PostId = ? AND Reactor = ?")) {
            stmt.setInt(1, postId);
            stmt.setString(2, reactor);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("ReactType");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "none";
    }

    public void handleIncomingPostPacket(PostPacket packet) {
        try {
            int postId = savePostToDB(packet);

            if (postId != -1) {
                PostPacket broadcastPacket = new PostPacket(
                        postId,
                        packet.getAuthor(),
                        packet.getContent(),
                        packet.getFileName(),
                        packet.getFileData(),
                        LocalDateTime.now(),
                        new HashMap<>(),
                        "none",
                        new ArrayList<>()
                );

                broadcastPost(broadcastPacket);
                System.out.println("Broadcasted post (with file): " + postId + " by " + packet.getAuthor());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }




    private int savePostToDB(PostPacket packet) throws SQLException {
        int postId = -1;
        try (PreparedStatement stmt = databaseConnection.prepareStatement(
                "INSERT INTO Posts (Author, Content, FileName, FileData, Timestamp) VALUES (?, ?, ?, ?, datetime('now'))",
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, packet.getAuthor());
            stmt.setString(2, packet.getContent());
            stmt.setString(3, packet.getFileName());
            stmt.setBytes(4, packet.getFileData());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    postId = keys.getInt(1);
                }
            }
        }
        return postId;
    }



}