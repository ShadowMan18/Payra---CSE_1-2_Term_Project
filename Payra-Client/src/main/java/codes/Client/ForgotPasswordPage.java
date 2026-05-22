package codes.Client;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class ForgotPasswordPage {
    public ForgotPasswordPage(){}

    public void startForgotPasswordPageView(Client client, Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/codes/ForgotPasswordPage.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), Screen.SCREENWIDTH, Screen.SCREENHEIGHT);
        ForgotPasswordPageController forgotPasswordPageController = fxmlLoader.getController();
        forgotPasswordPageController.setForgotPasswordPageController(client, stage);
        Image icon = new Image(String.valueOf(getClass().getResource("/images/Payra.png")));
        stage.getIcons().add(icon);
        stage.setTitle("Login");
        stage.setScene(scene);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), scene.getRoot());
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        forgotPasswordPageController.RecoveryQuestionLabel.setText("\uD83C\uDD60 " + client.getRecoveryQuestion());

        stage.setOnCloseRequest(event ->{
            Platform.exit();
            System.exit(0);
        });

        stage.show();
    }

    public void stopForgotPasswordPageView(Client client){
    }

}
