package codes;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class InboxController {
    @FXML
    public StackPane InboxLayout;
    @FXML
    public Group InboxView;
    
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
        client.getNewsFeed().startNewsFeedView(client, stage);
    }

    @FXML
    public void onHomeButtonClicked(ActionEvent mouseEvent) throws IOException {
        client.getHomePage().startHomePageView(client, stage);
    }

    @FXML
    public void onNotificationButtonClick(ActionEvent actionEvent) throws IOException {
        client.getNotificationPage().startNotificationPageView(client, stage);
    }

    public void onProfileButtonClick(ActionEvent actionEvent) throws IOException {
        client.getProfilePage().startProfilePageView(client, stage);
    }
}
