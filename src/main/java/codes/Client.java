package codes;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;


public class Client {
    // Client information

    private ClientInfo info;
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
    private CountDownLatch friendStatusLatch;
    private String latestFriendStatusResponse;

    // Client pages

    private Stage stage;
    private final IntroPage introPage;
    private final LoginPage loginPage;
    private final SignupPage signupPage;
    private final ProfilePicturePage profilePicturePage;
    private final ForgotPasswordPage forgotPasswordPage;
    private final HomePage homePage;
    private final Inbox inbox;
    private final NewsFeed newsFeed;

    // Client server network

//    private final String ipAddress = "192.168.112.229";
        private final String ipAddress = "127.0.0.1";
    private final Socket serverSocket;
    private final ObjectOutputStream serverOutput;
    private final ObjectInputStream serverInput;

    // Chat server network

    private Socket chatSocket;
    private ObjectOutputStream chatOutput;
    private ObjectInputStream chatInput;
    public Vector<ClientInfo> clients;
    private String receiverId;
    private boolean chatStatus;
    private boolean newNotification;
    public Map<String, Pair<String, String>> notification = new LinkedHashMap<>();

    // Call network

    private String receiverIPAddress;
    private ClientInfo callerInfo;
    private String callAcceptanceStatus;
    private boolean callStatus;

    // NewsFeed server network

    private Socket feedSocket;
    private ObjectInputStream feedInput;
    private ObjectOutputStream feedOutput;
    private NewsFeedController newsFeedController;
    private boolean connectedToNewsFeed;
    private final Map<Integer, Consumer<List<String>>> commentCallbacks = new HashMap<>();




    private List<ClientInfo> friendList = new Vector<>();
    private List<ClientInfo> pendingRequests = new Vector<>();

    public static final Map<String, ObjectOutputStream> feedClients = new ConcurrentHashMap<>();



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

    public static void broadcast(Object obj) {
        System.out.println("Hi, yes, you have reached broadcast for comments and co.");
        for (ObjectOutputStream out : allClientOutputs) {
            try {
                out.writeObject(obj);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
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
        this.gettingRequests=false;
        this.gettingFriends=false;

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
                    if (!active) {
                        this.info = info;
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
                    }
                    else {
                        callerInfo = info;
                    }

                    if (latch != null) {
                        latch.countDown();
                        latch = null;
                    }
                }
                else if (fromServer instanceof Vector<?> v) {

                    if(friendsSentTheMessage){
                        friendsSentTheMessage=false;
                        System.out.println("Yes, I did come here for newsFeed");
                        if (!v.isEmpty() && v.get(0) instanceof ClientInfo) {
                            clients = (Vector<ClientInfo>) v;
                            clients.sort(Comparator.comparing(ClientInfo::getFirstName));
                            receiveClientList(clients);
                        }
                    }
                    else{
                        System.out.println("Yes, I did come here for Messenger");
                        if (!v.isEmpty() && v.get(0) instanceof ClientInfo) {
                            clients = (Vector<ClientInfo>) v;
                            clients.sort(Comparator.comparing(ClientInfo::getFirstName));
                        }

                        if (latch != null) {
                            latch.countDown();
                            latch = null;
                        }
                    }
                }
                else if (fromServer instanceof Map<?, ?> map) {

                    if (!map.isEmpty() && map.keySet().iterator().next() instanceof String) {
                        Map<String, String> statusMap = (Map<String, String>) map;
                        setFriendStatusMap(statusMap);
                        System.out.println("Received friend status map of size: " + statusMap.size());
                    }
                    else{
                        if(fetchFriendStatusLatch!=null){
                            fetchFriendStatusLatch.countDown();
                            fetchFriendStatusLatch=null;
                        }
                    }
                }
                else if (fromServer instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof String str && str.startsWith("comment:")) {
                    // It's a list of comments for a post
                    int postId = Integer.parseInt(str.split(":")[1]); // Format: "comment:postId"
                    List<String> comments = new ArrayList<>();
                    for (Object obj : list) {
                        if (obj instanceof String s) comments.add(s);
                    }

                    Consumer<List<String>> callback;
                    synchronized (commentCallbacks) {
                        callback = commentCallbacks.remove(postId);
                    }

                    if (callback != null) {
                        List<String> finalComments = comments;
                        Platform.runLater(() -> callback.accept(finalComments));
                    }
                }



                else if (fromServer instanceof List<?> list) {

                    //System.out.println("Client: I got the List of size: "+list.size());

                    if (!list.isEmpty() && list.get(0) instanceof ClientInfo) {
                        List<ClientInfo> incoming = (List<ClientInfo>) list;
                        //System.out.println("Yep, is getting requests true: "+gettingRequests);

                        if (friendsSentTheMessage) {
                            System.out.println("No please");
                            this.clients = new Vector<>(incoming);
                            this.clients.sort(Comparator.comparing(ClientInfo::getFirstName));
                            friendsSentTheMessage = false;

                            if (clientListLatch != null) {
                                clientListLatch.countDown();
                                clientListLatch = null;
                            }
                        } else if (latch != null) {

                            if (gettingRequests) {
                                // System.out.println("Hi I am setting my pending requests");
                                this.pendingRequests = incoming;
                                //System.out.println("Hi this is client and pending request: "+pendingRequests.size());
                                gettingRequests=false;
                            } else if(gettingFriends) {
                                //System.out.println("Hi I am setting my friend list");
                                this.friendList = incoming;
                                gettingFriends=false;
                            }

                            latch.countDown();
                            latch = null;
                        } else {

                            this.clients = new Vector<>(incoming);
                            this.clients.sort(Comparator.comparing(ClientInfo::getFirstName));
                        }
                    } else {
                        System.out.println("Received empty list");
                        if (friendsSentTheMessage) {
                            this.clients = new Vector<>();
                            if (clientListLatch != null) {
                                clientListLatch.countDown();
                                clientListLatch = null;
                            }
                        } else if (latch != null) {
                            this.friendList = new Vector<>();
                            this.pendingRequests = new Vector<>();
                            latch.countDown();
                            latch = null;
                        }
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
                    System.out.println(receiverIPAddress);

                    if (latch != null) {
                        latch.countDown();
                        latch = null;
                    }
                }
                else if (fromServer instanceof String string && string.startsWith("call:")) {
                    System.out.println(string);
                    String[] callInfo = string.substring("call:".length()).split(",");

                    String callType = callInfo[0];
                    String callerIPAddress = callInfo[1];

                    while (callerInfo == null || callerInfo.getProfilePicture() == null) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    callAcceptanceStatus = null;
                    CallRinger.startReceiverEndRinger(this, callerInfo, info, callType, callerIPAddress);

                    callerInfo = null;
                }
                else if (fromServer instanceof String string && string.startsWith("call_response:")) {
                    String response = string.substring("call_response:".length());

                    if (response.equals("accepted")) {
                        callAcceptanceStatus = response;
                        System.out.println("call accepted in " + firstName);
                    }
                    else {
                        callAcceptanceStatus = response;
                        System.out.println("call declined in " + firstName);
                    }
                }
                else if (fromServer instanceof String string && string.equals("call_ended")) {
                    callAcceptanceStatus = "ended";
                    System.out.println("call ended in " + firstName);
                }
                else if (fromServer instanceof String string && string.startsWith("NewsFeed connection:")) {
                    int port = Integer.parseInt(string.substring("NewsFeed connection:".length()));

                    connectToFeedServer(port);

                    if (latch != null) {
                        latch.countDown();
                        latch = null;
                    }
                }
                else if (fromServer instanceof String string && (string.equals("send") || string.equals("pending") || string.equals("friends"))) {
                    if (friendStatusLatch != null) {
                        latestFriendStatusResponse = string;
                        friendStatusMap.put(latestFriendStatusQueryTarget, string);
                        friendStatusLatch.countDown();
                        friendStatusLatch = null;
                    }
                }

                else if (fromServer instanceof String string) {
                    System.out.println("Received: " + string);
                }


                else {
                    System.out.println("Received unknown type: " + fromServer.getClass());
                }

            }
        }).start();


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

    public ClientInfo getInfo() {
        return info;
    }

    public String getFullName(){
        return firstName+" "+lastName;
    }

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

    public Stage getStage() {
        return stage;
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

    public String  getCallAcceptanceStatus() { return callAcceptanceStatus; };

    public boolean getCallStatus() { return callStatus; }

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

    public void setProfilePicture(Image image) { this.profilePicture = image; }

    public void setChatStatus(boolean status) { chatStatus = status; }

    public void resetReceiverId() { receiverId = null; }

    public void resetCallAcceptanceStatus() { callAcceptanceStatus = null; }

    public void setCallStatus(boolean status) { callStatus = status; }

    public void resetNotificationStatus() { newNotification = false; }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

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
                    System.out.println("I am here to add the posts to the UI really. This part seems to be fine");

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
                                System.out.println("Add postToFeed called!!!!!");
                                newsFeedController.addPostToFeed(packet); // Also unambiguous
                            }
                            else if (feedUpdate instanceof CommentPacket comment) {
                                System.out.println("Yes, I did get the comment and I did send it, swear, " +
                                        "controller needs to up it's game");
                                newsFeedController.addLiveComment(comment);

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

    public String getMyId() {
        return this.id;
    }

    public Vector<ClientInfo> getAllClients() {
        return this.clients;
    }
    private String latestFriendStatusQueryTarget;

    public String getFriendStatus(String otherId) {
        try {
            friendStatusLatch = new CountDownLatch(1);
            latestFriendStatusQueryTarget = otherId;
            serverOutput.writeObject("friend_status:" + this.id + ":" + otherId);
            serverOutput.flush();
            friendStatusLatch.await();
            return latestFriendStatusResponse != null ? latestFriendStatusResponse : "unknown";
        } catch (Exception e) {
            e.printStackTrace();
            return "unknown";
        }
    }
    public void setFriendStatusMapEntry(String userId, String status) {
        friendStatusMap.put(userId, status);
    }


    boolean gettingRequests;
    public void sendFriendRequest(String receiverId) {
        try {
            gettingRequests=true;
            serverOutput.writeObject("friend_request:" + this.id + ":" + receiverId);
            serverOutput.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    boolean gettingFriends=false;
    public List<ClientInfo> getFriendList() {
        try {
            CountDownLatch friendLatch = new CountDownLatch(1);
            this.latch = friendLatch;

            gettingFriends=true;
            serverOutput.writeObject("get_friends");
            serverOutput.flush();

            friendLatch.await();

            gettingFriends=false;
            return friendList;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Vector<>();
    }

    public List<ClientInfo> getPendingRequests() {
        //System.out.println("Requesting the server to send the pending requests");
        try {
            gettingRequests=true;
            CountDownLatch requestLatch = new CountDownLatch(1);
            this.latch = requestLatch;

            serverOutput.writeObject("get_requests");
            serverOutput.flush();

            requestLatch.await();


            gettingRequests=false;
            return pendingRequests;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Vector<>();
    }

    public void acceptFriendRequest(String senderId) {
        try {
            serverOutput.writeObject("friend_accept:" + senderId);
            serverOutput.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void rejectFriendRequest(String senderId) {
        try {
            serverOutput.writeObject("friend_reject:" + senderId);
            serverOutput.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void unfriend(String friendId) {
        try {
            serverOutput.writeObject("unfriend:" + friendId);
            serverOutput.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private CountDownLatch clientListLatch;
    boolean friendsSentTheMessage=false;


    public void fetchClients() {
        try {
            clientListLatch = new CountDownLatch(1);
            friendsSentTheMessage=true;
            serverOutput.writeObject("load_clients");
            serverOutput.flush();


            clientListLatch.await();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void receiveClientList(Vector<ClientInfo> received) {
        this.clients = received;
        if (clientListLatch != null) {
            clientListLatch.countDown();
            clientListLatch = null;
        }
    }


    public CountDownLatch fetchFriendStatusLatch;

    public void fetchFriendStatusMap() {
        try {
            fetchFriendStatusLatch=new CountDownLatch(1);
            serverOutput.writeObject("get_friend_status_map");
            serverOutput.flush();
            try {
                fetchFriendStatusLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> friendStatusMap = new ConcurrentHashMap<>();


    public void setFriendStatusMap(Map<String, String> map) {
        friendStatusMap = map;
        if (fetchFriendStatusLatch != null) {
            fetchFriendStatusLatch.countDown();
            fetchFriendStatusLatch = null;
        }
    }

    public String getCachedFriendStatus(String userId) {
        return friendStatusMap.getOrDefault(userId, "send");
    }


    public static void addFeedClient(String id, ObjectOutputStream out) {
        feedClients.put(id, out);
    }

    public static void removeFeedClient(String id) {
        feedClients.remove(id);
    }


    public void sendComment(int postId, String commentText) {
        try {
            String safeComment = commentText.replace("|", "[PIPE]");

            String message = "send_comment|" + postId + "|" + getId() + "|" + safeComment;
            serverOutput.writeObject(message);
            serverOutput.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void fetchCommentsForPost(int postId, Consumer<List<String>> callback) {
        synchronized (commentCallbacks) {
            commentCallbacks.put(postId, callback);
        }

        try {
            serverOutput.writeObject("get_comments|" + postId);
            serverOutput.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}