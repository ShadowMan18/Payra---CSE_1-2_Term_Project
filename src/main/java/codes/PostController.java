package codes;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javafx.event.ActionEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.TextArea;


public class PostController {

    @FXML
    private AnchorPane postContainer;

    @FXML
    private TextArea postContent;

    @FXML
    private ImageView previewImageView;

    @FXML
    private Button attachButton;

    private Client client;
    private Stage stage;

    private String attachedFilePath;
    private String attachedFileName;
    private byte[] attachedFileBytes;

    public void setClient(Client client) {
        this.client = client;
    }


    @FXML
    public void onPostClick() {
        String content = postContent.getText().trim();
        if ((content.isEmpty() && attachedFileBytes == null) || client == null || client.getId() == null) return;

        client.sendPostToFeed(content, attachedFileName, attachedFileBytes);

        Stage stage = (Stage) postContent.getScene().getWindow();
        if (stage != null) stage.close();
    }


    @FXML
    public void onAttachButtonClicked(ActionEvent event) {
        stage = (Stage) postContent.getScene().getWindow();
        attachedFilePath = FileExplorer.openFileExplorer(stage);

        if (attachedFilePath != null) {
            File file = new File(attachedFilePath);
            attachedFileName = file.getName();

            String lowerName = attachedFileName.toLowerCase();
            if (!(lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                    lowerName.endsWith(".png") || lowerName.endsWith(".gif"))) {
                showError("Only image files (.jpg, .jpeg, .png, .gif) are allowed.");
                attachedFilePath = null;
                attachedFileName = null;
                attachedFileBytes = null;
                return;
            }

            try {
                attachedFileBytes = Files.readAllBytes(file.toPath());

                Image image = new Image(file.toURI().toString());
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(400);
                imageView.setPreserveRatio(true);

                StackPane root = new StackPane(imageView);
                root.setStyle("-fx-background-color: white; -fx-padding: 10;");

                Stage previewStage = new Stage();
                previewStage.setTitle("Image Preview");
                previewStage.setScene(new Scene(root));
                previewStage.initOwner(stage);
                previewStage.show();

            } catch (IOException e) {
                e.printStackTrace();
                showError("Failed to read the selected image.");
            }
        }
    }

    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Invalid File");
        alert.setHeaderText("Attachment Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

}



