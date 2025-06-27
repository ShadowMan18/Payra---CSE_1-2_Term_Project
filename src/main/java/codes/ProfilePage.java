package codes;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ProfilePage {
    private final int screenWidth = Screen.getWidth() - 5;
    private final int screenHeight = Screen.getHeight() - 35;

    public ProfilePage(){}

    public void startProfilePageView(Client client, Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ProfilePage.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), screenWidth, screenHeight);
        ProfilePageController profilePageController = fxmlLoader.getController();
        profilePageController.setProfilePageController(client, stage);
        stage.setTitle("Profile");
        stage.setScene(scene);
        stage.show();
    }

    public void stopProfilePageView(){}
}
