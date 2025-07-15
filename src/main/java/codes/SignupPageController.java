package codes;

import javafx.application.Platform;
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
    @FXML
    private Label SignupPageFirstNameLabel;
    @FXML
    private Label SignupPageLastNameLabel;
    @FXML
    private Label SignupPageEmailLabel;
    @FXML
    private Label SignupPageSetPasswordLabel;
    @FXML
    private Label SignupPageConfirmPasswordLabel;
    @FXML
    private Label SignupPageQuestionLabel;
    @FXML
    private Label SignupPageAnswerLabel;
    @FXML
    private Label SignupConfirmationLabel;

    private Client client;
    private Stage stage;

    private String firstName;
    private String lastName;
    private String email;
    private String password1;
    private String password2;
    private String question;
    private String answer;

    private CountDownLatch latch;

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
        boolean isSignedUp = signup();

        // Loading profile picture page

        if (isSignedUp) {
            try {
                client.getProfilePicturePage().startProfilePicturePageView(client, stage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @FXML
    public void onBackButtonClick(ActionEvent actionEvent) {
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

//            SignupPageLayout.setFocusTraversable(false);

            // Loading profile picture page

            if (isSignedUp) {
                try {
                    client.getProfilePicturePage().startProfilePicturePageView(client, stage);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public boolean signup() {
        // Taking inputs from the input fields

        firstName = SignupPageFirstNameField.getText().trim();
        lastName = SignupPageLastNameField.getText().trim();
        email = SignupPageEmailField.getText().trim();
        password1 = SignupPageSetPasswordField.getText().trim();
        password2 = SignupPageConfirmPasswordField.getText().trim();
        question = SignupPageQuestionField.getText().trim();
        answer = SignupPageAnswerField.getText().trim();

        boolean firstNameCheck = checkFirstName();
        boolean lastNameCheck = checkLastName();
        boolean emailCheck = checkEmailAddress();
        boolean passwordCheck = checkPassword();
        boolean passwordConfirmation = confirmPassword();
        boolean questionCheck = true;
        boolean answerCheck = true;
        
        if (question.isEmpty()) {
            SignupPageQuestionLabel.setText("This field can't be empty");
            questionCheck = false;
        }
        else {
            SignupPageQuestionLabel.setText("");
        }

        if (answer.isEmpty()) {
            SignupPageAnswerLabel.setText("This field can't be empty");
            answerCheck = false;
        }
        else {
            SignupPageAnswerLabel.setText("");
        }

        if (firstNameCheck && lastNameCheck && emailCheck && passwordCheck && passwordConfirmation && questionCheck && answerCheck) {
            client.setFirstName(firstName);
            client.setLastName(lastName);
            client.setEmail(email);
            client.setId(email.substring(0, email.length() - "@gmail.com".length()));
            client.setPassword(password1);
            client.setRecoveryQuestion(question);
            client.setRecoveryAnswer(answer);

            try {
                latch = new CountDownLatch(1);
                client.setLatch(latch);

                client.getServerOutput().writeObject("check:" + email.substring(0, email.length() - "@gmail.com".length()));
                client.getServerOutput().flush();

                latch.await();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (client.isRegistered()) {
                SignupPageEmailLabel.setText("Email already registered");
                return false;
            }
            
            // Sending sign up command with client's information to the server

            try {
                latch = new CountDownLatch(1);
                client.setLatch(latch);

                client.getServerOutput().writeObject("signup:" + client + "," + question + "," + answer);
                client.getServerOutput().flush();

                latch.await();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (!client.isRegistered()) {
//                Platform.runLater(() -> {
//                    SignupConfirmationLabel.setStyle("-fx-text-fill: #ff0000;");
//                    SignupConfirmationLabel.setText("Couldn't sign up. Please try again.");
//                });
                System.out.println("Couldn't register");
                return false;
            }

//            Platform.runLater(() -> {
//                SignupConfirmationLabel.setStyle("-fx-text-fill: #00ff00;");
//                SignupConfirmationLabel.setText("You are signed up successfully!");
//            });

//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }

            // Setting client information to the client object

            System.out.println(firstName);
            System.out.println(lastName);
            System.out.println(email);
            System.out.println(password1);
            System.out.println(password2);
            System.out.println(question);
            System.out.println(answer);

            return true;
        }
        else {
            return false;
        }
    }

    public boolean checkFirstName() {
        if (firstName.isEmpty()) {
            SignupPageFirstNameLabel.setText("This field can't be empty");
            return false;
        }
        else {
            for (int i = 0; i < firstName.length(); i++) {
                if (!((firstName.charAt(i) >= 'A' && firstName.charAt(i) <= 'Z') || (firstName.charAt(i) >= 'a' && firstName.charAt(i) <= 'z') || firstName.charAt(i) == ' ' || firstName.charAt(i) == '.' || firstName.charAt(i) == '-')) {
                    SignupPageFirstNameLabel.setText("Invalid name");
                    return false;
                }
            }
        }

        SignupPageFirstNameLabel.setText("");
        return true;
    }

    public boolean checkLastName() {
        if (lastName.isEmpty()) {
            SignupPageLastNameLabel.setText("This field can't be empty");
            return false;
        }
        else {
            for (int i = 0; i < lastName.length(); i++) {
                if (!((lastName.charAt(i) >= 'A' && lastName.charAt(i) <= 'Z') || (lastName.charAt(i) >= 'a' && lastName.charAt(i) <= 'z') || lastName.charAt(i) == ' ' || lastName.charAt(i) == '.' || lastName.charAt(i) == '-')) {
                    SignupPageLastNameLabel.setText("Invalid name");
                    return false;
                }
            }
        }

        SignupPageLastNameLabel.setText("");
        return true;
    }

    public boolean checkEmailAddress() {
        if (email.isEmpty()) {
            SignupPageEmailLabel.setText("This field can't be empty");
            return false;
        }
        else if (!(email.length() > 10 && email.endsWith("@gmail.com"))) {
            SignupPageEmailLabel.setText("Invalid email address");
            return false;
        }
        else {
            String id = email.substring(0, email.length() - "@gmail.com".length());

            if (id.length() < 6) {
                SignupPageEmailLabel.setText("Username is too short");
                return false;
            }
            else if (id.length() > 30) {
                SignupPageEmailLabel.setText("Username is too long");
                return false;
            }
            else if (!(id.charAt(0) >= 'a' && id.charAt(0) <= 'z')) {
                SignupPageEmailLabel.setText("Email address must start with a letter");
                return false;
            }
            else {
                for(int i = 0; i < id.length(); i++) {
                    if (!((id.charAt(i) >= 'a' && id.charAt(i) <= 'z') || (id.charAt(i) >= '0' && id.charAt(i) <= '9') || id.charAt(i) == '.' || id.charAt(i) == '-' || id.charAt(i) == '_')) {
                        SignupPageEmailLabel.setText("Characters must be a~z, 0~9, . _ -");
                        return false;
                    }
                }
            }
        }

        SignupPageEmailLabel.setText("");
        return true;
    }

    public boolean checkPassword() {
        int upperCase = 0;
        int lowerCase = 0;
        int specialCharacter = 0;
        int digit = 0;
        String specialCharacters = "!@#$%^&*_+-=(){}[]<>|\\/,.?:;\"'";

        if (password1.isEmpty()) {
            SignupPageSetPasswordLabel.setText("This field can't be empty");
            return false;
        }
        else if (password1.length() < 8) {
            SignupPageSetPasswordLabel.setText("Password must contain at least 8 characters");
            return false;
        }
        else {
            for (int i = 0; i < password1.length(); i++){
                if (password1.charAt(i) >= 'A' && password1.charAt(i) <= 'Z') {
                    upperCase++;
                }
                else if (password1.charAt(i) >= 'a' && password1.charAt(i) <= 'z') {
                    lowerCase++;
                }
                else if (specialCharacters.contains(String.valueOf(password1.charAt(i)))) {
                    specialCharacter++;
                }
                else if (password1.charAt(i) >= '0' && password1.charAt(i) <= '9') {
                    digit++;
                }
                else {
                    SignupPageSetPasswordLabel.setText("Characters must be A~Z, a~z, 0~9, !@#$%^&*_+-=(){}[]<>|\\/,.?:;\"'");
                    return false;
                }
            }

            if (upperCase == 0 || lowerCase == 0 || specialCharacter == 0 || digit ==0) {
                SignupPageSetPasswordLabel.setText("Password must contain uppercase, lowercase, special character and digit");
                return false;
            }
        }

        SignupPageSetPasswordLabel.setText("");
        return true;
    }
    
    public boolean confirmPassword() {
        if (password2.isEmpty()) {
            SignupPageConfirmPasswordLabel.setText("This field can't be empty");
            return false;
        }
        else if (!password2.equals(password1)) {
            SignupPageConfirmPasswordLabel.setText("Password doesn't match");
            return false;
        }

        SignupPageConfirmPasswordLabel.setText("");
        return true;
    }
}