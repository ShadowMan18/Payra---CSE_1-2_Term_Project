package com.example.javafx_project;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.awt.*;
import javax.swing.*;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import javafx.embed.swing.SwingNode;
import javafx.scene.layout.StackPane;
import javafx.application.Platform;

import java.io.IOException;

// Main for Shanon

public class Main extends Application {
    public static final int screenWidth = Screen.getWidth() - 5;
    public static final int screenHeight = Screen.getHeight() - 35;
    public static Stage stage;

    @Override
    public void start(Stage stage) throws IOException {
        this.stage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("HomePage.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), screenWidth, screenHeight);
        stage.setTitle("Main");
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args) {
        launch();
    }
}

// Main for Arana

//public class Main extends Application {
//    public static final int screenWidth = Screen.getWidth() - 5;
//    public static final int screenHeight = Screen.getHeight() - 35;
//    public static Stage stage;
//
//    @Override
//    public void start(Stage stage) throws IOException {
//        this.stage = stage;
//        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("HomePage.fxml"));
//        Scene scene = new Scene(fxmlLoader.load(), screenWidth, screenHeight);
//        stage.setTitle("Main");
//        stage.setScene(scene);
//        stage.show();
//    }
//    public static void main(String[] args) {
//        launch();
//    }
//}

