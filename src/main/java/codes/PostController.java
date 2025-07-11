package codes;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.*;
import java.util.HashSet;
import java.util.Set;


public class PostController {

    @FXML
    private TextArea postContent;

    private Client client;

    public void setClient(Client client) {
        this.client = client;
    }

    @FXML
    public void onPostClick() {
        String content = postContent.getText().trim();
        if (content.isEmpty() || client == null || client.getEmail() == null) return;

        client.sendPostToFeed(content);


        Stage stage = (Stage) postContent.getScene().getWindow();
        if (stage != null) stage.close();
    }
}
