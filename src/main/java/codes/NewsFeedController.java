package codes;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
    private final Map<Integer, Map<String, Label>> reactionLabelsByPostId = new HashMap<>();
    private final Map<Integer, String> userReactionsByPostId = new HashMap<>();
    private final Map<Integer, Map<String, Button>> reactionButtonsByPostId = new HashMap<>();


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
    private final String[] reactionTypes = { "like", "love", "sad" };

    public void addPostToFeed(String rawPost) {
        String[] parts = rawPost.split("\\|", 6);
        if (parts.length < 4) return;

        String postId = parts[0];
        String timestamp = parts[1];
        String username = parts[2];
        String content = parts[3];
        Map<String, Integer> reactionCounts = parseReactionCounts(parts.length >= 5 ? parts[4] : "");
        String userReactedType = (parts.length == 6) ? parts[5] : "none";
        int postIdInt = Integer.parseInt(postId);
        userReactionsByPostId.put(postIdInt, userReactedType);

        VBox postBox = new VBox();
        postBox.setStyle("-fx-padding: 10; -fx-border-color: #ccc; -fx-border-width: 0 0 1px 0;");
        postBox.getStyleClass().add("post");

        Label usernameLabel = new Label(username);
        usernameLabel.getStyleClass().add("username");

        Label timestampLabel = new Label(timestamp);
        timestampLabel.getStyleClass().add("timestamp");

        Label contentLabel = new Label(content);
        contentLabel.getStyleClass().add("content");
        contentLabel.setWrapText(true);

        HBox reactionRow = new HBox(10);
        reactionRow.setStyle("-fx-alignment: center-left;");

        // Reaction setup
        ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/images/redHeart.png"), 24, 24, true, true));
        Label countLabel = new Label(String.valueOf(reactionCounts.getOrDefault("like", 0)));

        VBox iconWithCount = new VBox(icon, countLabel);
        iconWithCount.setStyle("-fx-alignment: center;");

        Button likeButton = new Button();
        likeButton.setGraphic(iconWithCount);
        likeButton.setStyle("-fx-background-color: transparent;");
        if ("like".equals(userReactedType)) {
            likeButton.setStyle("-fx-background-color: #d0f0c0;");
        }

        likeButton.setOnAction(e -> sendReactionToServer(postId, "like"));

        Map<String, Label> countLabels = new HashMap<>();
        countLabels.put("like", countLabel);
        reactionLabelsByPostId.put(postIdInt, countLabels);

        Map<String, Button> reactionButtons = new HashMap<>();
        reactionButtons.put("like", likeButton);
        reactionButtonsByPostId.put(postIdInt, reactionButtons);

        reactionRow.getChildren().add(likeButton);
        postBox.getChildren().addAll(usernameLabel, timestampLabel, contentLabel, reactionRow);
        feedContainer.getChildren().add(0, postBox);
    }

    private void sendReactionToServer(String postIdStr, String selectedType) {
        try {
            int postId = Integer.parseInt(postIdStr);

            String currentType = userReactionsByPostId.getOrDefault(postId, "none");

            if (currentType.equals(selectedType)) {
                // Toggle off
                client.sendReaction(postId, "none");
            } else {
                // add
                client.sendReaction(postId, selectedType);
            }

        } catch (NumberFormatException e) {
            System.err.println("Invalid post ID format: " + postIdStr);
        }
    }


    private Map<String, Integer> parseReactionCounts(String data) {
        Map<String, Integer> map = new HashMap<>();
        for (String entry : data.split(";")) {
            String[] pair = entry.split("=");
            if (pair.length == 2) {
                try {
                    map.put(pair[0], Integer.parseInt(pair[1]));
                } catch (NumberFormatException ignored) {}
            }
        }
        return map;
    }

    public void updateReactionOnPost(int postId, String reactor, String oldType, String newType) {
        if (!"like".equals(oldType) && !"like".equals(newType)) return;

        Map<String, Label> labels = reactionLabelsByPostId.get(postId);
        Map<String, Button> buttons = reactionButtonsByPostId.get(postId);
        if (labels == null || buttons == null) return;

        if (client.getId().equals(reactor)) {
            if ("none".equals(newType)) {
                userReactionsByPostId.remove(postId);
                buttons.get("like").setStyle("-fx-background-color: transparent;");
            } else {
                userReactionsByPostId.put(postId, "like");
                buttons.get("like").setStyle("-fx-background-color: #d0f0c0;");
            }
        }

        Label countLabel = labels.get("like");
        if (countLabel != null) {
            try {
                int count = Integer.parseInt(countLabel.getText());
                if ("like".equals(oldType)) count--;
                if ("like".equals(newType)) count++;
                countLabel.setText(String.valueOf(Math.max(0, count)));
            } catch (NumberFormatException ignored) {}
        }
    }


}
