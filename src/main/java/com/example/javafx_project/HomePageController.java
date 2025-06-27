package com.example.javafx_project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.Stage;

import java.io.IOException;

public class HomePageController {
    Client client;
    Stage stage;

    public void setHomePageController(Client client, Stage stage) {
        this.client = client;
        this.stage = stage;
    }

    @FXML
    public void onNewsFeedButtonClick(ActionEvent actionEvent) throws IOException {
        client.getNewsFeed().startNewsFeedView(client, stage);
    }

    @FXML
    public void onChatButtonClick(ActionEvent actionEvent) throws IOException {
        client.getInbox().startInboxView(client, stage);
    }
}
