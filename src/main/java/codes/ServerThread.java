package codes;

import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.sql.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class ServerThread implements Runnable{
    private Thread serverThread;
    private final ObjectOutputStream output;
    private final ObjectInputStream input;
    private ChatServer chatServer;
    private NewsFeedServer feedServer;
    private final Connection databaseConnection;
    private String id;

    // Creating server thread from the client

    public ServerThread(Socket clientSocket) {
        serverThread = new Thread(this);

        // Initiating the output and input stream to communicate with the client

        try {
            this.output = new ObjectOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            this.input = new ObjectInputStream(clientSocket.getInputStream());
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

            // Checking if a client is present

            if (fromClient instanceof String string && string.startsWith("check:")) {
                String id = string.substring("check:".length());
                this.id = id;
                System.out.println(id);

                try {
                    System.out.println(Server.clients.contains(id));
                    output.writeObject(Server.clients.contains(id));
                    output.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            // Signing up a client

            if (fromClient instanceof String string && string.startsWith("signup:")) {
                String[] clientInfo = string.substring("signup:".length()).split(",");

                this.id = clientInfo[0];
                Server.clients.add(id);

                try (PreparedStatement addUser = databaseConnection.prepareStatement("INSERT INTO Users (UserId, First_Name, Last_Name, Password, Question, Answer) VALUES (?, ?, ?, ?, ?, ?)")) {
                    addUser.setString(1, clientInfo[0]);
                    addUser.setString(2, clientInfo[2]);
                    addUser.setString(3, clientInfo[3]);
                    addUser.setString(4, clientInfo[1]);
                    addUser.setString(5, clientInfo[4]);
                    addUser.setString(6, clientInfo[5]);

                    addUser.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                try {
                    output.writeObject("signup_successful");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

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

                try {
                    output.writeObject("profile_picture_set");
                    output.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            // Getting client's information

            if (fromClient instanceof String string && string.startsWith("get_info:")) {
                String id = string.substring("get_info:".length());
                this.id = id;

                try {
                    output.writeObject(getClientInfo(id));
                    output.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            // Logging in a client

            if (fromClient instanceof String string && string.startsWith("login:")) {
                this.id = string.substring("login:".length());
                Server.currentClients.put(id, this);

                try {
                    output.writeObject("login_successful");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                System.out.println(this.id + " logged in");
                for (String s : Server.clients) {
                    System.out.println(s);
                }
            }

            // Updating client's information

            if (fromClient instanceof String string && string.startsWith("update:")) {
                String[] updateInfo = string.substring("update:".length()).split(",");

                if (updateInfo[0].equals("password")) {
                    try (PreparedStatement updatePassword = databaseConnection.prepareStatement("UPDATE Users SET Password = ? WHERE UserId = ?")) {
                        updatePassword.setString(1, updateInfo[1]);
                        updatePassword.setString(2, id);

                        updatePassword.executeUpdate();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }

                try {
                    output.writeObject("updated");
                    output.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            // Sending all clients' information to the user

            if (fromClient instanceof String string && string.equals("load_clients")) {
                Vector<ClientInfo> clientInfo = new Vector<>();

                for (String id : Server.clients) {
                    clientInfo.add(getClientInfo(id));
                }

                System.out.println("Loading clients");

                for (ClientInfo c : clientInfo) {
                    System.out.println(c.getFirstName());
                }

                try {
                    output.writeObject(clientInfo);
                    output.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            // Enabling a client to chat with another client

            if (fromClient instanceof String string && string.startsWith("chat_with:")) {
                String receiverId = string.substring("chat_with:".length());

                // Creating new chat server for the client and sending the connection information to the client

                int port = 0;

                for (int i = 0; i < 45000; i++) {
                    if (Server.port.get(i) == 0) {
                        port = i + 1025;
                        Server.port.set(i, 1);
                        break;
                    }
                }

                chatServer = new ChatServer(port, id, receiverId);

                String receiverName;
                String receiverFirstName;

                try (PreparedStatement getReceiverName = databaseConnection.prepareStatement("SELECT * FROM Users WHERE UserId = ?")) {
                    getReceiverName.setString(1, receiverId);

                    try (ResultSet queryResult = getReceiverName.executeQuery()) {
                        receiverName = queryResult.getString("First_Name") + " " + queryResult.getString("Last_Name");
                        receiverFirstName = queryResult.getString("First_Name");
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                try {
                    output.writeObject("connect_to:" + port + "," + receiverId + "," + receiverName + "," + receiverFirstName);
                    output.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if (fromClient instanceof String string && string.equals("close_chat")) {
                if (chatServer != null) {
                    chatServer.shutdown();

                    try {
                        output.writeObject("chat_closed");
                        output.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    System.out.println("Shut down chat server for client " + id);
                }
            }

            if (fromClient instanceof String string && string.startsWith("NewsFeed: open")) {

                int port = 0;

                for (int i = 0; i < 45000; i++) {
                    if (Server.port.get(i) == 0) {
                        port = i + 1025;
                        Server.port.set(i, 1);
                        break;
                    }
                }

                CountDownLatch fLatch = new CountDownLatch(1);
                feedServer = new NewsFeedServer(port, fLatch);
                Server.feedServers.put(id, feedServer);

                try {
                    fLatch.await();
                    output.writeObject("NewsFeed connection:" + port);
                    output.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
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
                    password = queryResult.getString("Password");
                    recoveryQuestion = queryResult.getString("Question");
                    recoveryAnswer = queryResult.getString("Answer");
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
}