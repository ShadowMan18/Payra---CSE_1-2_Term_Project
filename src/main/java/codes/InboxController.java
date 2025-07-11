package codes;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class InboxController {
    @FXML
    public StackPane InboxLayout;
    @FXML
    public Group InboxView;
    @FXML
    public Label User;
    @FXML
    public TextField SearchBar;
    @FXML
    public ImageView ChatBox;
    @FXML
    public ImageView ReceiverProfilePicture;
    @FXML
    public Label ReceiverName;
    @FXML
    public TextField Message;
    @FXML
    public ScrollPane MessageScroller;
    @FXML
    public VBox MessageContainer;


    private Client client;
    private Stage stage;
    private String receiverId;
    private Image receiverProfilePicture;
    private String lastSender;


    public void setInboxController(Client client, Stage stage) {
        this.client = client;
        this.stage = stage;
        client.setChatStatus(false);
        Message.setOpacity(0);
        Message.setDisable(true);
        InboxLayout.setPrefWidth(Screen.SCREENWIDTH);
        InboxLayout.setPrefHeight(Screen.SCREENHEIGHT);
        InboxView.scaleXProperty().bind(InboxLayout.widthProperty().divide(1600));
        InboxView.scaleYProperty().bind(InboxLayout.heightProperty().divide(900));
        MessageScroller.setFitToWidth(true);
        MessageContainer.setFillWidth(true);
        MessageContainer.setMinHeight(Region.USE_PREF_SIZE);
    }

    @FXML
    public void onNewsFeedButtonClicked(ActionEvent mouseEvent) throws IOException {
        // Loading news feed page

        client.getNewsFeed().startNewsFeedView(client, stage);
    }

    @FXML
    public void onHomeButtonClicked(ActionEvent mouseEvent) throws IOException {
        // Loading home page

        client.getHomePage().startHomePageView(client, stage);
    }

    @FXML
    public void onNotificationButtonClick(ActionEvent actionEvent) throws IOException {
        // Loading notification page

        client.getNotificationPage().startNotificationPageView(client, stage);
    }

    public void onProfileButtonClick(ActionEvent actionEvent) throws IOException {
        // Loading profile page

        client.getProfilePage().startProfilePageView(client, stage);
    }

    public void onSearchButtonClicked(ActionEvent actionEvent) {
        // Taking recipient id from the input field

        receiverId = SearchBar.getText().trim();
        receiverId = receiverId.substring(0, receiverId.length() - "@gmail.com".length());

        SearchBar.clear();

        // Sending chat command with recipient id to the server

        try {
            CountDownLatch latch = new CountDownLatch(1);
            client.setLatch(latch);

            client.getServerOutput().writeObject("chat_with:" + receiverId);
            client.getServerOutput().flush();

            latch.await();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (!receiverId.equals(client.getReceiverId())) {
            System.out.println("Unable to connect");
            return;
        }

        client.setChatStatus(true);

        Image chatBox = new Image(String.valueOf(getClass().getResource("/images/ChatBox.png")));
        ChatBox.setImage(chatBox);
        Message.setOpacity(1);
        Message.setDisable(false);

        ReceiverName.setText(client.getReceiverName());

        receiverProfilePicture = new Image(String.valueOf(getClass().getResource("/images/Payra.png")));
        ReceiverProfilePicture.setImage(receiverProfilePicture);
        Circle clip = new Circle(30, 30, 30);
        ReceiverProfilePicture.setClip(clip);


        // Starting chat reader thread (Receives message from the chat server and shows it in the chat box)

        new Thread(() -> {
            while (true) {
                Object messageInfo;

                try {
                    messageInfo = client.getChatInput().readObject();
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

                String sender = ((String) messageInfo).split(",")[0];
                String timestamp = ((String) messageInfo).split(",")[1];
                String message = ((String) messageInfo).substring(sender.length() + timestamp.length() + 2);

                Platform.runLater(() -> {
                    addMessage(message, sender);
                });
            }
        }).start();
    }

    void addMessage(String message, String sender) {
        ImageView ProfilePicture = new ImageView(String.valueOf(getClass().getResource("/images/WhiteBackground.png")));
        Label SenderLabel = new Label();

        ProfilePicture.setFitWidth(40);
        ProfilePicture.setFitHeight(40);
        ProfilePicture.setSmooth(true);
        Circle clip = new Circle(20, 20, 20);
        ProfilePicture.setClip(clip);

        SenderLabel.setStyle("-fx-font-family: Arial Rounded MT Bold; -fx-font-size: 10; -fx-text-fill: #000000");
        HBox SenderBox = new HBox(SenderLabel);

        Label MessageLabel = new Label(message);
        MessageLabel.setMaxWidth(450);
        MessageLabel.setPrefWidth(Region.USE_COMPUTED_SIZE);
        MessageLabel.setPrefHeight(Region.USE_COMPUTED_SIZE);
        MessageLabel.setWrapText(true);

        if (sender.equals(client.getId())) {
            if (!sender.equals(lastSender)) {
                MessageLabel.setStyle("-fx-font-family: Arial Rounded MT Bold; -fx-font-size: 18; -fx-font-weight: bold; -fx-padding: 8; -fx-background-color: #75baff; -fx-background-radius: 20 0 20 20;");
            }
            else {
                MessageLabel.setStyle("-fx-font-family: Arial Rounded MT Bold; -fx-font-size: 18; -fx-font-weight: bold; -fx-padding: 8; -fx-background-color: #75baff; -fx-background-radius: 20 20 20 20;");
            }
        }
        else {
            if (!sender.equals(lastSender)) {
                MessageLabel.setStyle("-fx-font-family: Arial Rounded MT Bold; -fx-font-size: 18; -fx-font-weight: bold; -fx-padding: 8; -fx-background-color: #b6b9c0; -fx-background-radius: 0 20 20 20;");
            }
            else {
                MessageLabel.setStyle("-fx-font-family: Arial Rounded MT Bold; -fx-font-size: 18; -fx-font-weight: bold; -fx-padding: 8; -fx-background-color: #b6b9c0; -fx-background-radius: 20 20 20 20;");
            }
        }

        VBox TextContainer;

        if (sender.equals(client.getId()) && !sender.equals(lastSender)) {
            ProfilePicture.setImage(client.getProfilePicture());
            SenderLabel.setText(client.getFirstName());
            SenderBox.setAlignment(Pos.CENTER_RIGHT);
            TextContainer = new VBox(SenderBox, MessageLabel);
            lastSender = client.getId();
        }
        else if (sender.equals(receiverId) && !sender.equals(lastSender)) {
            ProfilePicture.setImage(receiverProfilePicture);
            SenderLabel.setText(client.getReceiverFirstName());
            SenderBox.setAlignment(Pos.CENTER_LEFT);
            TextContainer = new VBox(SenderBox, MessageLabel);
            lastSender = receiverId;
        }
        else {
            TextContainer = new VBox(MessageLabel);
        }

        TextContainer.setSpacing(5);

        HBox MessageBubble;

        if (sender.equals(client.getId())) {
            MessageBubble = new HBox(TextContainer, ProfilePicture);
        }
        else {
            MessageBubble = new HBox(ProfilePicture, TextContainer);
        }

        MessageBubble.setSpacing(10);

        if (sender.equals(client.getId())) {
            MessageContainer.setAlignment(Pos.CENTER_RIGHT);
            MessageBubble.setAlignment(Pos.CENTER_RIGHT);
        }
        else {
            MessageContainer.setAlignment(Pos.CENTER_LEFT);
            MessageBubble.setAlignment(Pos.CENTER_LEFT);
        }

        MessageContainer.getChildren().add(MessageBubble);

        Platform.runLater(() -> {
            MessageContainer.layout();
            MessageScroller.layout();
            MessageScroller.setVvalue(1.0);
        });
    }

    public void onSendButtonClicked(ActionEvent actionEvent) {
        sendMessage();
    }

    public void onEnterKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.ENTER)) {
            sendMessage();
        }
    }

    public void onAttachButtonClicked(ActionEvent actionEvent) {
        if (client.getChatStatus()) {

        }
    }

    public void onAudioCallButtonClicked(ActionEvent actionEvent) {
        if (client.getChatStatus()) {

        }
    }

    public void onVideoCallButtonClicked(ActionEvent actionEvent) {
        if (client.getChatStatus()) {

        }
    }

    public void sendMessage() {
        if (client.getChatStatus()) {
            String message = Message.getText();
            Message.clear();

            if (message.isEmpty()) {
                return;
            }

            try {
                client.getChatOutput().writeObject(message);
                client.getChatOutput().flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
