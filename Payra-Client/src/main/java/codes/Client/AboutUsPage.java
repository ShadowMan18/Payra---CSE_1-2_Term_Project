package codes.Client;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;

public class AboutUsPage {
    public void startAboutUsPageView(Client client, Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/codes/AboutUsPage.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), Screen.SCREENWIDTH, Screen.SCREENHEIGHT);
        AboutUsPageController aboutUsPageController = fxmlLoader.getController();
        aboutUsPageController.setAboutUsPageController(client, stage);
        Image icon = new Image(String.valueOf(getClass().getResource("/images/Payra.png")));
        stage.getIcons().add(icon);
        stage.setTitle("AboutUs");
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


