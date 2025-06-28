package codes;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Vector;

public class Client {
    // All clients

    static Vector<Client> clients;

    // Client information

    private String firstName;
    private String lastName;
    private String userName;
    private String email;
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

    private final Socket socket;
    private final ObjectOutputStream output;
    private final ObjectInputStream input;

    // Constructor

    public Client() {
        System.out.println("Client created");


        this.introPage = new IntroPage();
        this.loginPage = new LoginPage();
        this.signupPage = new SignupPage();
        this.homePage = new HomePage();
        this.inbox = new Inbox();
        this.newsFeed = new NewsFeed();
        this.profilePage = new ProfilePage();
        this.notificationPage = new NotificationPage();


        try {
            this.socket = new Socket("127.0.0.1", 4349);
            this.output = new ObjectOutputStream(socket.getOutputStream());
            this.input = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        clients.add(this);



//       Server.currentClients.add(this);
    }



    // Send message to server
    public void send(String msg) {
        try {
            output.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Read message from server
    public String receive() {
        try {
            return (String) input.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Close the connection
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Client)) return false;
        return userName.equals(((Client) obj).getUserName());
    }

    @Override
    public int hashCode() {
        return userName.hashCode();
    }



    // Getters

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getUserName() {
        return userName;
    }

    public String getEmail() {
        return email;
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
        return socket;
    }

    public ObjectOutputStream getOutput() {
        return output;
    }

    public ObjectInputStream getInput() {
        return input;
    }

    // Setters

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
