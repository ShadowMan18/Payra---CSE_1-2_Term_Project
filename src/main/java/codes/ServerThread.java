package codes;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class ServerThread implements Runnable{
    private Thread serverThread;
    private boolean running;
    private final ObjectOutputStream output;
    private final ObjectInputStream input;
    private final Connection databaseConnection;
    private String id;

    // Creating server thread from the client

    public ServerThread(Socket clientSocket) {
        serverThread = new Thread(this);
        this.running = true;

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

        while (running) {
            Object fromClient = null;

            try {
                fromClient = input.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Client connection lost.");
                if (id != null) {
                    Server.currentClients.remove(id);
                }
                this.running = false;
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
                System.out.println(Server.clients.size());
            }

            // Getting client's information

            if (fromClient instanceof String string && string.startsWith("get_info:")) {
                String id = string.substring("get_info:".length());
                this.id = id;

                try (PreparedStatement getUserInfo = databaseConnection.prepareStatement("SELECT * FROM Users WHERE UserId = ?")) {
                    getUserInfo.setString(1, id);

                    try (ResultSet queryResult = getUserInfo.executeQuery()) {
                        if (queryResult.next()) {
                            output.writeObject("info:" + queryResult.getString("UserId") + "," + queryResult.getString("First_Name") + "," + queryResult.getString("Last_Name") + "," + queryResult.getString("Password") + "," + queryResult.getString("Question") + "," + queryResult.getString("Answer"));
                            output.flush();
                        }
                    }
                } catch (SQLException | IOException e) {
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
                System.out.println(Server.currentClients.size());
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

            // Enabling a client to chat with another client

            if (fromClient instanceof String string && string.startsWith("chat_with:")) {
                String receiverId = string.substring("chat_with:".length());

                // Creating new chat server for the client and sending the connection information to the client

                int port = 0;

                for (int i = 0; i < 45000; i++) {
                    if (Server.port[i] == 0) {
                        port = i + 1025;
                        Server.port[i] = 1;
                        break;
                    }
                }

                new ChatServer(port, id, receiverId);

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

            if (fromClient instanceof String string && string.startsWith("NewsFeed: open")) {

                int port = 0;

                for (int i = 0; i < 45000; i++) {
                    if (Server.port[i] == 0) {
                        port = i + 1025;
                        Server.port[i] = 1;
                        break;
                    }
                }

                CountDownLatch fLatch = new CountDownLatch(1);
                NewsFeedServer feedServer = new NewsFeedServer(port, fLatch);
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
                NewsFeedServer feedServer = Server.feedServers.remove(id);
                if (feedServer != null) {
                    int feedPort = feedServer.getPort();
                    feedServer.shutdown();
                    System.out.println("Shut down NewsFeedServer for client " + id);
                    Server.port[feedPort - 1025] = 0;
                }
            }
        }
    }

}
