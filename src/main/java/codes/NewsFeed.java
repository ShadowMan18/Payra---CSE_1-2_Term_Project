package codes;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class NewsFeed {
    public NewsFeed(){}

    public void startNewsFeedView(Client client, Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("NewsFeed.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), Screen.SCREENWIDTH, Screen.SCREENHEIGHT);
        NewsFeedController newsFeedController = fxmlLoader.getController();
        newsFeedController.setNewsFeedController(client, stage);

        client.setNewsFeedController(newsFeedController);

        Image icon = new Image(String.valueOf(getClass().getResource("/images/Payra.png")));
        stage.getIcons().add(icon);
        stage.setTitle("Newsfeed");
        stage.setScene(scene);

        stage.setOnCloseRequest(event ->{
            Platform.exit();
            System.exit(0);
        });

        stage.show();
    }

    public void stopNewsFeedView(){}
}


