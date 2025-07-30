package codes;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class ServerThread implements Runnable{
    private Thread serverThread;
    private final ObjectOutputStream output;
    private final ObjectInputStream input;
    private final String clientIPAddress;
    private ChatServer chatServer;
    private NewsFeedServer feedServer;
    private final Connection databaseConnection;
    private String id;
    private CountDownLatch latch;

    // Creating server thread from the client

    public ServerThread(Socket clientSocket) {
        serverThread = new Thread(this);

        // Initiating the output and input stream to communicate with the client

        try {
            this.output = new ObjectOutputStream(clientSocket.getOutputStream());
            this.input = new ObjectInputStream(clientSocket.getInputStream());
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

    public ChatServer getChatServer() { return chatServer; }

    @Override
    public void run() {
        // Receiving instructions from the client and sending feedbacks

        while (true) {
            Object fromClient = null;

            try {
                fromClient = input.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Client connection lost.");
                if (id != null) {
                    Server.currentClients.remove(id);
                }
                if (chatServer != null) {
                    chatServer.shutdown();
                }
                if (feedServer != null) {
                    feedServer.shutdown();
                }
                break;
            }

//            if (fromClient instanceof String string && string.startsWith("m:"))
//            {
//                System.out.println((String) fromClient);
//            }

            if (fromClient instanceof String string && string.equals("done:")) {
                if (latch != null) {
                    latch.countDown();
                    latch = null;
                }
            }

            // Checking if a client is present

            if (fromClient instanceof String string && string.startsWith("check:")) {
                String id = string.substring("check:".length());
                this.id = id;
                System.out.println(id);
                System.out.println(Server.clients.contains(id));
                sendToClient(Server.clients.contains(id));
            }

            // Signing up a client

            if (fromClient instanceof ClientInfo clientInfo) {
                this.id = clientInfo.getId();
                Server.clients.add(id);

                try (PreparedStatement addUser = databaseConnection.prepareStatement("INSERT INTO Users (UserId, First_Name, Last_Name, Password, Question, Answer, Profile_Picture) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
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

                if (!mediaDirectory.exists()) {
                    mediaDirectory.mkdir();
                }

                File mediaFile = new File(mediaDirectory, "DefaultProfilePicture.png");
                try {
                    FileOutputStream fos = new FileOutputStream(mediaFile);
                    fos.write(clientInfo.getProfilePicture());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                sendToClient("signup_successful");

                System.out.println(this.id + " signed up.");
                for (String s : Server.clients) {
                    System.out.println(s);
                }
            }

            if (fromClient instanceof byte[] imageBytes) {
                InputStream is = new ByteArrayInputStream(imageBytes);
                ImageInputStream iis = null;
                try {
                    iis = ImageIO.createImageInputStream(is);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
                String format = "png";
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    try {
                        format = reader.getFormatName().toLowerCase();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                BufferedImage bufferedImage = null;
                try {
                    bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                String filename = id + "_ProfilePicture." + format;
                File mediaDirectory = new File("src/Media Database");

                if (!mediaDirectory.exists()) {
                    mediaDirectory.mkdir();
                }

                File file = new File(mediaDirectory, filename);
                try {
                    ImageIO.write(bufferedImage, format, file);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                try (PreparedStatement addProfilePicture = databaseConnection.prepareStatement("UPDATE Users SET Profile_Picture = ? WHERE UserId = ?")) {
                    addProfilePicture.setString(1, filename);
                    addProfilePicture.setString(2, id);

                    addProfilePicture.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                sendToClient("profile_picture_set");
            }

            // Getting client's information

            if (fromClient instanceof String string && string.startsWith("get_info:")) {
                String id = string.substring("get_info:".length());

                sendToClient(getClientInfo(id));
            }

            // Logging in a client

            if (fromClient instanceof String string && string.startsWith("login:")) {
                this.id = string.substring("login:".length());
                Server.currentClients.put(id, this);

                sendToClient("login_successful");

                // Starting notification thread

                new Thread (() -> {
                    int lastNotifId = 0;

                    while (true) {
                        try (PreparedStatement getNotification = databaseConnection.prepareStatement("SELECT * FROM Notification WHERE Receiver = ? AND id > ? ORDER BY id ASC")) {
                            getNotification.setString(1, id);
                            getNotification.setInt(2, lastNotifId);

                            try (ResultSet notifications = getNotification.executeQuery()) {
                                while (notifications.next()) {
                                    sendToClient("notif:" + notifications.getString("Sender") + "," + notifications.getString("Type") + "," + notifications.getString("Status"));
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
                for (String s : Server.clients) {
                    System.out.println(s);
                }
            }

            // Logging out a client

            if (fromClient instanceof String string && string.startsWith("logout:")) {
                this.id = string.substring("logout:".length());
                Server.currentClients.remove(id);
                Server.inCall.remove(id);
            }

            // Updating client's information

            if (fromClient instanceof String string && string.startsWith("update:")) {
                String[] updateInfo = string.substring("update:".length()).split(",");

                if (updateInfo[0].equals("name")) {
                    try (PreparedStatement updateName = databaseConnection.prepareStatement("UPDATE Users SET First_Name = ?, Last_Name = ? WHERE UserId = ?")) {
                        updateName.setString(1, updateInfo[1]);
                        updateName.setString(2, updateInfo[2]);
                        updateName.setString(3, id);

                        updateName.executeUpdate();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
                else if (updateInfo[0].equals("password")) {
                    try (PreparedStatement updatePassword = databaseConnection.prepareStatement("UPDATE Users SET Password = ? WHERE UserId = ?")) {
                        updatePassword.setString(1, EncryptionProcessor.encrypt(updateInfo[1]));
                        updatePassword.setString(2, id);

                        updatePassword.executeUpdate();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }

                sendToClient("updated");
            }

            // Sending all clients' information to the user

            if (fromClient instanceof String string && string.equals("load_clients")) {
                Vector<ClientInfo> clientInfo = new Vector<>();

                for (String id : Server.clients) {
                    clientInfo.add(getClientInfo(id));  //adding clients to the vector
                }

                System.out.println("Loading clients");

                for (ClientInfo c : clientInfo) {
                    System.out.println(c.getFirstName());   //printing the first name for debug ig
                }

                sendToClient(clientInfo);
            }

            // Enabling a client to chat with another client

            if (fromClient instanceof String string && string.startsWith("chat_with:")) {
                String receiverId = string.substring("chat_with:".length());

                // Creating new chat server for the client and sending the connection information to the client

                int port = 0;

                for (int i = 0; i < 45000; i++) {
                    if (Server.port.get(i) == 0) {
                        port = i + 1025;
                        try (ServerSocket serverSocket = new ServerSocket(port)) {
                            serverSocket.setReuseAddress(true);
                            Server.port.set(i, 1);
                            break;
                        } catch (IOException ignored) {}
                    }
                }

                chatServer = new ChatServer(port, id, receiverId);

                sendToClient("connect_to:" + port + "," + receiverId);
            }

            if (fromClient instanceof String string && string.equals("close_chat")) {
                if (chatServer != null) {
                    chatServer.shutdown();
                }
                sendToClient("chat_closed");
                System.out.println("Shut down chat server for client " + id);
            }

            if (fromClient instanceof String string && string.startsWith("seen:")) {
                String[] notificationInfo = string.substring("seen:".length()).split(",");
                String sender = notificationInfo[0];
                String receiver = notificationInfo[1];

                try (PreparedStatement updateNotificationStatus = databaseConnection.prepareStatement("UPDATE Notification SET Status = 'seen' WHERE Sender = ? AND Receiver = ?")) {
                    updateNotificationStatus.setString(1, sender);
                    updateNotificationStatus.setString(2, receiver);

                    updateNotificationStatus.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            if (fromClient instanceof String string && string.startsWith("call:")) {
                String[] callInfo = string.substring("call:".length()).split(",");

                String callType = callInfo[0];
                String receiverId = callInfo[1];

                if (Server.inCall.contains(receiverId)) {
                    sendToClient("receiverIP:busy");
                }
                else if (Server.currentClients.get(receiverId) != null) {
                    Server.inCall.add(id);
                    Server.inCall.add(receiverId);

                    Server.currentClients.get(receiverId).sendToClient(getClientInfo(id));
                    Server.currentClients.get(receiverId).sendToClient("call:" + callType + "," + clientIPAddress);
                    sendToClient("receiverIP:" + Server.currentClients.get(receiverId).getClientIPAddress());
                }
                else {
                    sendToClient("receiverIP:n/a");
                }
            }

            if (fromClient instanceof String string && string.startsWith("call_accepted:")) {
                String callerId = string.substring("call_accepted:".length());

                if (Server.currentClients.get(callerId) != null) {
                    Server.currentClients.get(callerId).sendToClient("call_response:accepted");
                }
            }

            if (fromClient instanceof String string && string.startsWith("call_declined:")) {
                String callerId = string.substring("call_declined:".length());

                Server.inCall.remove(id);
                Server.inCall.remove(callerId);

                if (Server.currentClients.get(callerId) != null) {
                    Server.currentClients.get(callerId).sendToClient("call_response:declined");
                }
            }

            if (fromClient instanceof String string && string.startsWith("call_ended:")) {
                String receiverID = string.substring("call_ended:".length());

                Server.inCall.remove(id);
                Server.inCall.remove(receiverID);

                System.out.println("call ended " + receiverID);

                if (Server.currentClients.get(receiverID) != null) {
                    Server.currentClients.get(receiverID).sendToClient("call_ended");
                }
            }

            if (fromClient instanceof String string && string.startsWith("NewsFeed: open")) {

                int port = 0;

                for (int i = 0; i < 45000; i++) {
                    if (Server.port.get(i) == 0) {
                        port = i + 1025;
                        try (ServerSocket serverSocket = new ServerSocket(port)) {
                            serverSocket.setReuseAddress(true);
                            Server.port.set(i, 1);
                            break;
                        } catch (IOException ignored) {}
                    }
                }

                CountDownLatch fLatch = new CountDownLatch(1);
                feedServer = new NewsFeedServer(port, fLatch);
                Server.feedServers.put(id, feedServer);
                sendToClient("NewsFeed connection:" + port);
            }

            if (fromClient instanceof String string && string.startsWith("NewsFeed: close")) {
                Server.feedServers.remove(id);
                if (feedServer != null) {
                    feedServer.shutdown();
                }
            }

            else if (fromClient instanceof String string && string.startsWith("REACTION|")) {
                //System.out.println("Hello, I am a reaction. Add me to db");

                String[] parts = string.split("\\|");
                if (parts.length == 4) {
                    int postId = Integer.parseInt(parts[1]);
                    String reactor = parts[2];
                    String reactType = parts[3];

                    try {
                        // old reaction
                        String oldType = "none";
                        try (PreparedStatement oldReactStmt = databaseConnection.prepareStatement(
                                "SELECT ReactType FROM Reacts WHERE PostId = ? AND Reactor = ?")) {
                            oldReactStmt.setInt(1, postId);
                            oldReactStmt.setString(2, reactor);
                            try (ResultSet rs = oldReactStmt.executeQuery()) {
                                if (rs.next()) oldType = rs.getString("ReactType");
                            }
                        }

                        // Delete reaction
                        try (PreparedStatement deleteStmt = databaseConnection.prepareStatement(
                                "DELETE FROM Reacts WHERE PostId = ? AND Reactor = ?")) {
                            deleteStmt.setInt(1, postId);
                            deleteStmt.setString(2, reactor);
                            deleteStmt.executeUpdate();
                        }

                        // insert
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

            else if (fromClient instanceof PostPacket packet) {
                if (feedServer != null) {
                    feedServer.handleIncomingPostPacket(packet);
                } else {
                    System.out.println("Received PostPacket but feed server is null for user: " + id);
                }
            }



            //Because my lonely user needs some fake friends


            else if (fromClient instanceof String string && string.startsWith("friend_request:")) {
                String receiverId = string.substring("friend_request:".length());
                boolean sent = friendManager.sendFriendRequest(databaseConnection, id, receiverId);
                sendToClient("friend_request_result:" + (sent ? "sent" : "failed"));
            }


            else if (fromClient instanceof String string && string.startsWith("friend_accept:")) {
                String senderId = string.substring("friend_accept:".length());
                boolean accepted = friendManager.acceptFriendRequest(databaseConnection, senderId, id);
                sendToClient("friend_accept_result:" + (accepted ? "accepted" : "failed"));
            }


            else if (fromClient instanceof String string && string.startsWith("friend_reject:")) {
                String senderId = string.substring("friend_reject:".length());
                boolean rejected = friendManager.rejectFriendRequest(databaseConnection, senderId, id);
                sendToClient("friend_reject_result:" + (rejected ? "rejected" : "failed"));
            }


            else if (fromClient instanceof String string && string.equals("get_friends")) {
                List<String> myFriends  = friendManager.getFriendList(databaseConnection, id);
                //System.out.println("Size of friend list: " + myFriends.size());
                List <ClientInfo> myFriendInfos = new ArrayList<>();

                for (String senderId : myFriends) {
                    ClientInfo info = getClientInfo(senderId);
                    if (info != null) {
                        myFriendInfos.add(info);
                        //System.out.println("Added my friend requester: " + senderId);
                    }
                }

               // System.out.println("Size of the clientInfoList "+myFriendInfos.size());

                sendToClient(myFriendInfos);
            }


            else if (fromClient instanceof String string && string.equals("get_requests")) {
                //System.out.println("Got the request to send the requests for " + id);

                List<String> pendingIds = friendManager.getPendingRequests(databaseConnection, id);
                //System.out.println("Size of request list: " + pendingIds.size());

                List <ClientInfo> pendingRequestInfos = new ArrayList<>();
                for (String senderId : pendingIds) {
                    ClientInfo info = getClientInfo(senderId);
                    if (info != null) {
                        pendingRequestInfos.add(info);
                       // System.out.println("Added pending requester: " + senderId);
                    }
                }

                //System.out.println("Size of the clientInfoList "+pendingRequestInfos.size());

                sendToClient(pendingRequestInfos);
            }



            else if (fromClient instanceof String string && string.startsWith("unfriend:")) {
                String friendId = string.substring("unfriend:".length());
                boolean removed = friendManager.unfriend(databaseConnection, id, friendId);
                sendToClient("unfriend_result:" + (removed ? "done" : "failed"));
            }


            else if (fromClient instanceof String string && string.equals("get_friend_status_map")) {
                Map<String, String> statusMap = new HashMap<>();

                for (String otherId : Server.clients) {
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
                        stmt.setString(1, id);                         // me -> other (Friends)
                        stmt.setString(2, otherId);
                        stmt.setString(3, otherId);                    // other -> me (Friends)
                        stmt.setString(4, id);

                        stmt.setString(5, id);                         // sent: I sent to them
                        stmt.setString(6, id + ":" + otherId);

                        stmt.setString(7, otherId);                    // pending: they sent to me
                        stmt.setString(8, otherId + ":" + id);




                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                String status = rs.getString("status");

                               // System.out.println("In server Status for " + otherId + " is " + status);

                                statusMap.put(otherId, status);
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

                sendToClient(statusMap);
            }



            else if (fromClient instanceof String string && string.startsWith("friend_status:")) {
                String[] parts = string.split(":");
                if (parts.length == 3) {
                    String me = parts[1];
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
                        stmt.setString(1, me);
                        stmt.setString(2, other);
                        stmt.setString(3, other);
                        stmt.setString(4, me);
                        stmt.setString(5, me);
                        stmt.setString(6, other);
                        stmt.setString(7, other);
                        stmt.setString(8, me);

                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                sendToClient(rs.getString("status"));
                            } else {
                                sendToClient("send");
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                        sendToClient("send");
                    }
                } else {
                    sendToClient("send");
                }
            }



            else if (fromClient instanceof String str && str.startsWith("send_comment|")) {
                String[] parts = str.split("\\|", 4);
                if (parts.length == 4) {
                    int postId = Integer.parseInt(parts[1]);
                    String commenter = parts[2];
                    String commentText = parts[3].replace("[PIPE]", "|");  // restore escaped pipes

                    try (PreparedStatement stmt = databaseConnection.prepareStatement(
                            "INSERT INTO Comments (postId, commenter, comment, timestamp) VALUES (?, ?, ?, CURRENT_TIMESTAMP)")) {
                        stmt.setInt(1, postId);
                        stmt.setString(2, commenter);
                        stmt.setString(3, commentText);
                        stmt.executeUpdate();

                        System.out.println("Comment saved on post " + postId + " by " + commenter);


                        CommentPacket packet = new CommentPacket(
                                postId,
                                commenter,
                                commentText,
                                LocalDateTime.now()
                        );

                        Client.broadcast(packet);

                        System.out.println("Look I am server and I did broadcast it, you gotta talk to Client.java");
                        System.out.println("Broadcasted comment: " + commenter + " said " + commentText);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
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
                            String commenter = commentRs.getString("commenter");
                            String commentText = commentRs.getString("comment");
                            comments.add(commenter + ": " + commentText);
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                sendToClient(comments); // objectOutputStream.writeObject(comments);
            }
        }
    }

    public ClientInfo getClientInfo(String id) {
        String firstName = null;
        String lastName = null;
        String password = null;
        String recoveryQuestion = null;
        String recoveryAnswer = null;
        byte[] profilePictureBytes = null;

        try (PreparedStatement getUserInfo = databaseConnection.prepareStatement("SELECT * FROM Users WHERE UserId = ?")) {
            getUserInfo.setString(1, id);

            try (ResultSet queryResult = getUserInfo.executeQuery()) {
                if (queryResult.next()) {
                    firstName = queryResult.getString("First_Name");
                    lastName = queryResult.getString("Last_Name");
                    password = EncryptionProcessor.decrypt(queryResult.getString("Password"));
                    recoveryQuestion = EncryptionProcessor.decrypt(queryResult.getString("Question"));
                    recoveryAnswer = EncryptionProcessor.decrypt(queryResult.getString("Answer"));
                    String imageFile = queryResult.getString("Profile_Picture");
                    File file = new File("src/Media Database", imageFile);
                    profilePictureBytes = Files.readAllBytes(file.toPath());
                }
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }

        return new ClientInfo(firstName, lastName, id, password, recoveryQuestion, recoveryAnswer, profilePictureBytes);
    }

    public String getClientIPAddress() {
        return clientIPAddress;
    }
}