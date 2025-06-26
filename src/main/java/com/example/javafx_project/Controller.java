package com.example.javafx_project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class Controller {
    @FXML
    private Label welcomeText;

    @FXML
    public void onGoButtonClick(ActionEvent actionEvent) {
        HomePage.viewHome();
    }

    @FXML
    protected void onStartButtonClick() {
        WebcamCapture.startWebcam(Main.stage);
        MicrophoneCapture.startMicrophone();
    }

    @FXML
    public void onStopButtonClick(ActionEvent actionEvent) {
        WebcamCapture.stopWebcam();
        MicrophoneCapture.stopMicrophone();
        HomePage.viewHome();
    }

    public void onTakeAShotButtonClick(ActionEvent actionEvent) {
        WebcamCapture.captureImage();
    }
}