package codes;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class NewsFeedController {
    @FXML
    public StackPane NewsFeedLayout;
    @FXML
    public Group NewsFeedView;
    @FXML
    public ImageView userProfilePictureView;
    @FXML
    private VBox feedContainer;

    private Client client;
    private Stage stage;
    private CountDownLatch latch;

    public void setNewsFeedController(Client client, Stage stage) {
        try {
            latch = new CountDownLatch(1);
            client.setLatch(latch);

            client.getServerOutput().writeObject("NewsFeed: open");
            client.getServerOutput().flush();

            client.clientIsConnectedToNewsFeed();

            latch.await();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

        this.client = client;
        this.stage = stage;

        userProfilePictureView.setImage(client.getProfilePicture());
        Circle clip = new Circle(35, 35, 35);
        userProfilePictureView.setClip(clip);

        NewsFeedLayout.setPrefWidth(Screen.SCREENWIDTH);
        NewsFeedLayout.setPrefHeight(Screen.SCREENHEIGHT);
        NewsFeedView.scaleXProperty().bind(NewsFeedLayout.widthProperty().divide(1600));
        NewsFeedView.scaleYProperty().bind(NewsFeedLayout.heightProperty().divide(900));
    }

    @FXML
    public void onChatButtonClicked(ActionEvent mouseEvent) throws IOException {
        // Loading inbox page
        System.out.println("Chat button clicked!");
        client.disconnectFromFeedServer();
        client.getInbox().startInboxView(client, stage);
        //client.
    }

    @FXML
    public void onHomeButtonClicked(ActionEvent mouseEvent) throws IOException {
        // Loading home page
        System.out.println("Home button clicked!");
        client.disconnectFromFeedServer();
        client.getHomePage().startHomePageView(client, stage);
    }

    @FXML
    public void onNotificationButtonClick(ActionEvent actionEvent) throws IOException {
        // Loading notification page
        client.disconnectFromFeedServer();
        client.getNotificationPage().startNotificationPageView(client, stage);
    }

    public void onProfileButtonClick(ActionEvent actionEvent) throws IOException {
        // Loading profile page
        client.disconnectFromFeedServer();
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

    public void addPostToFeed(String post) {
        Label label = new Label(post);
        label.setWrapText(true);
        label.setStyle("-fx-padding: 10; -fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-background-radius: 5;");
        feedContainer.getChildren().add(0, label);
    }


}
