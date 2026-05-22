package codes.Server;

import codes.Wrappers.*;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class MainServerThread implements Runnable {
    private Thread serverThread;
    private final ObjectOutputStream output;
    private final ObjectInputStream input;
    private final String clientIPAddress;
    private final Connection databaseConnection;
    private String id;
    private CountDownLatch latch;

    public MainServerThread(Socket clientSocket) {
        serverThread = new Thread(this);

        try {
            this.output = new ObjectOutputStream(clientSocket.getOutputStream());
            this.input  = new ObjectInputStream(clientSocket.getInputStream());
            this.clientIPAddress = clientSocket.getInetAddress().getHostAddress();
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

        serverThread.start();
    }

    public synchronized void sendToClient(Object obj) {
        try {
            output.writeObject(obj);
            output.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        while (true) {
            Object fromClient;

            try {
                fromClient = input.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Client connection lost.");
                if (id != null) MainServer.currentClients.remove(id);
                break;
            }

            if (fromClient instanceof String string && string.equals("done:")) {
                if (latch != null) { latch.countDown(); latch = null; }
            }

            // Check if a client id exists
            if (fromClient instanceof String string && string.startsWith("check:")) {
                String checkId = string.substring("check:".length());
                this.id = checkId;
                System.out.println(checkId);
                System.out.println(MainServer.clients.contains(checkId));
                sendToClient(MainServer.clients.contains(checkId));
            }

            // Sign up
            if (fromClient instanceof ClientInfo clientInfo) {
                this.id = clientInfo.getId();
                MainServer.clients.add(id);

                try (PreparedStatement addUser = databaseConnection.prepareStatement(
                        "INSERT INTO Users (UserId, First_Name, Last_Name, Password, Question, Answer, Profile_Picture) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    addUser.setString(1, clientInfo.getId());
                    addUser.setString(2, clientInfo.getFirstName());
                    addUser.setString(3, clientInfo.getLastName());
                    addUser.setString(4, EncryptionProcessor.encrypt(clientInfo.getPassword()));
                    addUser.setString(5, EncryptionProcessor.encrypt(clientInfo.getRecoveryQuestion()));
                    addUser.setString(6, EncryptionProcessor.encrypt(clientInfo.getRecoveryAnswer()));
                    addUser.setString(7, "DefaultProfilePicture.png");
                    addUser.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                File mediaDirectory = new File("src/Media Database");
                if (!mediaDirectory.exists()) mediaDirectory.mkdir();
                File mediaFile = new File(mediaDirectory, "DefaultProfilePicture.png");
                try {
                    FileOutputStream fos = new FileOutputStream(mediaFile);
                    fos.write(clientInfo.getProfilePicture());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                sendToClient("signup_successful");

                String message = "Dear " + clientInfo.getFirstName() + ",\n\n"
                        + "Thanks for signing up in Payra. May all your words find their destinations. "
                        + "We'd love to hear your feedback!. Your support is our inspiration.\n"
                        + "Welcome to the community!\n\nWith love,\n- Team Payra";
                EmailSender.sendEmail(id + "@gmail.com", "Welcome to Payra!", message);
            }

            // Profile picture bytes
            if (fromClient instanceof byte[] imageBytes) {
                InputStream is = new ByteArrayInputStream(imageBytes);
                ImageInputStream iis;
                try { iis = ImageIO.createImageInputStream(is); } catch (IOException e) { throw new RuntimeException(e); }
                Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
                String format = "png";
                if (readers.hasNext()) {
                    try { format = readers.next().getFormatName().toLowerCase(); } catch (IOException e) { throw new RuntimeException(e); }
                }
                BufferedImage bufferedImage;
                try { bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes)); } catch (IOException e) { throw new RuntimeException(e); }
                String filename = id + "_ProfilePicture." + format;
                File mediaDirectory = new File("src/Media Database");
                if (!mediaDirectory.exists()) mediaDirectory.mkdir();
                File file = new File(mediaDirectory, filename);
                try { ImageIO.write(bufferedImage, format, file); } catch (IOException e) { throw new RuntimeException(e); }
                try (PreparedStatement addProfilePicture = databaseConnection.prepareStatement(
                        "UPDATE Users SET Profile_Picture = ? WHERE UserId = ?")) {
                    addProfilePicture.setString(1, filename);
                    addProfilePicture.setString(2, id);
                    addProfilePicture.executeUpdate();
                } catch (SQLException e) { throw new RuntimeException(e); }
                sendToClient("profile_picture_set");
            }

            // Get client info
            if (fromClient instanceof String string && string.startsWith("get_info:")) {
                String infoId = string.substring("get_info:".length());
                sendToClient(getClientInfo(infoId));
            }

            // Login
            if (fromClient instanceof String string && string.startsWith("login:")) {
                this.id = string.substring("login:".length());
                MainServer.currentClients.put(id, this);
                sendToClient("login_successful");

                // Notification polling thread
                new Thread(() -> {
                    int lastNotifId = 0;
                    while (true) {
                        try (PreparedStatement getNotification = databaseConnection.prepareStatement(
                                "SELECT * FROM Notification WHERE Receiver = ? AND id > ? ORDER BY id ASC")) {
                            getNotification.setString(1, id);
                            getNotification.setInt(2, lastNotifId);
                            try (ResultSet notifications = getNotification.executeQuery()) {
                                while (notifications.next()) {
                                    sendToClient("notif:" + notifications.getString("Sender")
                                            + "," + notifications.getString("Type")
                                            + "," + notifications.getString("Status"));
                                    lastNotifId = notifications.getInt("id");
                                }
                            }
                            Thread.sleep(1000);
                        } catch (SQLException | InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                }).start();

                System.out.println(this.id + " logged in");
            }

            // Logout
            if (fromClient instanceof String string && string.startsWith("logout:")) {
                String logoutId = string.substring("logout:".length());
                MainServer.currentClients.remove(logoutId);
                MainServer.inCall.remove(logoutId);
            }

            // Update profile
            if (fromClient instanceof String string && string.startsWith("update:")) {
                String[] updateInfo = string.substring("update:".length()).split(",");
                if (updateInfo[0].equals("name")) {
                    try (PreparedStatement updateName = databaseConnection.prepareStatement(
                            "UPDATE Users SET First_Name = ?, Last_Name = ? WHERE UserId = ?")) {
                        updateName.setString(1, updateInfo[1]);
                        updateName.setString(2, updateInfo[2]);
                        updateName.setString(3, id);
                        updateName.executeUpdate();
                    } catch (SQLException e) { throw new RuntimeException(e); }
                } else if (updateInfo[0].equals("password")) {
                    try (PreparedStatement updatePassword = databaseConnection.prepareStatement(
                            "UPDATE Users SET Password = ? WHERE UserId = ?")) {
                        updatePassword.setString(1, EncryptionProcessor.encrypt(updateInfo[1]));
                        updatePassword.setString(2, id);
                        updatePassword.executeUpdate();
                    } catch (SQLException e) { throw new RuntimeException(e); }
                }
                sendToClient("updated");
            }

            // Load all clients
            if (fromClient instanceof String string && string.equals("load_clients")) {
                Vector<ClientInfo> clientInfo = new Vector<>();
                for (String cid : MainServer.clients) clientInfo.add(getClientInfo(cid));
                System.out.println("Loading clients");
                sendToClient(clientInfo);
            }

            // Chat routing
            if (fromClient instanceof String string && string.startsWith("chat_with:")) {
                String receiverId = string.substring("chat_with:".length());
                sendToClient("connect_to:" + receiverId);
            }

            if (fromClient instanceof String string && string.equals("close_chat")) {
                sendToClient("chat_closed");
            }

            // Mark notification seen
            if (fromClient instanceof String string && string.startsWith("seen:")) {
                String[] notificationInfo = string.substring("seen:".length()).split(",");
                String sender   = notificationInfo[0];
                String receiver = notificationInfo[1];
                try (PreparedStatement updateNotificationStatus = databaseConnection.prepareStatement(
                        "UPDATE Notification SET Status = 'seen' WHERE Sender = ? AND Receiver = ?")) {
                    updateNotificationStatus.setString(1, sender);
                    updateNotificationStatus.setString(2, receiver);
                    updateNotificationStatus.executeUpdate();
                } catch (SQLException e) { throw new RuntimeException(e); }
            }

            // Call routing
            if (fromClient instanceof String string && string.startsWith("call:")) {
                String[] callInfo = string.substring("call:".length()).split(",");
                String callType  = callInfo[0];
                String receiverId = callInfo[1];
                if (MainServer.inCall.contains(receiverId)) {
                    sendToClient("receiverIP:busy");
                } else if (MainServer.currentClients.get(receiverId) != null) {
                    MainServer.inCall.add(id);
                    MainServer.inCall.add(receiverId);
                    MainServer.currentClients.get(receiverId).sendToClient(getClientInfo(id));
                    MainServer.currentClients.get(receiverId).sendToClient("call:" + callType + "," + clientIPAddress);
                    sendToClient("receiverIP:" + MainServer.currentClients.get(receiverId).getClientIPAddress());
                } else {
                    sendToClient("receiverIP:n/a");
                }
            }

            if (fromClient instanceof String string && string.startsWith("call_accepted:")) {
                String callerId = string.substring("call_accepted:".length());
                if (MainServer.currentClients.get(callerId) != null)
                    MainServer.currentClients.get(callerId).sendToClient("call_response:accepted");
            }

            if (fromClient instanceof String string && string.startsWith("call_declined:")) {
                String callerId = string.substring("call_declined:".length());
                MainServer.inCall.remove(id);
                MainServer.inCall.remove(callerId);
                if (MainServer.currentClients.get(callerId) != null)
                    MainServer.currentClients.get(callerId).sendToClient("call_response:declined");
            }

            if (fromClient instanceof String string && string.startsWith("call_ended:")) {
                String receiverID = string.substring("call_ended:".length());
                MainServer.inCall.remove(id);
                MainServer.inCall.remove(receiverID);
                System.out.println("call ended " + receiverID);
                if (MainServer.currentClients.get(receiverID) != null)
                    MainServer.currentClients.get(receiverID).sendToClient("call_ended");
            }

            // Delete account
            if (fromClient instanceof String string && string.startsWith("delete:")) {
                String deleteId = string.substring("delete:".length());
                MainServer.currentClients.remove(deleteId);
                MainServer.inCall.remove(deleteId);
                MainServer.clients.remove(deleteId);
                try (PreparedStatement deleteUser = databaseConnection.prepareStatement(
                        "DELETE FROM Users WHERE UserId = ?")) {
                    deleteUser.setString(1, deleteId);
                    deleteUser.executeUpdate();
                } catch (SQLException e) { e.printStackTrace(); }
            }

            // ----------------------------------------------------------------
            // NewsFeed: open  — just tell the client to connect to port 4351.
            // No port search, no CountDownLatch, no feedServers map.
            // ----------------------------------------------------------------
            if (fromClient instanceof String string && string.startsWith("NewsFeed: open")) {
                sendToClient("NewsFeed connection:4351");
            }

            // NewsFeed: close — nothing to tear down on the server side;
            // the NewsFeedThread cleans itself up when the socket closes.
            else if (fromClient instanceof String string && string.startsWith("NewsFeed: close")) {
                // no-op: the client will close its feed socket; NewsFeedThread catches the IOException
            }

            // Reaction sent via the main socket (from InboxController / other non-feed contexts)
            else if (fromClient instanceof String string && string.startsWith("REACTION|")) {
                String[] parts = string.split("\\|");
                if (parts.length == 4) {
                    int    postId   = Integer.parseInt(parts[1]);
                    String reactor  = parts[2];
                    String reactType = parts[3];

                    try {
                        String oldType = "none";
                        try (PreparedStatement oldReactStmt = databaseConnection.prepareStatement(
                                "SELECT ReactType FROM Reacts WHERE PostId = ? AND Reactor = ?")) {
                            oldReactStmt.setInt(1, postId);
                            oldReactStmt.setString(2, reactor);
                            try (ResultSet rs = oldReactStmt.executeQuery()) {
                                if (rs.next()) oldType = rs.getString("ReactType");
                            }
                        }
                        try (PreparedStatement deleteStmt = databaseConnection.prepareStatement(
                                "DELETE FROM Reacts WHERE PostId = ? AND Reactor = ?")) {
                            deleteStmt.setInt(1, postId);
                            deleteStmt.setString(2, reactor);
                            deleteStmt.executeUpdate();
                        }
                        if (!reactType.equals("none")) {
                            try (PreparedStatement insertStmt = databaseConnection.prepareStatement(
                                    "INSERT INTO Reacts (PostId, Reactor, ReactType, Timestamp) VALUES (?, ?, ?, datetime('now'))")) {
                                insertStmt.setInt(1, postId);
                                insertStmt.setString(2, reactor);
                                insertStmt.setString(3, reactType);
                                insertStmt.executeUpdate();
                            }
                        }
                        NewsFeedServer.broadcastToAll(postId, reactor, oldType, reactType);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Friend system
            else if (fromClient instanceof String string && string.startsWith("friend_request:")) {
                String receiverId = string.substring("friend_request:".length());
                boolean sent = FriendManager.sendFriendRequest(databaseConnection, id, receiverId);
                sendToClient("friend_request_result:" + (sent ? "sent" : "failed"));
            }

            else if (fromClient instanceof String string && string.startsWith("friend_accept:")) {
                String senderId = string.substring("friend_accept:".length());
                boolean accepted = FriendManager.acceptFriendRequest(databaseConnection, senderId, id);
                sendToClient("friend_accept_result:" + (accepted ? "accepted" : "failed"));
            }

            else if (fromClient instanceof String string && string.startsWith("friend_reject:")) {
                String senderId = string.substring("friend_reject:".length());
                boolean rejected = FriendManager.rejectFriendRequest(databaseConnection, senderId, id);
                sendToClient("friend_reject_result:" + (rejected ? "rejected" : "failed"));
            }

            else if (fromClient instanceof String string && string.equals("get_friends")) {
                List<String> myFriends = FriendManager.getFriendList(databaseConnection, id);
                List<ClientInfo> myFriendInfos = new ArrayList<>();
                for (String fid : myFriends) {
                    ClientInfo info = getClientInfo(fid);
                    if (info != null) myFriendInfos.add(info);
                }
                sendToClient(myFriendInfos);
            }

            else if (fromClient instanceof String string && string.equals("get_requests")) {
                List<String> pendingIds = FriendManager.getPendingRequests(databaseConnection, id);
                List<ClientInfo> pendingRequestInfos = new ArrayList<>();
                for (String senderId : pendingIds) {
                    ClientInfo info = getClientInfo(senderId);
                    if (info != null) pendingRequestInfos.add(info);
                }
                sendToClient(pendingRequestInfos);
            }

            else if (fromClient instanceof String string && string.startsWith("unfriend:")) {
                String friendId = string.substring("unfriend:".length());
                boolean removed = FriendManager.unfriend(databaseConnection, id, friendId);
                sendToClient("unfriend_result:" + (removed ? "done" : "failed"));
            }

            else if (fromClient instanceof String string && string.equals("get_friend_status_map")) {
                Map<String, String> statusMap = new HashMap<>();
                for (String otherId : MainServer.clients) {
                    if (otherId.equals(id)) continue;
                    String query = """
                    SELECT CASE
                        WHEN EXISTS (
                            SELECT 1 FROM Friends
                            WHERE (user1 = ? AND user2 = ?) OR (user1 = ? AND user2 = ?)
                        ) THEN 'friends'
                        WHEN EXISTS (
                            SELECT 1 FROM FriendRequests
                            WHERE sender = ? AND receiver = ? AND status = 'pending'
                        ) THEN 'sent'
                        WHEN EXISTS (
                            SELECT 1 FROM FriendRequests
                            WHERE sender = ? AND receiver = ? AND status = 'pending'
                        ) THEN 'pending'
                        ELSE 'send'
                    END AS status
                """;
                    try (PreparedStatement stmt = databaseConnection.prepareStatement(query)) {
                        stmt.setString(1, id);
                        stmt.setString(2, otherId);
                        stmt.setString(3, otherId);
                        stmt.setString(4, id);
                        stmt.setString(5, id);
                        stmt.setString(6, id + ":" + otherId);
                        stmt.setString(7, otherId);
                        stmt.setString(8, otherId + ":" + id);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) statusMap.put(otherId, rs.getString("status"));
                        }
                    } catch (SQLException e) { e.printStackTrace(); }
                }
                sendToClient(statusMap);
            }

            else if (fromClient instanceof String string && string.startsWith("friend_status:")) {
                String[] parts = string.split(":");
                if (parts.length == 3) {
                    String me    = parts[1];
                    String other = parts[2];
                    String query = """
                SELECT
                    CASE
                        WHEN EXISTS (SELECT 1 FROM Friends WHERE (user1 = ? AND user2 = ?) OR (user1 = ? AND user2 = ?)) THEN 'friends'
                        WHEN EXISTS (SELECT 1 FROM FriendRequests WHERE sender = ? AND receiver = ? AND status = 'pending') THEN 'pending'
                        WHEN EXISTS (SELECT 1 FROM FriendRequests WHERE sender = ? AND receiver = ? AND status = 'pending') THEN 'pending'
                        ELSE 'send'
                    END as status
                """;
                    try (PreparedStatement stmt = databaseConnection.prepareStatement(query)) {
                        stmt.setString(1, me);    stmt.setString(2, other);
                        stmt.setString(3, other); stmt.setString(4, me);
                        stmt.setString(5, me);    stmt.setString(6, other);
                        stmt.setString(7, other); stmt.setString(8, me);
                        try (ResultSet rs = stmt.executeQuery()) {
                            sendToClient(rs.next() ? rs.getString("status") : "send");
                        }
                    } catch (SQLException e) { e.printStackTrace(); sendToClient("send"); }
                } else {
                    sendToClient("send");
                }
            }

            else if (fromClient instanceof String str && str.startsWith("send_comment|")) {
                String[] parts = str.split("\\|", 4);
                if (parts.length == 4) {
                    int    postId      = Integer.parseInt(parts[1]);
                    String commenter   = parts[2];
                    String commentText = parts[3].replace("[PIPE]", "|");
                    try (PreparedStatement stmt = databaseConnection.prepareStatement(
                            "INSERT INTO Comments (postId, commenter, comment, timestamp) VALUES (?, ?, ?, CURRENT_TIMESTAMP)")) {
                        stmt.setInt(1, postId);
                        stmt.setString(2, commenter);
                        stmt.setString(3, commentText);
                        stmt.executeUpdate();
                        CommentPacket packet = new CommentPacket(postId, commenter, commentText, LocalDateTime.now());
                        NewsFeedServer.broadcast(packet);
                    } catch (SQLException e) { e.printStackTrace(); }
                }
            }

            else if (fromClient instanceof String str && str.startsWith("get_comments|")) {
                int postId = Integer.parseInt(str.split("\\|")[1]);
                List<String> comments = new ArrayList<>();
                try (PreparedStatement commentStmt = databaseConnection.prepareStatement(
                        "SELECT commenter, comment FROM Comments WHERE postId = ? ORDER BY timestamp ASC")) {
                    commentStmt.setInt(1, postId);
                    try (ResultSet commentRs = commentStmt.executeQuery()) {
                        while (commentRs.next()) {
                            comments.add(commentRs.getString("commenter") + ": " + commentRs.getString("comment"));
                        }
                    }
                } catch (SQLException e) { e.printStackTrace(); }
                sendToClient(comments);
            }
        }
    }

    public ClientInfo getClientInfo(String infoId) {
        String firstName = null, lastName = null, password = null;
        String recoveryQuestion = null, recoveryAnswer = null;
        byte[] profilePictureBytes = null;

        try (PreparedStatement getUserInfo = databaseConnection.prepareStatement(
                "SELECT * FROM Users WHERE UserId = ?")) {
            getUserInfo.setString(1, infoId);
            try (ResultSet queryResult = getUserInfo.executeQuery()) {
                if (queryResult.next()) {
                    firstName        = queryResult.getString("First_Name");
                    lastName         = queryResult.getString("Last_Name");
                    password         = EncryptionProcessor.decrypt(queryResult.getString("Password"));
                    recoveryQuestion = EncryptionProcessor.decrypt(queryResult.getString("Question"));
                    recoveryAnswer   = EncryptionProcessor.decrypt(queryResult.getString("Answer"));
                    String imageFile = queryResult.getString("Profile_Picture");
                    File file = new File("src/Media Database", imageFile);
                    profilePictureBytes = Files.readAllBytes(file.toPath());
                }
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }

        return new ClientInfo(firstName, lastName, infoId, password, recoveryQuestion, recoveryAnswer, profilePictureBytes);
    }

    public String getClientIPAddress() { return clientIPAddress; }
}