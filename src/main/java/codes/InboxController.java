package codes;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class InboxController {
    @FXML
    public StackPane InboxLayout;
    @FXML
    public Group InboxView;
    @FXML
    public TextField Recipient;
    @FXML
    public TextArea Chat;
    @FXML
    public TextArea Message;
    @FXML
    public Label User;

    Client client;
    Stage stage;

    public void setInboxController(Client client, Stage stage) {
        this.client = client;
        this.stage = stage;
        InboxLayout.setPrefWidth(Screen.SCREENWIDTH);
        InboxLayout.setPrefHeight(Screen.SCREENHEIGHT);
        InboxView.scaleXProperty().bind(InboxLayout.widthProperty().divide(1600));
        InboxView.scaleYProperty().bind(InboxLayout.heightProperty().divide(900));
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

    public String getRecipientId() {
        // Taking recipient id from the input field

        String recipientId = Recipient.getText();
        recipientId = recipientId.substring(0, recipientId.length() - "@gmail.com".length());

        Recipient.clear();

        return recipientId;
    }

    public String getMessage() {
        // Taking message from the input field

        String message = Message.getText();
        message = message.substring(0, message.length() - 1);

        Message.clear();

        return message;
    }
}
