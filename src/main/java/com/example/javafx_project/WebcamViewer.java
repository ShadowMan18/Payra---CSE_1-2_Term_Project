package com.example.javafx_project;

import javafx.scene.Scene;
import javafx.stage.Stage;
import java.awt.*;
import javax.swing.*;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import javafx.embed.swing.SwingNode;
import javafx.scene.layout.StackPane;
import javafx.application.Platform;

public class WebcamViewer {
    private static final int screenWidth = (int)(Screen.getWidth() - 5);
    private static final int screenHeight = (int)(Screen.getHeight() - 35);

    public static void viewWebcam(Stage stage)
    {
        Webcam webcam = Webcam.getDefault();
        if (webcam == null) {
            System.err.println("No webcam detected.");
            Platform.exit();
            return;
        }

        webcam.setViewSize(new Dimension(640, 480));
        webcam.open();

        WebcamPanel webcamPanel = new WebcamPanel(webcam);
        webcamPanel.setFPSDisplayed(true);
        webcamPanel.setMirrored(true);

        SwingNode swingNode = new SwingNode();
        SwingUtilities.invokeLater(() -> swingNode.setContent(webcamPanel));
        StackPane root = new StackPane(swingNode);
        Scene scene = new Scene(root, screenWidth, screenHeight);
        stage.setTitle("JavaFX Webcam Viewer");
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(e -> {
            webcam.close();
            Platform.exit();
        });
    }
}

