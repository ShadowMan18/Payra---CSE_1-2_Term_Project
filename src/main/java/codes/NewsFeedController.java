package codes;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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
    @FXML
    private VBox friendsSidebarBox;
    @FXML
    private VBox pendingRequestsBox;
    @FXML private
    VBox currentFriendsBox;


    private Client client;
    private Stage stage;
    private CountDownLatch latch;
    private final Map<Integer, Map<String, Label>> reactionLabelsByPostId = new HashMap<>();
    private final Map<Integer, String> userReactionsByPostId = new HashMap<>();
    private final Map<Integer, Map<String, Button>> reactionButtonsByPostId = new HashMap<>();

    Map<Integer, ScrollPane> commentScrollPaneByPostId = new HashMap<>();


    public void setNewsFeedController(Client client, Stage stage) {
        try {
            System.out.println("Setting up newsFeed");
            latch = new CountDownLatch(1);
            client.setLatch(latch);

            client.getServerOutput().writeObject("NewsFeed: open");
            client.getServerOutput().flush();

            client.clientIsConnectedToNewsFeed();

            latch.await();

            this.client = client;
            this.stage = stage;

            System.out.println("I am trying to load friends stuff");
            loadFriendData();

        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

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

    @FXML
    private void onFindFriendsClick() {
        System.out.println("Find Friends clicked");

        new Thread(() -> {
            try {
                client.fetchClients();
                System.out.println("Clients has been fetched");

                client.fetchFriendStatusMap();
                System.out.println("Friend status map has been fetched");

                try { Thread.sleep(300); } catch (InterruptedException ignored) {}

                Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/codes/FriendFinderPopup.fxml"));
                        Parent popupRoot = loader.load();
                        FriendFinderController controller = loader.getController();
                        controller.setClient(client);

                        Stage popupStage = new Stage();
                        popupStage.setTitle("Find Friends");
                        popupStage.setScene(new Scene(popupRoot));
                        popupStage.initModality(Modality.APPLICATION_MODAL);
                        popupStage.show();
                    } catch (IOException e) {
                        System.out.println("FXML load failed:");
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                System.out.println("Error inside friend fetch thread:");
                e.printStackTrace();
            }
        }).start();
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

    public void addPostToFeed(PostPacket packet) {

        //System.out.println("I am in newsFeed Controller trying to add it");

        //client.fetchFriendStatusMap();

        //System.out.println("I am in newsFeed Controller trying to add it");

//        if (!packet.getAuthor().equals(client.getMyId()) && !"friends".equals(client.getCachedFriendStatus(packet.getAuthor()))) {
//            System.out.println("rejecting "+packet.getAuthor()+"'s post for not being my friend "+ client.getCachedFriendStatus(packet.getAuthor()));
//            return;
//        }

        //System.out.println("I am past the check up stuff");

        int postId = packet.getPostId();
        userReactionsByPostId.put(postId, packet.getUserReactedType());

        VBox postBox = new VBox();
        postBox.setStyle("-fx-padding: 10; -fx-border-color: #ccc; -fx-border-width: 0 0 1px 0;");
        postBox.getStyleClass().add("post");

        Label usernameLabel = new Label(packet.getAuthor());
        usernameLabel.getStyleClass().add("username");

        Label timestampLabel = new Label(packet.getFormattedTimestamp());
        timestampLabel.getStyleClass().add("timestamp");

        Label contentLabel = new Label(packet.getContent());
        contentLabel.getStyleClass().add("content");
        contentLabel.setWrapText(true);

        VBox postBodyBox = new VBox(usernameLabel, timestampLabel, contentLabel);
        postBodyBox.getStyleClass().add("post-body");
        postBox.getChildren().add(postBodyBox);


        if (packet.getFileData() != null && packet.getFileData().length > 0) {
            String filename = packet.getFileName().toLowerCase();
            if (filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                javafx.scene.image.Image fxImage = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(packet.getFileData()));
                ImageView imageView = new ImageView(fxImage);
                imageView.setFitWidth(300);
                imageView.setPreserveRatio(true);
                postBox.getChildren().add(imageView);
            }
            // need more handlers to support video or audio previews, that's just image for now
        }

        HBox reactionRow = new HBox(10);
        reactionRow.setStyle("-fx-alignment: center-left;");

        ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/images/redHeart.png"), 24, 24, true, true));
        Label countLabel = new Label(String.valueOf(packet.getReactionCounts().getOrDefault("like", 0)));

        VBox iconWithCount = new VBox(icon, countLabel);
        iconWithCount.setStyle("-fx-alignment: center;");

        Button likeButton = new Button();
        likeButton.setGraphic(iconWithCount);
        likeButton.setStyle("-fx-background-color: transparent;");
        if ("like".equals(packet.getUserReactedType())) {
            likeButton.setStyle("-fx-background-color: #d0f0c0;");
        }

        likeButton.setOnAction(e -> sendReactionToServer(String.valueOf(postId), "like"));

        Map<String, Label> countLabels = new HashMap<>();
        countLabels.put("like", countLabel);
        reactionLabelsByPostId.put(postId, countLabels);

        Map<String, Button> reactionButtons = new HashMap<>();
        reactionButtons.put("like", likeButton);
        reactionButtonsByPostId.put(postId, reactionButtons);
        reactionRow.getChildren().add(likeButton);
        postBox.getChildren().add(reactionRow);


        //System.out.println("I got through the rest of it");

        // === Comments Section ===
        VBox commentsBox = new VBox();
        commentsBox.getStyleClass().add("comment-section");

        ScrollPane commentScrollPane = new ScrollPane(commentsBox);
        commentScrollPane.setFitToWidth(true);

        commentScrollPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
        commentScrollPane.setMaxHeight(150); // scroll only after enough comments

        commentScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        commentScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        commentScrollPane.getStyleClass().add("comment-scroll-pane");

        commentScrollPaneByPostId.put(postId, commentScrollPane);


        for (String comment : packet.getComments()) {
            Label commentLabel = new Label(comment);
            commentLabel.getStyleClass().add("comment-label");

            commentsBox.getChildren().add(commentLabel);
        }

// Text field to add a new comment
        HBox commentInputRow = new HBox(5);
        commentInputRow.setPadding(new Insets(5, 0, 0, 10));

        TextField commentField = new TextField();
        commentField.setPromptText("Write a comment...");
        commentField.setPrefWidth(300);

        Button postCommentButton = new Button("Send");

        commentField.getStyleClass().add("comment-input");
        postCommentButton.getStyleClass().add("send-button");


        //commentInputRow.getChildren().addAll(commentField, postCommentButton);
        String currentUsername = client.getMyId();
        postCommentButton.setOnAction(e -> {
            String text = commentField.getText().trim();
            if (!text.isEmpty()) {
                client.sendComment(postId, text);
                commentField.clear();
            }
        });

        commentField.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER -> {
                    String text = commentField.getText().trim();
                    if (!text.isEmpty()) {
                        client.sendComment(postId, text);
                        commentField.clear();
                    }
                }
            }
        });



        commentInputRow.getChildren().addAll(commentField, postCommentButton);

        commentsBox.layout();  // ensure layout pass
        commentsBox.setTranslateY(commentsBox.getHeight());  // scroll if wrapped in ScrollPane


        postBox.getChildren().addAll(commentInputRow, commentScrollPane);


        commentBoxByPostId.put(postId, commentsBox);
        feedContainer.getChildren().add(0, postBox);
        //System.out.println("Done");
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



    //What are friends for really?
    private void loadFriendData() {
        pendingRequestsBox.getChildren().clear();
        currentFriendsBox.getChildren().clear();

        //System.out.println("I want the requests I have, if any");

        List<ClientInfo> pending=client.getPendingRequests();

       //System.out.println("Received requests with a list of size: "+pending.size());
        for (ClientInfo requester : pending) {
            HBox row = createUserRow(requester, "pending");
            Button accept = new Button("Accept");
            Button reject = new Button("Reject");

            accept.setOnAction(e -> {
                client.acceptFriendRequest(requester.getId());
                loadFriendData();
            });

            reject.setOnAction(e -> {
                client.rejectFriendRequest(requester.getId());
                loadFriendData();
            });

            row.getChildren().addAll(accept, reject);
            pendingRequestsBox.getChildren().add(row);
        }

        //System.out.println("Now we'll try to get Friend list");

        // Load Friend List
        for (ClientInfo friend : client.getFriendList()) {
            System.out.println("My friend: "+friend.getId());
            HBox row = createUserRow(friend, "friend");
            Button unfriend = new Button("Unfriend");
            unfriend.setOnAction(e -> {
                client.unfriend(friend.getId());
                loadFriendData();
            });

            row.getChildren().add(unfriend);
            currentFriendsBox.getChildren().add(row);
        }

    }

    private HBox createUserRow(ClientInfo user, String type) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(5));
        row.setStyle("-fx-border-color: lightgray; -fx-alignment: center-left;");

        ImageView img = new ImageView(new Image(new ByteArrayInputStream(user.getProfilePicture())));
        img.setFitWidth(40);
        img.setFitHeight(40);
        img.setPreserveRatio(true);

        Label name = new Label(user.getFirstName() + " " + user.getLastName());
        name.setStyle("-fx-font-size: 14px;");

        row.getChildren().addAll(img, name);
        return row;
    }


    private void refreshCommentsForPost(VBox postBox, int postId, VBox commentsBox) {
        client.fetchCommentsForPost(postId, comments -> {
            commentsBox.getChildren().clear();
            for (String comment : comments) {
                Label commentLabel = new Label(comment);
                commentLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
                commentsBox.getChildren().add(commentLabel);
            }
        });
    }


    Map<Integer, VBox> commentBoxByPostId = new HashMap<>();


    public void addLiveComment(CommentPacket comment) {
        int postId = comment.getPostId();
        String commentText = comment.getCommenter() + ": " + comment.getCommentText();

        if (commentBoxByPostId.containsKey(postId)) {
            VBox commentsBox = commentBoxByPostId.get(postId);
            Label newCommentLabel = new Label(commentText);
            newCommentLabel.getStyleClass().add("comment-label");

            Platform.runLater(() -> {
                commentsBox.getChildren().add(newCommentLabel);

                Platform.runLater(() -> {
                    ScrollPane scrollPane = commentScrollPaneByPostId.get(postId);
                    scrollPane.setVvalue(1.0); // Scroll to bottom after layout
                });
            });

        } else {
            System.out.println("No comment box found for postId: " + postId);
        }
    }








}
