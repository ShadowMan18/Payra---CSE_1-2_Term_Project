package codes;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class HomePageController {
    @FXML
    public StackPane HomePageLayout;
    @FXML
    public Group HomePageView;
    @FXML
    public ImageView userProfilePictureView;

    private Client client;
    private Stage stage;


    public void setHomePageController(Client client, Stage stage) {
        this.client = client;
        this.stage = stage;
        HomePageLayout.setPrefWidth(Screen.SCREENWIDTH);
        HomePageLayout.setPrefHeight(Screen.SCREENHEIGHT);
        HomePageView.scaleXProperty().bind(HomePageLayout.widthProperty().divide(1600));
        HomePageView.scaleYProperty().bind(HomePageLayout.heightProperty().divide(900));

        Platform.runLater(() -> {
                userProfilePictureView.setImage(client.getProfilePicture());
                Circle clip = new Circle(35, 35, 35);
                userProfilePictureView.setClip(clip);
        });
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
    public void onProfileButtonClick(MouseEvent mouseEvent) {
        // Loading profile popup

        Rectangle background = new Rectangle(350, 350);
        background.setArcWidth(28);
        background.setArcHeight(28);
        background.setFill(Color.web("#f4f4f4"));
        background.setStroke(Color.BLACK);
        background.setStrokeWidth(0);
        background.setLayoutX(0);
        background.setLayoutY(0);

        VBox profileBox = new VBox(3);
        profileBox.setPrefSize(321, 334);
        profileBox.setStyle("-fx-background-color: transparent;");

//        ScrollPane notificationScroller = new ScrollPane(profileBox);
//        notificationScroller.setLayoutX(14);
//        notificationScroller.setLayoutY(0);
//        notificationScroller.setPrefSize(321, 334);
//        notificationScroller.setStyle("-fx-background-color: transparent;");
//        notificationScroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
//        notificationScroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        Pane profileContainer = new Pane(new StackPane(background, profileBox));
        profileContainer.setLayoutX(600);
        profileContainer.setLayoutY(112);

        profileContainer.setOpacity(0);
        profileContainer.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.15)));
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), profileContainer);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        Button signOutButton = new Button("Sign Out");
        signOutButton.setPrefSize(100, 30);
        signOutButton.setStyle("-fx-background-color: black; -fx-text-fill: white; -fx-background-radius: 5;");

        signOutButton.setOnAction(event -> {
            try {
                client.getLoginPage().startLoginPageView(client, stage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        profileBox.getChildren().add(signOutButton);

        Platform.runLater(() -> {
            stage.getScene().addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                Bounds boxBounds = profileContainer.localToScene(profileContainer.getBoundsInParent());
                double x = event.getSceneX();
                double y = event.getSceneY();

                if (!boxBounds.contains(x, y)) {
                    HomePageView.getChildren().remove(profileContainer);
                }
            });
        });

        HomePageView.getChildren().add(profileContainer);
    }
}
