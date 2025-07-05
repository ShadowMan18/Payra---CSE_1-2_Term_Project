package codes;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SignupPageController {
    @FXML
    private StackPane SignupPageLayout;
    @FXML
    private Group SignupPageView;
    @FXML
    private TextField SignupPageFirstNameField;
    @FXML
    private TextField SignupPageLastNameField;
    @FXML
    private TextField SignupPageEmailField;
    @FXML
    private TextField SignupPageSetPasswordField;
    @FXML
    private TextField SignupPageConfirmPasswordField;
    @FXML
    private TextField SignupPageQuestionField;
    @FXML
    private TextField SignupPageAnswerField;

    private Client client;
    private Stage stage;

    private String firstName;
    private String lastName;
    private String email;
    private String password1;
    private String password2;
    private String question;
    private String answer;


    public void setSignupPageController(Client client, Stage stage) {
        this.client = client;
        this.stage = stage;
        SignupPageLayout.setPrefWidth(Screen.SCREENWIDTH);
        SignupPageLayout.setPrefHeight(Screen.SCREENHEIGHT);
        SignupPageView.scaleXProperty().bind(SignupPageLayout.widthProperty().divide(1600));
        SignupPageView.scaleYProperty().bind(SignupPageLayout.heightProperty().divide(900));
        SignupPageLayout.setFocusTraversable(true);
        SignupPageLayout.requestFocus();
    }

    @FXML
    public void onSignupButtonClick(ActionEvent mouseEvent) {
        signup();

        try {
            client.getLoginPage().startLoginPageView(client, stage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void onEnterKeyPress(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            signup();
            SignupPageLayout.setFocusTraversable(false);

            try {
                client.getLoginPage().startLoginPageView(client, stage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void signup() {
        firstName = SignupPageFirstNameField.getText();
        lastName = SignupPageLastNameField.getText();
        email = SignupPageEmailField.getText();
        password1 = SignupPageSetPasswordField.getText();
        password2 = SignupPageConfirmPasswordField.getText();
        question = SignupPageQuestionField.getText();
        answer = SignupPageAnswerField.getText();

        System.out.println(firstName);
        System.out.println(lastName);
        System.out.println(email);
        System.out.println(password1);
        System.out.println(password2);
        System.out.println(question);
        System.out.println(answer);

        client.setFirstName(firstName);
        client.setLastName(lastName);
        client.setEmail(email);
        client.setId(email.substring(0, email.length() - "@gmail.com".length()));
        client.setPassword(password1);

        try {
            client.getServerOutput().writeObject("signup:" + client + "," + question + "," + answer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
