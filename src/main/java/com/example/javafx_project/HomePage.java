package com.example.javafx_project;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

import java.io.IOException;

public class HomePage {
    public static void viewHome()
    {
        try
        {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("HomePage.fxml"));
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


