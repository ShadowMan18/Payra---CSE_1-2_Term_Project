package codes;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class ProfilePage {
    public ProfilePage(){}

    public void startProfilePageView(Client client, Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ProfilePage.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), Screen.SCREENWIDTH, Screen.SCREENHEIGHT);
        ProfilePageController profilePageController = fxmlLoader.getController();
        profilePageController.setProfilePageController(client, stage);
        Image icon = new Image(String.valueOf(getClass().getResource("/images/Payra.png")));
        stage.getIcons().add(icon);
        stage.setTitle("Profile");
        stage.setScene(scene);
        stage.show();
    }

    public void stopProfilePageView(){}
}
