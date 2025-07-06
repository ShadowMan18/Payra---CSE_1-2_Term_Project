package codes;

import javafx.event.ActionEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class ProfilePageController {
    Client client;
    Stage stage;

    public void setProfilePageController(Client client, Stage stage) {
        this.client = client;
        this.stage = stage;
    }

    public void onHomeButtonClick(ActionEvent event) {
        try {
            client.getHomePage().startHomePageView(client, stage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
