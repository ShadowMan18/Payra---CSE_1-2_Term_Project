package com.example.javafx_project;

import javafx.application.Application;
import javafx.stage.Stage;

public class ClientWindow extends Application{
    static Client client;

    @Override
    public void start(Stage stage) throws Exception {
        client.getLoginPage().startLoginPageView(client, stage);
    }

    public static void main(String[] args) {
        client = new Client();
        launch();
    }
}
