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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

                File image = new File("src/main/resources/images/DefaultProfilePicture.png");
                byte[] imageBytes;
                imageBytes = Files.readAllBytes(image.toPath());

                client.getServerOutput().writeObject(new ClientInfo(firstName, lastName, email.substring(0, email.length() - "@gmail.com".length()), password1, question, answer, imageBytes));
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
            String verdict = Validator.validateName(firstName);
            SignupPageFirstNameLabel.setText(verdict);
            if (verdict.isEmpty()) {
                return true;
            }
            else {
                return false;
            }
        }
    }

    public boolean checkLastName() {
        if (lastName.isEmpty()) {
            SignupPageLastNameLabel.setText("This field can't be empty");
            return false;
        }
        else {
            String verdict = Validator.validateName(lastName);
            SignupPageLastNameLabel.setText(verdict);
            if (verdict.isEmpty()) {
                return true;
            }
            else {
                return false;
            }
        }
    }

    public boolean checkEmailAddress() {
        if (email.isEmpty()) {
            SignupPageEmailLabel.setText("This field can't be empty");
            return false;
        }
        else {
            String verdict = Validator.validateEmail(email);
            SignupPageEmailLabel.setText(verdict);
            if (verdict.isEmpty()) {
                return true;
            }
            else {
                return false;
            }
        }
    }

    public boolean checkPassword() {
        if (password1.isEmpty()) {
            SignupPageSetPasswordLabel.setText("This field can't be empty");
            return false;
        }
        else {
            String verdict = Validator.validatePassword(password1);
            SignupPageSetPasswordLabel.setText(verdict);
            if (verdict.isEmpty()) {
                return true;
            }
            else {
                return false;
            }
        }
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