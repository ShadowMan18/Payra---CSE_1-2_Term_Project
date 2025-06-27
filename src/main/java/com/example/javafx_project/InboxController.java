package com.example.javafx_project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.Stage;

import java.io.IOException;

public class InboxController {
    Client client;
    Stage stage;

    public void setInboxController(Client client, Stage stage) {
        this.client = client;
        this.stage = stage;
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
}
