package com.example.javafx_project;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class SignupPage {
    private final int screenWidth = Screen.getWidth() - 5;
    private final int screenHeight = Screen.getHeight() - 35;

    public SignupPage(){}

    public void startSignupPageView(Client client, Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("SignupPage.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), screenWidth, screenHeight);
        SignupPageController signupPageController = fxmlLoader.getController();
        signupPageController.setSignupPageController(client, stage);
        stage.setTitle("Signup");
        stage.setScene(scene);
        stage.show();
    }

    public void stopSignupPageView(){}

}


