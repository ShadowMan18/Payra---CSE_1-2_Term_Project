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
        // Proceeding to sign up
        signup();

        // Loading login page

        try {
            client.getLoginPage().startLoginPageView(client, stage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void onEnterKeyPress(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            // Proceeding to sign up

            boolean isSignedUp = signup();

            SignupPageLayout.setFocusTraversable(false);

            // Loading login page

            if (isSignedUp) {
                try {
                    client.getLoginPage().startLoginPageView(client, stage);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public boolean signup() {
        // Taking inputs from the input fields

        firstName = SignupPageFirstNameField.getText();
        lastName = SignupPageLastNameField.getText();
        email = SignupPageEmailField.getText();
        password1 = SignupPageSetPasswordField.getText();
        password2 = SignupPageConfirmPasswordField.getText();
        question = SignupPageQuestionField.getText();
        answer = SignupPageAnswerField.getText();

        boolean firstNameCheck = checkName(firstName);
        boolean lastNameCheck = checkName(lastName);
        boolean emailCheck = checkEmailAddress(email);
        boolean password1Check = true;
        boolean password2Check = true;
        boolean questionCheck = true;
        boolean answerCheck = true;

        System.out.println(firstName);
        System.out.println(lastName);
        System.out.println(email);
        System.out.println(password1);
        System.out.println(password2);
        System.out.println(question);
        System.out.println(answer);

        // Setting client information to the client object

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

        return true;

//        if (firstNameCheck && lastNameCheck && emailCheck && password1Check && password2Check && questionCheck && answerCheck) {
//            // Sending sign up command with client's information to the server
//
//            try {
//                client.getServerOutput().writeObject("signup:" + client + "," + question + "," + answer);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//
//            return true;
//        }
//        else {
//            return false;
//        }
    }

    public boolean checkName(String name) {
        for (int i = 0; i < name.length(); i++) {
            if (!((name.charAt(i) >= 'A' && name.charAt(i) <= 'Z') || (name.charAt(i) >= 'a' && name.charAt(i) <= 'z') || name.charAt(i) == ' ' || name.charAt(i) == '.' || name.charAt(i) == '-')) {
                return false;
            }
        }
        return true;
    }

    public boolean checkEmailAddress(String emailAddress) {
        if (!emailAddress.endsWith("@gmail.com")) {
            return false;
        }
        else {
            String id = emailAddress.substring(emailAddress.length() - "@gmail.com".length());

            if (id.length() < 6) {
                return false;
            }
            else if (id.length() > 30) {
                return false;
            }
            else if (!(id.charAt(0) >= 'a' && id.charAt(0) <= 'z')) {
                return false;
            }
            else if (id.endsWith(".")) {
                return false;
            }
            else {
                for(int i = 0; i < id.length(); i++) {
                    if (!((id.charAt(i) >= 'a' && id.charAt(i) <= 'z') || (id.charAt(i) >= '0' && id.charAt(i) <= '9') || id.charAt(i) == '.' || id.charAt(i) == '-' || id.charAt(i) == '_')) {
                        return false;
                    }
                }
            }

            try {
                client.getServerOutput().writeObject("check:" + id);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                if (client.getServerInput().readBoolean()) {
                    return false;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

//    public boolean passwordCheck(String password) {
//        int upperCase = 0;
//        int lowerCase = 0;
//        int specialCharacter = 0;
//        int digit = 0;
//
//        if (password.length() < 8) {
//            return false;
//        }
//        else {
//            for (int i = 0; i < password.length(); i++){
//                if (password.charAt(i) >= 'A' && password.charAt(i) <= 'Z') {
//                    upperCase++;
//                }
//                else if (password.charAt(i) >= 'a' && password.charAt(i) <= 'z') {
//                    lowerCase++;
//                }
//                else if (password.charAt(i) == )
//            }
//        }
//    }
}
