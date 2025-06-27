package com.example.javafx_project;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginPage {
    private final int screenWidth = Screen.getWidth() - 5;
    private final int screenHeight = Screen.getHeight() - 35;

    public LoginPage(){}

    public void startLoginPageView(Client client, Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("LoginPage.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), screenWidth, screenHeight);
        LoginPageController loginPageController = fxmlLoader.getController();
        loginPageController.setLoginPageController(client, stage);
        stage.setTitle("Login");
        stage.setScene(scene);
        stage.show();
    }

    public void stopLoginPageView(Client client){
    }

}
