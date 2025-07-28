package codes;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;

public class ProfilePicturePageController {
    @FXML
    public StackPane ProfilePicturePageLayout;
    @FXML
    public Group ProfilePicturePageView;
    @FXML
    public ImageView ProfilePictureView;
    @FXML
    public Label UploadImageLabel;

    private Client client;
    private Stage stage;
    private String filePath;
    private CountDownLatch latch;


    public void setProfilePicturePageController(Client client, Stage stage) {
        this.client = client;
        this.stage = stage;
        ProfilePicturePageLayout.setPrefWidth(Screen.SCREENWIDTH);
        ProfilePicturePageLayout.setPrefHeight(Screen.SCREENHEIGHT);
        ProfilePicturePageView.scaleXProperty().bind(ProfilePicturePageLayout.widthProperty().divide(1600));
        ProfilePicturePageView.scaleYProperty().bind(ProfilePicturePageLayout.heightProperty().divide(900));
    }

    @FXML
    public void onNewsFeedButtonClick(ActionEvent actionEvent) {
        // Loading news feed page
        try {
            client.getNewsFeed().startNewsFeedView(client, stage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void onChatButtonClick(ActionEvent actionEvent) throws IOException {
        // Loading inbox page

        client.getInbox().startInboxView(client, stage);
    }

    @FXML
    public void onSubmitButtonClick(ActionEvent actionEvent) {
        if (filePath == null) {
            UploadImageLabel.setText("No image selected");
            return;
        }

        File image = new File(filePath);
        byte[] imageBytes;

        try {
            imageBytes = Files.readAllBytes(image.toPath());
            latch = new CountDownLatch(1);
            client.setLatch(latch);

            client.getServerOutput().writeObject(imageBytes);
            client.getServerOutput().flush();

            latch.await();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        filePath = null;
        UploadImageLabel.setText("");

        // Loading login page

        try {
            client.getLoginPage().startLoginPageView(client, stage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void onUploadButtonClick(ActionEvent actionEvent) {
        filePath = FileExplorer.openFileExplorer(stage);
        if (!(filePath.toLowerCase().endsWith(".png")  || filePath.toLowerCase().endsWith(".jpg")  || filePath.toLowerCase().endsWith(".jpeg") || filePath.toLowerCase().endsWith(".bmp"))) {
            UploadImageLabel.setText("Select a valid image file (png/jpg/jpeg/bmp)");
            return;
        }

        UploadImageLabel.setText("");
        File image = new File(filePath);
        ProfilePictureView.setImage(new Image(image.toURI().toString()));
        Circle clip = new Circle(175, 175, 175);
        ProfilePictureView.setClip(clip);
        System.out.println("Profile picture selected: " + filePath);
    }

    @FXML
    public void onSkipButtonClick(ActionEvent actionEvent) {
        // Loading login page

        try {
            client.getLoginPage().startLoginPageView(client, stage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void onBackButtonClick(ActionEvent actionEvent) {
    }
}
