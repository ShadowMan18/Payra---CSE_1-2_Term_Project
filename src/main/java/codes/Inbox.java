package codes;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class Inbox {
    public Inbox(){}

    public void startInboxView(Client client, Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Inbox.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), Screen.SCREENWIDTH, Screen.SCREENHEIGHT);
        InboxController inboxController = fxmlLoader.getController();
        inboxController.setInboxController(client, stage);
        Image icon = new Image(String.valueOf(getClass().getResource("/images/Payra.png")));
        stage.getIcons().add(icon);
        stage.setTitle("Inbox");
        stage.setScene(scene);

        stage.setOnCloseRequest(event ->{
            Platform.exit();
            System.exit(0);
        });

        stage.show();
    }

    public void stopInboxView(){}
}


