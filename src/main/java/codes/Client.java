package codes;

import javafx.application.Platform;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
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
    private boolean registered;
    private boolean active;
    private String receiverId;
    private CountDownLatch latch;

    // Client pages

    private final IntroPage introPage;
    private final LoginPage loginPage;
    private final SignupPage signupPage;
    private final HomePage homePage;
    private final Inbox inbox;
    private final NewsFeed newsFeed;
    private final ProfilePage profilePage;
    private final NotificationPage notificationPage;

    // Client network
    private final String ipAddress = "127.0.0.1";
 //   private final String ipAddress = "192.168.252.50";
    private final Socket serverSocket;
    private final ObjectOutputStream serverOutput;
    private final ObjectInputStream serverInput;
    private Socket chatSocket;
    private ObjectOutputStream chatOutput;
    private ObjectInputStream chatInput;
    private boolean inChat;


    //NewsFeed
    private Socket feedSocket;
    private ObjectInputStream feedInput;
    private ObjectOutputStream feedOutput;
    private NewsFeedController newsFeedController;


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

    // Constructor

    public Client() {
        this.registered = false;
        this.active = false;
        this.receiverId = null;
        this.introPage = new IntroPage();
        this.loginPage = new LoginPage();
        this.signupPage = new SignupPage();
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

        this.inChat = false;

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
                    throw new RuntimeException(e);
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
                else if (fromServer instanceof String string && string.startsWith("info:")) {
                    String[] info = string.substring("info:".length()).split(",");
                    this.id = info[0];
                    this.email  = id + "@gmail.com";
                    this.firstName = info[1];
                    this.lastName = info[2];
                    this.password = info[3];
                    this.recoveryQuestion = info[4];
                    this.recoveryAnswer = info[5];

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
                else if (fromServer instanceof String string && string.startsWith("connect_to:")) {
                    String[] connectionInfo = string.substring("connect_to:".length()).split(",");
                    int port = Integer.parseInt(connectionInfo[0]);
                    this.receiverId = connectionInfo[1];
                    connectToChatServer(port);
                    inChat = true;

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

    public boolean isRegistered() {
        return registered;
    }

    public boolean isActive() {
        return active;
    }

    public String getReceiverId() {
        return receiverId;
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


    // Might be necessary

    public Socket getServerSocketSocket() {
        return serverSocket;
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

    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    @Override
    public String toString() {
        return id + "," + password + "," + firstName + "," + lastName;
    }

    // Public methods

    public void connectToChatServer(int port) {
        try {
            this.chatSocket = new Socket(ipAddress, port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Connected to: " + port);

        try {
            this.chatOutput = new ObjectOutputStream(chatSocket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            this.chatInput = new ObjectInputStream(chatSocket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
            this.feedOutput.flush();
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
                    String feedUpdate = (String) feedInput.readObject();
                    System.out.println("Feed: " + feedUpdate);

                    Platform.runLater(() -> {
                        if (newsFeedController != null) {
                            newsFeedController.addPostToFeed(feedUpdate);
                        }
                    });

                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    public void sendPostToFeed(String content) {
        try {
            feedOutput.writeObject(content);
            feedOutput.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
