package codes;

import com.almasb.fxgl.io.FileExtension;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.util.concurrent.CountDownLatch;

public class IntroPage {
    public IntroPage(){}

    public void startIntroPageView(Client client, Stage stage) throws IOException {
        Font.loadFont(getClass().getResourceAsStream("/fonts/Hello Paris Sans Regular.ttf"), 12);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Glacial Indifference Bold.otf"), 12);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Glacial Indifference Regular.otf"), 12);
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("IntroPage.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), Screen.SCREENWIDTH, Screen.SCREENHEIGHT);
        IntroPageController introPageController = fxmlLoader.getController();
        introPageController.setIntroPageController(client, stage);
        Image icon = new Image(String.valueOf(getClass().getResource("/images/Payra.png")));
        stage.getIcons().add(icon);
        stage.setTitle("Intro");
        stage.setScene(scene);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(1500), scene.getRoot());
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        stage.setOnCloseRequest(event ->{
            Platform.exit();
            System.exit(0);
        });

        stage.show();
    }
}


