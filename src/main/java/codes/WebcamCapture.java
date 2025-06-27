package codes;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import javafx.embed.swing.SwingNode;
import javafx.scene.layout.StackPane;
import javafx.application.Platform;

public class WebcamCapture {
    private static Webcam webcam;

    public WebcamCapture(){}

    public static void startWebcam(Stage stage)
    {
        webcam = Webcam.getDefault();
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
        StackPane webcamPane = new StackPane();
        webcamPane.getChildren().add(swingNode);
        FXMLLoader fxmlLoader = new FXMLLoader(WebcamCapture.class.getResource("Webcam.fxml"));
        try{
            webcamPane.getChildren().add(fxmlLoader.load());
            Scene scene = new Scene(webcamPane, Main.screenWidth, Main.screenHeight);
            stage.setTitle("JavaFX Webcam Viewer");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Unable to load webcam.");
        }

        stage.setOnCloseRequest(e -> {
            stopWebcam();
            Platform.exit();
        });
    }

    public static void stopWebcam()
    {
        webcam.close();
    }

    public static void captureImage() {
        if (webcam != null && webcam.isOpen()) {
            BufferedImage image = webcam.getImage();
            String filename = "Hello.png";
            File file = new File(filename);
            try
            {
                ImageIO.write(image, "PNG", file);
                System.out.println("Image captured: " + file.getAbsolutePath());
            }
            catch (IOException e)
            {
                System.err.println("Failed to save image: " + e.getMessage());
            }
        }
    }
}

