package codes;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import javafx.scene.control.TextArea;


public class PostController {

    @FXML
    private TextArea postContent;

    private Client client;

    public void setClient(Client client) {
        this.client = client;
    }

    //files
    private String attachedFilePath;
    private String attachedFileName;
    private byte[] attachedFileBytes;
    private Stage stage;


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
        stage = (Stage) postContent.getScene().getWindow();  // Needed for FileChooser dialog
        attachedFilePath = FileExplorer.openFileExplorer(stage);

        if (attachedFilePath != null) {
            System.out.println("File selected: " + attachedFilePath);
            File file = new File(attachedFilePath);
            attachedFileName = file.getName();

            try {
                attachedFileBytes = Files.readAllBytes(file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }

            postContent.setText(postContent.getText() + "\n[Attached: " + attachedFileName + "]");
            postContent.setEditable(true);
            Platform.runLater(() -> postContent.requestFocus());
        }
    }
}

