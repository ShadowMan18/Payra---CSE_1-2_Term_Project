package codes;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.Stage;

import java.io.IOException;

public class SignupPageController {
    Client client;
    Stage stage;

    public void setSignupPageController(Client client, Stage stage) {
        this.client = client;
        this.stage = stage;
    }

    @FXML
    public void onSignupButtonClick(ActionEvent mouseEvent) throws IOException {
        client.getLoginPage().startLoginPageView(client, stage);
    }
}
