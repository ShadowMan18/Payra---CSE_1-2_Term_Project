package codes;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class NotificationPage {
    public NotificationPage(){}

    public void startNotificationPageView(Client client, Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("NotificationPage.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), Screen.SCREENWIDTH, Screen.SCREENHEIGHT);
        NotificationPageController notificationPageController = fxmlLoader.getController();
        notificationPageController.setNotificationPageController(client, stage);
        Image icon = new Image(String.valueOf(getClass().getResource("/images/Payra.png")));
        stage.getIcons().add(icon);
        stage.setTitle("Notification");
        stage.setScene(scene);
        stage.show();
    }

    public void stopNotificationPageView(){}
}
