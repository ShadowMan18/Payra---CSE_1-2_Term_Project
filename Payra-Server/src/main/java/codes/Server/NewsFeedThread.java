package codes.Server;

import codes.Wrappers.CommentPacket;
import codes.Wrappers.PostPacket;

import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Date;

public class NewsFeedThread implements Runnable {

    private final Thread feedThread;
    private final ObjectOutputStream output;
    private final ObjectInputStream input;
    private final Connection databaseConnection;
    private String clientId;
    private volatile boolean running = true;

    // -----------------------------------------------------------------------
    // Constructor — called by NewsFeedServer for every accepted socket
    // -----------------------------------------------------------------------

    public NewsFeedThread(Socket clientSocket) {
        try {
            // Output stream MUST be created before input stream (both sides must do this)
            this.output = new ObjectOutputStream(clientSocket.getOutputStream());
            this.output.flush();
            this.input  = new ObjectInputStream(clientSocket.getInputStream());
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

        feedThread = new Thread(this);
        feedThread.start();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    public synchronized void sendToClient(Object obj) {
        try {
            output.writeObject(obj);
            output.flush();
        } catch (IOException e) {
            // client disconnected; run() will handle cleanup
        }
    }

    // -----------------------------------------------------------------------
    // Main loop
    // -----------------------------------------------------------------------

    @Override
    public void run() {
        try {
            // First message from client is always their userId
            clientId = (String) input.readObject();
            System.out.println("NewsFeedThread: client connected — " + clientId);

            NewsFeedServer.addClient(output);
            System.out.println("NewsFeedThread: total feed clients = " + NewsFeedServer.clientListSize());

            // Send existing posts (own posts + friends' posts, newest first → display oldest first)
            sendInitialPosts();

            // Main message loop
            while (running) {
                Object incoming;
                try {
                    incoming = input.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    break;
                }

                if (incoming instanceof PostPacket packet) {
                    handleIncomingPostPacket(packet);
                }
                else if (incoming instanceof String message) {
                    handleStringMessage(message);
                }
                else if (incoming instanceof CommentPacket comment) {
                    handleCommentPacket(comment);
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("NewsFeedThread: connection lost for " + clientId);
        } finally {
            NewsFeedServer.removeClient(output);
            System.out.println("NewsFeedThread: removed client " + clientId
                    + " — remaining = " + NewsFeedServer.clientListSize());
            try { input.close();  } catch (IOException ignored) {}
            try { output.close(); } catch (IOException ignored) {}
        }
    }

    // -----------------------------------------------------------------------
    // Initial post load
    // -----------------------------------------------------------------------

    private void sendInitialPosts() {
        List<PostPacket> posts = new ArrayList<>();

        try (PreparedStatement getPosts = databaseConnection.prepareStatement("""
                SELECT * FROM Posts
                WHERE Author = ?
                   OR Author IN (
                       SELECT user2 FROM Friends WHERE user1 = ?
                       UNION
                       SELECT user1 FROM Friends WHERE user2 = ?
                   )
                ORDER BY Timestamp DESC
                LIMIT 100
                """)) {
            getPosts.setString(1, clientId);
            getPosts.setString(2, clientId);
            getPosts.setString(3, clientId);

            try (ResultSet rs = getPosts.executeQuery()) {
                while (rs.next()) {
                    int    postId   = rs.getInt("id");
                    String author   = rs.getString("Author");
                    String content  = rs.getString("Content");
                    String time     = rs.getString("Timestamp");
                    String fileName = rs.getString("FileName");
                    byte[] fileData = rs.getBytes("FileData");

                    Map<String, Integer> reactionMap = new HashMap<>();
                    String userReacted = "none";

                    try (PreparedStatement getReacts = databaseConnection.prepareStatement(
                            "SELECT ReactType, COUNT(*) as count FROM Reacts WHERE PostId = ? GROUP BY ReactType")) {
                        getReacts.setInt(1, postId);
                        try (ResultSet reactSet = getReacts.executeQuery()) {
                            while (reactSet.next()) {
                                reactionMap.put(reactSet.getString("ReactType"), reactSet.getInt("count"));
                            }
                        }
                    }

                    try (PreparedStatement getUserReaction = databaseConnection.prepareStatement(
                            "SELECT ReactType FROM Reacts WHERE PostId = ? AND Reactor = ?")) {
                        getUserReaction.setInt(1, postId);
                        getUserReaction.setString(2, clientId);
                        try (ResultSet rsUser = getUserReaction.executeQuery()) {
                            if (rsUser.next()) userReacted = rsUser.getString("ReactType");
                        }
                    }

                    List<String> comments = new ArrayList<>();
                    try (PreparedStatement commentStmt = databaseConnection.prepareStatement(
                            "SELECT commenter, comment FROM Comments WHERE postId = ? ORDER BY timestamp ASC")) {
                        commentStmt.setInt(1, postId);
                        try (ResultSet commentRs = commentStmt.executeQuery()) {
                            while (commentRs.next()) {
                                comments.add(commentRs.getString("commenter") + ": " + commentRs.getString("comment"));
                            }
                        }
                    }

                    posts.add(new PostPacket(
                            postId, author, content, fileName, fileData,
                            Timestamp.valueOf(time).toLocalDateTime(),
                            reactionMap, userReacted, comments
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Send oldest-first so the client displays them in chronological order
        for (int i = posts.size() - 1; i >= 0; i--) {
            sendToClient(posts.get(i));
        }
    }

    // -----------------------------------------------------------------------
    // String message handling (reactions + plain-text posts)
    // -----------------------------------------------------------------------

    private void handleStringMessage(String message) {
        if (message.startsWith("REACT:")) {
            // Format: "REACT:<postId>:<newType>"
            String[] parts = message.split(":", 3);
            if (parts.length != 3) return;

            int    postId  = Integer.parseInt(parts[1]);
            String newType = parts[2];
            String oldType = getOldReactionType(postId, clientId);

            try {
                try (PreparedStatement deleteStmt = databaseConnection.prepareStatement(
                        "DELETE FROM Reacts WHERE PostId = ? AND Reactor = ?")) {
                    deleteStmt.setInt(1, postId);
                    deleteStmt.setString(2, clientId);
                    deleteStmt.executeUpdate();
                }

                if (!newType.equals(oldType)) {
                    try (PreparedStatement insertStmt = databaseConnection.prepareStatement(
                            "INSERT INTO Reacts (PostId, Reactor, ReactType) VALUES (?, ?, ?)")) {
                        insertStmt.setInt(1, postId);
                        insertStmt.setString(2, clientId);
                        insertStmt.setString(3, newType);
                        insertStmt.executeUpdate();
                    }
                    System.out.println("Reaction updated: " + clientId + " -> " + newType);
                } else {
                    System.out.println("Reaction removed (toggle off): " + clientId);
                    newType = "none";
                }

                NewsFeedServer.broadcast("REACTION|" + postId + "|" + clientId + "|" + oldType + "|" + newType);

            } catch (SQLException e) {
                e.printStackTrace();
            }

        } else {
            // Plain-text post (legacy string path)
            try (PreparedStatement insertPost = databaseConnection.prepareStatement(
                    "INSERT INTO Posts (Author, Content, Timestamp) VALUES (?, ?, datetime('now'))",
                    Statement.RETURN_GENERATED_KEYS)) {
                insertPost.setString(1, clientId);
                insertPost.setString(2, message);
                insertPost.executeUpdate();

                try (ResultSet generatedKeys = insertPost.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int    postId    = generatedKeys.getInt(1);
                        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
                        String finalPost = postId + "|" + timestamp + "|" + clientId + "|" + message;
                        NewsFeedServer.broadcast(finalPost);
                        System.out.println("New text post (ID: " + postId + "): " + finalPost);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // -----------------------------------------------------------------------
    // PostPacket handling (posts with optional file attachments)
    // -----------------------------------------------------------------------

    public void handleIncomingPostPacket(PostPacket packet) {
        try {
            int postId = savePostToDB(packet);
            if (postId != -1) {
                PostPacket broadcastPacket = new PostPacket(
                        postId, packet.getAuthor(), packet.getContent(),
                        packet.getFileName(), packet.getFileData(),
                        LocalDateTime.now(), new HashMap<>(), "none", new ArrayList<>()
                );
                NewsFeedServer.broadcast(broadcastPacket);
                System.out.println("Broadcasted post (ID: " + postId + ") by " + packet.getAuthor());
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
                if (keys.next()) postId = keys.getInt(1);
            }
        }
        return postId;
    }

    // -----------------------------------------------------------------------
    // CommentPacket handling (received directly from the feed socket)
    // -----------------------------------------------------------------------

    private void handleCommentPacket(CommentPacket comment) {
        try (PreparedStatement stmt = databaseConnection.prepareStatement(
                "INSERT INTO Comments (postId, commenter, comment) VALUES (?, ?, ?)")) {
            stmt.setInt(1, comment.getPostId());
            stmt.setString(2, comment.getCommenter());
            stmt.setString(3, comment.getCommentText());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------------------------
    // Utilities
    // -----------------------------------------------------------------------

    private String getOldReactionType(int postId, String reactor) {
        try (PreparedStatement stmt = databaseConnection.prepareStatement(
                "SELECT ReactType FROM Reacts WHERE PostId = ? AND Reactor = ?")) {
            stmt.setInt(1, postId);
            stmt.setString(2, reactor);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("ReactType");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "none";
    }
}