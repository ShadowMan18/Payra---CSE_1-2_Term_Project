package codes;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class IntroPage {
    Client client;
    Stage stage;

    public IntroPage(){}

    public void startIntroPageView(Client client, Stage stage) throws IOException {
        this.client = client;
        this.stage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("IntroPage.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), Screen.SCREENWIDTH, Screen.SCREENHEIGHT);
        IntroPage introPageController = fxmlLoader.getController();
        introPageController.client = client;
        introPageController.stage = stage;
        Image icon = new Image(String.valueOf(getClass().getResource("/images/Payra.png")));
        stage.getIcons().add(icon);
        stage.setTitle("Intro");
        stage.setScene(scene);
        stage.show();
    }

    public void stopIntroPageView(){}

    public void onNextButtonClick(ActionEvent actionEvent) throws IOException {
        client.getProfilePage().startProfilePageView(client, stage);
    }
}
