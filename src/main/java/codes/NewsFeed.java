package codes;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class NewsFeed {
    private final int screenWidth = Screen.getWidth() - 5;
    private final int screenHeight = Screen.getHeight() - 35;

    public NewsFeed(){}

    public void startNewsFeedView(Client client, Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("NewsFeed.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), screenWidth, screenHeight);
        NewsFeedController newsFeedController = fxmlLoader.getController();
        newsFeedController.setNewsFeedController(client, stage);
        stage.setTitle("Newsfeed");
        stage.setScene(scene);
        stage.show();
    }

    public void stopNewsFeedView(){}
}


