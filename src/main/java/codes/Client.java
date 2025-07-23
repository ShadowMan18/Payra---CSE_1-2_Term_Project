package codes;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.util.Pair;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

public class Client {
    // Client information

    private String firstName;
    private String lastName;
    private String email;
    private String id;
    private String password;
    private String recoveryQuestion;
    private String recoveryAnswer;
    private Image profilePicture;
    private boolean registered;
    private boolean active;
    private CountDownLatch latch;

    // Client pages

    private final IntroPage introPage;
    private final LoginPage loginPage;
    private final SignupPage signupPage;
    private final ProfilePicturePage profilePicturePage;
    private final ForgotPasswordPage forgotPasswordPage;
    private final HomePage homePage;
    private final Inbox inbox;
    private final NewsFeed newsFeed;
    private final ProfilePage profilePage;
    private final NotificationPage notificationPage;

    // Client server network

    private final String ipAddress = "192.168.68.11";
    private final Socket serverSocket;
    private final ObjectOutputStream serverOutput;
    private final ObjectInputStream serverInput;

    // Chat server network

    private Socket chatSocket;
    private ObjectOutputStream chatOutput;
    private ObjectInputStream chatInput;
    private Vector<ClientInfo> clients;
    private String receiverId;
    private boolean chatStatus;
    private boolean newNotification;
    public Map<String, Pair<String, String>> notification = new LinkedHashMap<>();

    private String receiverIPAddress;

    // NewsFeed server network

    private Socket feedSocket;
    private ObjectInputStream feedInput;
    private ObjectOutputStream feedOutput;
    private NewsFeedController newsFeedController;
    private boolean connectedToNewsFeed;


    public void setNewsFeedController(NewsFeedController controller) {
        this.newsFeedController = controller;
    }

    public static final CopyOnWriteArrayList<ObjectOutputStream> allClientOutputs = new CopyOnWriteArrayList<>();

    // Optionally add helper methods:
    public static void addClient(ObjectOutputStream out) {
        allClientOutputs.add(out);
    }

    public static void removeClient(ObjectOutputStream out) {
        allClientOutputs.remove(out);
    }
    public static int clientListSize(){
        return allClientOutputs.size();
    }

    public static void broadcast(String message) {
        for (ObjectOutputStream out : allClientOutputs) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (Exception e) {
                removeClient(out);
            }
        }
    }
    public static void broadcast(PostPacket packet) {
        for (ObjectOutputStream out : allClientOutputs) {
            try {
                out.writeObject(packet);
                out.flush();
            } catch (Exception e) {
                removeClient(out);
            }
        }
    }

    // Constructor

    public Client() {
        this.registered = false;
        this.active = false;
        this.receiverId = null;
        this.newNotification = false;
        this.connectedToNewsFeed=false;
        this.introPage = new IntroPage();
        this.loginPage = new LoginPage();
        this.signupPage = new SignupPage();
        this.profilePicturePage = new ProfilePicturePage();
        this.forgotPasswordPage = new ForgotPasswordPage();
        this.homePage = new HomePage();
        this.inbox = new Inbox();
        this.newsFeed = new NewsFeed();
        this.profilePage = new ProfilePage();
        this.notificationPage = new NotificationPage();

        try {
            this.serverSocket = new Socket(ipAddress, 1024);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            this.serverOutput = new ObjectOutputStream(serverSocket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            this.serverInput = new ObjectInputStream(serverSocket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        clients = new Vector<>();

//        Thread Writer = new Thread(() -> {
//            Scanner scanner = new Scanner(System.in);
//            while (true) {
//                String message = scanner.nextLine();
//
//                if (inChat) {
//                    try {
//                        String[] splittedMessage = message.split(",");
//                        int clientIndex = Integer.parseInt(splittedMessage[0]);
//                        chatOutput.writeObject(message);
//                        chatOutput.flush();
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//                else {
//                    try {
//                        serverOutput.writeObject(message);
//                        serverOutput.flush();
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//            }
//        });

        new Thread(() -> {
            while (true) {
                Object fromServer;

                try {
                    fromServer = serverInput.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("No connection with server");
                    break;
                }

                if (fromServer instanceof Boolean b) {
                    this.registered = b;

                    if (latch != null) {
                        latch.countDown();
                        latch = null;
                    }
                }
                else if (fromServer instanceof String string && string.equals("signup_successful")) {
                    this.registered = true;

                    if (latch != null) {
                        latch.countDown();
                        latch = null;
                    }
                }
                else if (fromServer instanceof String string && string.equals("profile_picture_set")) {
                    if (latch != null) {
                        latch.countDown();
                        latch = null;
                    }
                }
                else if (fromServer instanceof ClientInfo info) {
                    this.firstName = info.getFirstName();
                    this.lastName = info.getLastName();
                    this.id = info.getId();
                    this.email  = id + "@gmail.com";
                    this.password = info.getPassword();
                    this.recoveryQuestion = info.getRecoveryQuestion();
                    this.recoveryAnswer = info.getRecoveryAnswer();
                    byte[] profilePictureByte = info.getProfilePicture();

                    Platform.runLater(() -> {
                        profilePicture = new Image(new ByteArrayInputStream(profilePictureByte));
                    });

                    if (latch != null) {
                        latch.countDown();
                        latch = null;
                    }
                }
                else if (fromServer instanceof Vector<?> v) {
                    if (!v.isEmpty() && v.get(0) instanceof ClientInfo) {
                        clients = (Vector<ClientInfo>) v;
                        clients.sort(Comparator.comparing(ClientInfo::getFirstName));
                    }

                    if (latch != null) {
                        latch.countDown();
                        latch = null;
                    }
                }
                else if (fromServer instanceof String string && string.equals("login_successful")) {
                    this.active = true;

                    if (latch != null) {
                        latch.countDown();
                        latch = null;
                    }
                }
                else if (fromServer instanceof String string && string.startsWith("updated")) {
                    if (latch != null) {
                        latch.countDown();
                        latch = null;
                    }
                }
                else if (fromServer instanceof String string && string.startsWith("connect_to:")) {
                    String[] connectionInfo = string.substring("connect_to:".length()).split(",");
                    int port = Integer.parseInt(connectionInfo[0]);
                    this.receiverId = connectionInfo[1];

                    connectToChatServer(port);

                    if (latch != null) {
                        latch.countDown();
                        latch = null;
                    }
                }
                else if (fromServer instanceof String string && string.equals("chat_closed")) {
                    chatStatus = false;

                    if (latch != null) {
                        latch.countDown();
                        latch = null;
                    }
                }
                else if (fromServer instanceof String string && string.startsWith("notif:")) {
                    String[] notificationInfo = string.substring("notif:".length()).split(",");
                    String sender = notificationInfo[0];
                    String type = notificationInfo[1];
                    String status = notificationInfo[2];

                    if (notification.get(sender) != null) {
                        notification.remove(sender);
                    }

                    notification.put(sender, new Pair(type, status));

                    if (status.equals("unseen")) {
                        newNotification = true;
                    }

                    if (type.equals("message")) {
                        System.out.println(sender + " sent a " + notification.get(sender));
                    }
                }
                else if (fromServer instanceof String string && string.startsWith("receiverIP:")) {
                    this.receiverIPAddress = string.substring("receiverIP:".length());

                    if (latch != null) {
                        latch.countDown();
                        latch = null;
                    }
                }
                else if (fromServer instanceof String string && string.startsWith("NewsFeed connection:")) {
                    int port = Integer.parseInt(string.substring("NewsFeed connection:".length()));

                    connectToFeedServer(port);

                    if (latch != null) {
                        latch.countDown();
                        latch = null;
                    }
                }
                else {
                    assert fromServer instanceof String;
                    System.out.println("Received: " + (String) fromServer);
                }
            }
        }).start();

//        Writer.start();
//        serverReader.start();
    }

    public void clientIsConnectedToNewsFeed(){
        connectedToNewsFeed=true;
    }

    public boolean isConnectedToNewsFeed(){
        return connectedToNewsFeed;
    }

    public void disconnectFromFeedServer() {
        System.out.println("disconnectFromFeedServer called");
        System.out.println("connectedToNewsFeed = " + connectedToNewsFeed);
        System.out.println("feedOutput = " + feedOutput);
        if(!connectedToNewsFeed) return;
        try {
            serverOutput.writeObject("NewsFeed: close");
            serverOutput.flush();
            if (feedOutput != null){
                Client.removeClient(feedOutput);
                System.out.println(this.id+ ":left client list");
                System.out.println("Size of client list "+Client.clientListSize());
                feedOutput.close();
            }
            else{
                System.out.println("feedOutput is NULL, skipping removal");
            }
            if (feedInput != null) feedInput.close();
            if (feedSocket != null && !feedSocket.isClosed()) feedSocket.close();
            System.out.println(this.id+": Disconnected from NewsFeed Server and gave up port");
            connectedToNewsFeed=false;



        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnectFromChatServer() {
        System.out.println("Chat status = " + connectedToNewsFeed);
        if(!chatStatus) return;

        try {
            serverOutput.writeObject("close_chat");
            serverOutput.flush();

            if (feedSocket != null && !feedSocket.isClosed()) {
                feedSocket.close();
            }

            if (feedOutput != null) {
                feedOutput.close();
            }

            if (feedInput != null) {
                feedInput.close();
            }

            chatStatus = false;

            System.out.println(this.id+": Disconnected from Chat Server and gave up port");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Getters

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    public String getRecoveryQuestion() {
        return recoveryQuestion;
    }

    public String getRecoveryAnswer() {
        return recoveryAnswer;
    }

    public Image getProfilePicture() {
        return profilePicture;
    }

    public boolean isRegistered() {
        return registered;
    }

    public boolean isActive() {
        return active;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public boolean getChatStatus() {
        return chatStatus;
    }

    public String getReceiverIPAddress() {
        return receiverIPAddress;
    }

    public IntroPage getIntroPage() {
        return introPage;
    }

    public LoginPage getLoginPage() {
        return loginPage;
    }

    public SignupPage getSignupPage() {
        return signupPage;
    }

    public ProfilePicturePage getProfilePicturePage() {
        return profilePicturePage;
    }

    public ForgotPasswordPage getForgotPasswordPage() {
        return forgotPasswordPage;
    }

    public HomePage getHomePage() {
        return homePage;
    }

    public Inbox getInbox() {
        return inbox;
    }

    public NewsFeed getNewsFeed() {
        return newsFeed;
    }

    public ProfilePage getProfilePage() {
        return profilePage;
    }

    public NotificationPage getNotificationPage() {
        return notificationPage;
    }

    public ObjectOutputStream getServerOutput() {
        return serverOutput;
    }

    public ObjectInputStream getServerInput() {
        return serverInput;
    }

    public ObjectOutputStream getChatOutput() {
        return chatOutput;
    }

    public ObjectInputStream getChatInput() {
        return chatInput;
    }

    public ObjectOutputStream getFeedOutput() {
        return feedOutput;
    }

    public ObjectInputStream getFeedInput() {
        return feedInput;
    }

    public Vector<ClientInfo> getClients() {
        return clients;
    }

    public boolean hasNewNotification() { return newNotification; }
    // Setters

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRecoveryQuestion(String recoveryQuestion) {
        this.recoveryQuestion = recoveryQuestion;
    }

    public void setRecoveryAnswer(String recoveryAnswer) {
        this.recoveryAnswer = recoveryAnswer;
    }

    public void setProfilePicture(String url) { this.profilePicture = new Image(String.valueOf(getClass().getResource(url))); }

    public void setChatStatus(boolean status) { chatStatus = status; }

    public void resetNotificationStatus() { newNotification = false; }

    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    @Override
    public String toString() {
        return id + "," + password + "," + firstName + "," + lastName;
    }

    // Public methods

    public synchronized void sendToServer(Object obj) {
        try {
            latch = new CountDownLatch(1);

            serverOutput.writeObject(obj);
            serverOutput.flush();

            latch.await();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void connectToChatServer(int port) {
        try {
            this.chatSocket = new Socket(ipAddress, port);
            this.chatOutput = new ObjectOutputStream(chatSocket.getOutputStream());
            this.chatInput = new ObjectInputStream(chatSocket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Connected to: " + port);

//        Thread chatWriter = new Thread(() -> {
//            while (true) {
//                String message = scanner.nextLine();
//
//                try {
//                    chatOutput.writeObject(message);
//                    chatOutput.flush();
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        });

//        Thread chatReader = new Thread(() -> {
//            while (true) {
//                String message;
//
//                try {
//                    message = (String) (input.readObject());
//                } catch (IOException | ClassNotFoundException e) {
//                    throw new RuntimeException(e);
//                }
//
//                System.out.println("Received from client: " + message);
//            }
//        });
//
////        chatWriter.start();
//        chatReader.start();
    }

    public void connectToFeedServer(int port) {
        try {
            this.feedSocket = new Socket(ipAddress, port);
            this.feedOutput = new ObjectOutputStream(feedSocket.getOutputStream());
            this.feedInput = new ObjectInputStream(feedSocket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Connected to Feed Server on port: " + port);

        try {
            feedOutput.writeObject(this.id);
            feedOutput.flush();
            System.out.println("I have been connected to feed server.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        new Thread(() -> {
            while (true) {
                try {
                    Object feedUpdate = feedInput.readObject();
                    //System.out.println("Feed: " + (String)feedUpdate);

                    Platform.runLater(() -> {
                        if (newsFeedController != null) {
                            if (feedUpdate instanceof String stringUpdate) {
                                if (stringUpdate.startsWith("REACTION|")) {
                                    String[] parts = stringUpdate.split("\\|");
                                    int postId = Integer.parseInt(parts[1]);
                                    String reactor = parts[2];
                                    String oldType = parts[3];
                                    String newType = parts[4];
                                    newsFeedController.updateReactionOnPost(postId, reactor, oldType, newType);
                                } else {
                                    newsFeedController.addPostToFeed(stringUpdate); // This is now unambiguous
                                }
                            } else if (feedUpdate instanceof PostPacket packet) {
                                newsFeedController.addPostToFeed(packet); // Also unambiguous
                            }

                        }
                    });

                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("Feed reading thread exiting for client " + this.id );
                    //e.printStackTrace();
                    break;
                }
            }
        }).start();

    }
    public void sendPostToFeed(String content, String filename, byte[] filedata) {
        try {
            PostPacket packet = new PostPacket(getId(), content, filename, filedata);
            feedOutput.writeObject(packet);
            feedOutput.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


//    public void sendPostToFeed(String content) {
//        try {
//            feedOutput.writeObject(content);
//            feedOutput.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public void sendReaction(int postId, String reactionType) {
        System.out.println("Hi I am a pretty little reaction trying to reach server Thread");
        try {
            String message = "REACTION|" + postId + "|" + this.id + "|" + reactionType;
            serverOutput.writeObject(message);
            serverOutput.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




}