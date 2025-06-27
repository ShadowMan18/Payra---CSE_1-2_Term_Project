package com.example.javafx_project;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class NotificationPage {
    private final int screenWidth = Screen.getWidth() - 5;
    private final int screenHeight = Screen.getHeight() - 35;

    public NotificationPage(){}

    public void startNotificationPageView(Client client, Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("NotificationPage.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), screenWidth, screenHeight);
        NotificationPageController notificationPageController = fxmlLoader.getController();
        notificationPageController.setNotificationPageController(client, stage);
        stage.setTitle("Notification");
        stage.setScene(scene);
        stage.show();
    }

    public void stopNotificationPageView(){}
}
