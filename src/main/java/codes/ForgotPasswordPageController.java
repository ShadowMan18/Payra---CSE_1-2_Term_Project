package codes;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class ForgotPasswordPageController {
    @FXML
    public StackPane ForgotPasswordPageLayout;
    @FXML
    public Group ForgotPasswordPageView;
    @FXML
    public TextField RecoveryAnswerField;
    @FXML
    public TextField SetNewPasswordField;
    @FXML
    public TextField ConfirmNewPasswordField;
    @FXML
    public Label ConfirmNewPasswordLabel;
    @FXML
    public Label SetNewPasswordLabel;
    @FXML
    public Label RecoveryAnswerLabel;
    @FXML
    public Label RecoveryQuestionLabel;

    Client client;
    Stage stage;
    
    private String password1;
    private String password2;

    private CountDownLatch latch;

    public void setForgotPasswordPageController(Client client, Stage stage) {
        this.client = client;
        this.stage = stage;
        ForgotPasswordPageLayout.setPrefWidth(Screen.SCREENWIDTH);
        ForgotPasswordPageLayout.setPrefHeight(Screen.SCREENHEIGHT);
        ForgotPasswordPageLayout.scaleXProperty().bind(ForgotPasswordPageLayout.widthProperty().divide(1600));
        ForgotPasswordPageLayout.scaleYProperty().bind(ForgotPasswordPageLayout.heightProperty().divide(900));
    }

    @FXML
    public void onSubmitButtonClicked(ActionEvent actionEvent) {
        String answer = RecoveryAnswerField.getText().trim();
        password1 = SetNewPasswordField.getText().trim();
        password2 = ConfirmNewPasswordField.getText().trim();

        boolean passwordCheck = checkPassword();
        boolean passwordConfirmation = confirmPassword();

        if (answer.isEmpty()) {
            RecoveryAnswerLabel.setText("This field can't be empty");
            return;
        }
        else {
            RecoveryAnswerLabel.setText("");
        }

        if (passwordCheck && passwordConfirmation) {
            if (!answer.equals(client.getRecoveryAnswer())) {
                RecoveryAnswerLabel.setText("Wrong answer");
                return;
            }

            RecoveryAnswerLabel.setText("");

            try {
                latch = new CountDownLatch(1);
                client.setLatch(latch);

                System.out.println(password1);

                client.getServerOutput().writeObject("update:password," + password1);
                client.getServerOutput().flush();

                latch.await();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }

            // Loading login page

            try {
                client.getLoginPage().startLoginPageView(client, stage);
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

    public boolean checkPassword() {
        int upperCase = 0;
        int lowerCase = 0;
        int specialCharacter = 0;
        int digit = 0;
        String specialCharacters = "!@#$%^&*_+-=(){}[]<>|\\/,.?:;\"'";

        if (password1.isEmpty()) {
            SetNewPasswordLabel.setText("This field can't be empty");
            return false;
        }
        else if (password1.length() < 8) {
            SetNewPasswordLabel.setText("Password must contain at least 8 characters");
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
                    SetNewPasswordLabel.setText("Characters must be A~Z, a~z, 0~9, !@#$%^&*_+-=(){}[]<>|\\/,.?:;\"'");
                    return false;
                }
            }

            if (upperCase == 0 || lowerCase == 0 || specialCharacter == 0 || digit ==0) {
                SetNewPasswordLabel.setText("Password must contain uppercase, lowercase, special character and digit");
                return false;
            }
        }

        SetNewPasswordLabel.setText("");
        return true;
    }

    public boolean confirmPassword() {
        if (password2.isEmpty()) {
            ConfirmNewPasswordLabel.setText("This field can't be empty");
            return false;
        }
        else if (!password2.equals(password1)) {
            ConfirmNewPasswordLabel.setText("Password doesn't match");
            return false;
        }

        ConfirmNewPasswordLabel.setText("");
        return true;
    }
}
