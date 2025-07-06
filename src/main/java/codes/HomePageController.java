package codes;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class HomePageController {
    @FXML
    public StackPane HomePageLayout;
    @FXML
    public Group HomePageView;

    Client client;
    Stage stage;

    public void setHomePageController(Client client, Stage stage) {
        this.client = client;
        this.stage = stage;
        HomePageLayout.setPrefWidth(Screen.SCREENWIDTH);
        HomePageLayout.setPrefHeight(Screen.SCREENHEIGHT);
        HomePageView.scaleXProperty().bind(HomePageLayout.widthProperty().divide(1600));
        HomePageView.scaleYProperty().bind(HomePageLayout.heightProperty().divide(900));
    }

    @FXML
    public void onNewsFeedButtonClick(ActionEvent actionEvent) throws IOException {
        // Loading news feed page

        client.getNewsFeed().startNewsFeedView(client, stage);
    }

    @FXML
    public void onChatButtonClick(ActionEvent actionEvent) throws IOException {
        // Loading inbox page

        client.getInbox().startInboxView(client, stage);
    }
}
