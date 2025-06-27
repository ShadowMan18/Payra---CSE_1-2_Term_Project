package codes;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.Stage;

import java.io.IOException;

public class NewsFeedController {
    Client client;
    Stage stage;

    public void setNewsFeedController(Client client, Stage stage) {
        this.client = client;
        this.stage = stage;
    }

    @FXML
    public void onChatButtonClicked(ActionEvent mouseEvent) throws IOException {
        client.getInbox().startInboxView(client, stage);
    }

    @FXML
    public void onHomeButtonClicked(ActionEvent mouseEvent) throws IOException {
        client.getHomePage().startHomePageView(client, stage);
    }

    @FXML
    public void onNotificationButtonClick(ActionEvent actionEvent) throws IOException {
        client.getNotificationPage().startNotificationPageView(client, stage);
    }
}
