package codes;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginPageController {
    @FXML
    private StackPane LoginPageLayout;
    @FXML
    private Group LoginPageView;
    @FXML
    private TextField LoginPageEmailField;
    @FXML
    private TextField LoginPagePasswordField;

    private Client client;
    private Stage stage;
    private String email;
    private String password;

    public void setLoginPageController(Client client, Stage stage) {
        this.client = client;
        this.stage = stage;
        LoginPageLayout.setPrefWidth(Screen.SCREENWIDTH);
        LoginPageLayout.setPrefHeight(Screen.SCREENHEIGHT);
        LoginPageView.scaleXProperty().bind(LoginPageLayout.widthProperty().divide(1600));
        LoginPageView.scaleYProperty().bind(LoginPageLayout.heightProperty().divide(900));
        LoginPageLayout.setFocusTraversable(true);
        LoginPageLayout.requestFocus();
    }

    @FXML
    public void onLoginButtonClick(ActionEvent actionEvent) {
        login();

        try {
            client.getHomePage().startHomePageView(client, stage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void onCreateButtonClick(ActionEvent actionEvent) throws IOException {
        client.getSignupPage().startSignupPageView(client, stage);
    }

    @FXML
    public void onForgotPasswordButtonClick(ActionEvent mouseEvent) {
    }

    public void onEnterKeyPress(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            login();
            LoginPageLayout.setFocusTraversable(false);

            try {
                client.getHomePage().startHomePageView(client, stage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void login() {
        email = LoginPageEmailField.getText();
        password = LoginPagePasswordField.getText();

        System.out.println(email);
        System.out.println(password);

        try {
            client.getServerOutput().writeObject("2_" + email);
            client.getServerOutput().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

