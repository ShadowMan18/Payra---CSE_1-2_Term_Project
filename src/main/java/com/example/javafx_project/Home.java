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

public class Home {
    public static void viewHome()
    {
        try
        {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("Home.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), Main.screenWidth, Main.screenHeight);
            Main.stage.setTitle("Home");
            Main.stage.setScene(scene);
            Main.stage.show();
        }
        catch(IOException e)
        {
            System.err.println("Couldn't load homepage");
        }
    }
}


