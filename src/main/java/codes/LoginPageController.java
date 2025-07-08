package codes;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.zip.CheckedOutputStream;

public class LoginPageController {
    @FXML
    private StackPane LoginPageLayout;
    @FXML
    private Group LoginPageView;
    @FXML
    private TextField LoginPageEmailField;
    @FXML
    private TextField LoginPagePasswordField;
    @FXML
    private Label LoginPageEmailLabel;
    @FXML
    private Label LoginPagePasswordLabel;

    private Client client;
    private Stage stage;
    private String email;
    private String password;
    
    private CountDownLatch latch;

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
        // Proceeding to log in

        boolean isLoggedIn = login();

        // Loading home page

        if (isLoggedIn) {
            try {
                client.getHomePage().startHomePageView(client, stage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @FXML
    public void onCreateButtonClick(ActionEvent actionEvent) throws IOException {
        // Loading sign up page

        client.getSignupPage().startSignupPageView(client, stage);
    }

    @FXML
    public void onForgotPasswordButtonClick(ActionEvent mouseEvent) {
        // Laoding forgot password page
    }

    public void onEnterKeyPress(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            // Proceeding to log in
            
            boolean isLoggedIn = login();

            LoginPageLayout.setFocusTraversable(false);

            // Loading home page

            if (isLoggedIn) {
                try {
                    client.getHomePage().startHomePageView(client, stage);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public boolean login() {
        // Taking inputs from the input fields

        email = LoginPageEmailField.getText();
        password = LoginPagePasswordField.getText();

        System.out.println(email);
        System.out.println(password);

        checkEmailAddress();
        boolean check = checkPassword();

        // Sending log in command with client's information to the server

        if (check) {
            try {
                client.getServerOutput().writeObject("login:" + client.getId());
                client.getServerOutput().flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return true;
        }
        else {
            return false;
        }
    }

    public boolean checkEmailAddress() {
        if (email.isEmpty()) {
            LoginPageEmailLabel.setText("This field can't be empty");
            return false;
        }
        else if (!(email.length() > 10 && email.endsWith("@gmail.com"))) {
            LoginPageEmailLabel.setText("Invalid email address");
            return false;
        }
        else {
            String id = email.substring(0, email.length() - "@gmail.com".length());

            try {
                latch = new CountDownLatch(1);
                client.setLatch(latch);

                client.getServerOutput().writeObject("check:" + id);
                client.getServerOutput().flush();

                latch.await();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (!client.isRegistered()) {
                LoginPageEmailLabel.setText("No account found");
                return false;
            }
        }

        LoginPageEmailLabel.setText("");
        return true;
    }

    public boolean checkPassword() {
        if (password.isEmpty()) {
            LoginPagePasswordLabel.setText("This field can't be empty");
            return false;
        }
        else {
            LoginPagePasswordLabel.setText("");
            if (checkEmailAddress()) {
                try {
                    latch = new CountDownLatch(1);
                    client.setLatch(latch);

                    client.getServerOutput().writeObject("get_info:" + email.substring(0, email.length() - "@gmail.com".length()));
                    client.getServerOutput().flush();

                    latch.await();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

                if (!password.equals(client.getPassword())) {
                    LoginPagePasswordLabel.setText("Wrong password");
                    return false;
                }
            }
            else {
                return false;
            }
        }

        LoginPagePasswordLabel.setText("");
        return true;
    }
}

