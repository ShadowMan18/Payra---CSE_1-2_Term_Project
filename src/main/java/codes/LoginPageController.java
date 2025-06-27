package codes;

import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginPageController {
    Client client;
    Stage stage;

    public void setLoginPageController(Client client, Stage stage) {
        this.client = client;
        this.stage = stage;
    }

    @FXML
    public void onLoginButtonClick(ActionEvent actionEvent) throws IOException {
        client.getHomePage().startHomePageView(client, stage);
    }

    @FXML
    public void onCreateButtonClick(ActionEvent actionEvent) throws IOException {
        client.getSignupPage().startSignupPageView(client, stage);
    }

}

