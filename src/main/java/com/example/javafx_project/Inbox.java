package com.example.javafx_project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Inbox {
    private final int screenWidth = Screen.getWidth() - 5;
    private final int screenHeight = Screen.getHeight() - 35;

    public Inbox(){}

    public void startInboxView(Client client, Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Inbox.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), screenWidth, screenHeight);
        InboxController inboxController = fxmlLoader.getController();
        inboxController.setInboxController(client, stage);
        stage.setTitle("Inbox");
        stage.setScene(scene);
        stage.show();
    }

    public void stopInboxView(){}
}


