package codes;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class NewsFeedController {
    @FXML
    public StackPane NewsFeedLayout;
    @FXML
    public Group NewsFeedView;
    
    Client client;
    Stage stage;

    public void setNewsFeedController(Client client, Stage stage) {
        this.client = client;
        this.stage = stage;
        NewsFeedLayout.setPrefWidth(Screen.SCREENWIDTH);
        NewsFeedLayout.setPrefHeight(Screen.SCREENHEIGHT);
        NewsFeedView.scaleXProperty().bind(NewsFeedLayout.widthProperty().divide(1600));
        NewsFeedView.scaleYProperty().bind(NewsFeedLayout.heightProperty().divide(900));
    }

    @FXML
    public void onChatButtonClicked(ActionEvent mouseEvent) throws IOException {
        client.getInbox().startInboxView(client, stage);
    }

    @FXML
    public void onHomeButtonClicked(ActionEvent mouseEvent) throws IOException {
        client.getHomePage().startHomePageView(client, stage);
    }

    @FXML
    public void onNotificationButtonClick(ActionEvent actionEvent) throws IOException {
        client.getNotificationPage().startNotificationPageView(client, stage);
    }

    public void onProfileButtonClick(ActionEvent actionEvent) throws IOException {
        client.getProfilePage().startProfilePageView(client, stage);
    }
    @FXML
    public void onPostButtonClick(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/codes/PopUp.fxml"));
            Parent root = loader.load();

            PostController controller = loader.getController();
            controller.setClient(client);

            Stage popupStage = new Stage();
            popupStage.setTitle("New Post");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
