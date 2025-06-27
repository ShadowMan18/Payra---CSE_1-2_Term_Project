package codes;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HomePage {
    private final int screenWidth = Screen.getWidth() - 5;
    private final int screenHeight = Screen.getHeight() - 35;

    public HomePage(){}

    public void startHomePageView(Client client, Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("HomePage.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), screenWidth, screenHeight);
        HomePageController homePageController = fxmlLoader.getController();
        homePageController.setHomePageController(client, stage);
        stage.setTitle("Home");
        stage.setScene(scene);
        stage.show();
    }

    public void stopHomePageView(){}
}


