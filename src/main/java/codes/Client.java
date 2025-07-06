package codes;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.Vector;

public class Client {
    // Client information

    private String firstName;
    private String lastName;
    private String email;
    private String id;
    private String password;

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

    private final Socket serverSocket;
    private final ObjectOutputStream serverOutput;
    private final ObjectInputStream serverInput;
    private Socket chatSocket;
    private ObjectOutputStream chatOutput;
    private ObjectInputStream chatInput;
    private Vector<String> connectedId;
    private boolean loginStatus;
    private boolean inChat;

    // Constructor

    public Client() {
        this.introPage = new IntroPage();
        this.loginPage = new LoginPage();
        this.signupPage = new SignupPage();
        this.homePage = new HomePage();
        this.inbox = new Inbox();
        this.newsFeed = new NewsFeed();
        this.profilePage = new ProfilePage();
        this.notificationPage = new NotificationPage();

        try {
            this.serverSocket = new Socket("192.168.1.104", 1024);
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

        this.connectedId = new Vector<>();

        this.loginStatus = false;
        this.inChat = false;

        Thread Writer = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String message = scanner.nextLine();

                if (inChat) {
                    try {
                        String[] splittedMessage = message.split(",");
                        int clientIndex = Integer.parseInt(splittedMessage[0]);
                        chatOutput.writeObject(message);
                        chatOutput.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                else {
                    try {
                        serverOutput.writeObject(message);
                        serverOutput.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        Thread serverReader = new Thread(() -> {
            while (true) {
                String message;

                try {
                    message = (String) (serverInput.readObject());
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

                if (message.startsWith("connect_to:")) {
                    int port = Integer.parseInt(message.substring("connect_to:".length()));
                    connectToChatServer(port);
                    inChat = true;
                } else {
                    System.out.println("Received: " + message);
                }
            }
        });

        Writer.start();
        serverReader.start();
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

    public Socket getSocket() {
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

    public int getIdIndex(String id) {
        return connectedId.indexOf(id);
    }

    public boolean getLoginStatus() { return loginStatus; }

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

    public void setLoginStatus(boolean status)
    {
        this.loginStatus = status;
    }

    @Override
    public String toString() {
        return id + "," + firstName + "," + lastName + "," + password;
    }

    // Public methods

    public boolean isConnected(String id) {
        return connectedId.contains(id);
    }

    public void connectToChatServer(int port) {
        try {
            this.chatSocket = new Socket("192.168.1.104", port);
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
}
