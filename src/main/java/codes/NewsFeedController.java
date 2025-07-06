package codes;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class NewsFeedController {
    @FXML
    public StackPane NewsFeedLayout;
    @FXML
    public Group NewsFeedView;
    
    Client client;
    Stage stage;

    public void setNewsFeedController(Client client, Stage stage) {
        this.client = client;
        this.stage = stage;
        NewsFeedLayout.setPrefWidth(Screen.SCREENWIDTH);
        NewsFeedLayout.setPrefHeight(Screen.SCREENHEIGHT);
        NewsFeedView.scaleXProperty().bind(NewsFeedLayout.widthProperty().divide(1600));
        NewsFeedView.scaleYProperty().bind(NewsFeedLayout.heightProperty().divide(900));
    }

    @FXML
    public void onChatButtonClicked(ActionEvent mouseEvent) throws IOException {
        // Loading inbox page

        client.getInbox().startInboxView(client, stage);
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
}
