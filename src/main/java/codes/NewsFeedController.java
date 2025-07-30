package codes;


import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.bridj.cpp.std.vector;
import javafx.util.Duration;
import javafx.util.Pair;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class NewsFeedController {
    @FXML
    public StackPane NewsFeedLayout;
    @FXML
    public Group NewsFeedView;
    @FXML
    public ImageView userProfilePictureView;
    @FXML
    public Circle notificationDot;
    @FXML
    private VBox feedContainer;
    @FXML
    private VBox friendsSidebarBox;
    @FXML
    private VBox pendingRequestsBox;
    @FXML private
    VBox currentFriendsBox;
    @FXML
    private TextField friendSearchBar;


    private boolean inFeed;
    private Client client;
    private Stage stage;
    private CountDownLatch latch;
    private final Map<Integer, Map<String, Label>> reactionLabelsByPostId = new HashMap<>();
    private final Map<Integer, String> userReactionsByPostId = new HashMap<>();
    private final Map<Integer, Map<String, Button>> reactionButtonsByPostId = new HashMap<>();
    private final Map<Integer, List<String>> allCommentsByPostId = new HashMap<>();
    private final Map<Integer, StackPane> iconStackByPostId = new HashMap<>();
    private final List<PostPacket> allPosts = new ArrayList<>();
    private final Set<Integer> displayedPostIds = new HashSet<>();
    private List<ClientInfo> allCurrentFriends = new ArrayList<>();



    Map<Integer, ScrollPane> commentScrollPaneByPostId = new HashMap<>();
    Map<Integer, VBox> commentBoxByPostId = new HashMap<>();


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
            client.sendToServer("load_clients");

            System.out.println("I am trying to load friends stuff");
            loadFriendData();

        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

        userProfilePictureView.setImage(client.getProfilePicture());
        Circle clip = new Circle(35, 35, 35);
        userProfilePictureView.setClip(clip);

        ImageView pfp = new ImageView(client.getProfilePicture());
        pfp.setFitWidth(40);
        pfp.setFitHeight(40);
        pfp.setClip(new Circle(20, 20, 20));

        Label nameLabel = new Label(client.getFullName());
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        HBox profileBox = new HBox(10, pfp, nameLabel);
        profileBox.setPadding(new Insets(10));
        profileBox.setAlignment(Pos.CENTER_LEFT);
        profileBox.setStyle("-fx-cursor: hand; -fx-background-color: white; -fx-background-radius: 12;");
        DropShadow shadow = new DropShadow();
        shadow.setRadius(8);
        shadow.setOffsetY(4);
        shadow.setColor(Color.rgb(0, 0, 0, 0.2)); // Darker shadow

        profileBox.setEffect(shadow);
        profileBox.setStyle("-fx-background-color: white; -fx-background-radius: 12;");

        VBox.setMargin(profileBox, new Insets(10));



        profileBox.setOnMouseEntered(e -> {
            DropShadow hoverShadow = new DropShadow();
            hoverShadow.setRadius(12);
            hoverShadow.setOffsetY(4);
            hoverShadow.setColor(Color.rgb(0, 0, 0, 0.3));
            profileBox.setEffect(hoverShadow);
            profileBox.setStyle("-fx-background-color: #fafafa; -fx-background-radius: 12;");
        });

        profileBox.setOnMouseExited(e -> {
            DropShadow normalShadow = new DropShadow();
            normalShadow.setRadius(8);
            normalShadow.setOffsetY(4);
            normalShadow.setColor(Color.rgb(0, 0, 0, 0.2));
            profileBox.setEffect(normalShadow);
            profileBox.setStyle("-fx-background-color: white; -fx-background-radius: 12;");
        });


        profileBox.setOnMouseClicked(e -> showFilteredPosts(client.getMyId()));



        friendsSidebarBox.setAlignment(Pos.TOP_LEFT);
        friendsSidebarBox.setPadding(new Insets(10, 20, 10, 20));
        friendsSidebarBox.getChildren().add(0, profileBox);




        NewsFeedLayout.setPrefWidth(Screen.SCREENWIDTH);
        NewsFeedLayout.setPrefHeight(Screen.SCREENHEIGHT);
        NewsFeedView.scaleXProperty().bind(NewsFeedLayout.widthProperty().divide(1600));
        NewsFeedView.scaleYProperty().bind(NewsFeedLayout.heightProperty().divide(900));

        inFeed = true;

        new Thread(() -> {
            while (inFeed) {
                if (client.hasNewNotification()) {
                    notificationDot.setOpacity(1);
                }
                else {
                    notificationDot.setOpacity(0);
                }
            }
        }).start();
    }

    @FXML
    public void onChatButtonClicked(ActionEvent mouseEvent) throws IOException {
        // Loading inbox page
        System.out.println("Chat button clicked!");
        client.disconnectFromFeedServer();
        inFeed = false;

        client.getInbox().startInboxView(client, stage);
        //client.
    }

    @FXML
    public void onHomeButtonClicked(ActionEvent mouseEvent) {
        // Loading home page
        System.out.println("Home button clicked!");
        client.disconnectFromFeedServer();
        inFeed = false;

        try {
            client.getHomePage().startHomePageView(client, stage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void onRefreshButtonClick() {
        System.out.println("Refresh button clicked!");
        client.disconnectFromFeedServer();
        inFeed = false;
        try {
            client.getNewsFeed().startNewsFeedView(client, stage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void onNotificationButtonClick(ActionEvent actionEvent) {
        client.resetNotificationStatus();

        Rectangle background = new Rectangle(350, 350);
        background.setArcWidth(28);
        background.setArcHeight(28);
        background.setFill(Color.web("#f4f4f4"));
        background.setStroke(Color.BLACK);
        background.setStrokeWidth(0);
        background.setLayoutX(0);
        background.setLayoutY(0);

        Label notificationLabel = new Label("Notifications");
        notificationLabel.setStyle("-fx-background-color: transparent; -fx-font-family: Open Sans; -fx-font-size: 20; -fx-font-weight: bold;");
        notificationLabel.setAlignment(Pos.CENTER);

        HBox notificationLabelBox = new HBox(notificationLabel);
        notificationLabelBox.setAlignment(Pos.CENTER);

        VBox notificationBox = new VBox(3);
        notificationBox.setPrefSize(321, 334);
        notificationBox.setStyle("-fx-background-color: transparent;");

        ScrollPane notificationScroller = new ScrollPane(new VBox(notificationLabelBox, notificationBox));
        notificationScroller.setLayoutX(14);
        notificationScroller.setLayoutY(0);
        notificationScroller.setPrefSize(321, 334);
        notificationScroller.setStyle("-fx-background-color: transparent;");
        notificationScroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        notificationScroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        Pane boxContainer = new Pane(background, notificationScroller);
        boxContainer.setLayoutX(99);
        boxContainer.setLayoutY(460);

        boxContainer.setOpacity(0);
        boxContainer.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.15)));
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), boxContainer);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        Vector<ClientInfo> users = client.getClients();

        for (String sender : client.notification.keySet()) {
            ClientInfo clientInfo = null;

            for (int j = 0; j < users.size(); j++) {
                if (users.get(j).getId().equals(sender)) {
                    clientInfo = users.get(j);
                }
            }

            Image profilePicture = new Image(new ByteArrayInputStream(clientInfo.getProfilePicture()));
            ImageView profilePictureView = new ImageView(profilePicture);
            profilePictureView.setFitWidth(24);
            profilePictureView.setFitHeight(24);
            Circle clip = new Circle(12, 12, 12);
            profilePictureView.setClip(clip);
            String text;
            if (client.notification.get(sender).getKey().equals("message")) {
                text = "sent a message.";
            }
            else {
                text = "called you.";
            }
            Label textLabel = new Label(clientInfo.getFirstName() + " " + clientInfo.getLastName() + " " + text);
            if (client.notification.get(sender).getValue().equals("unseen")) {
                textLabel.setStyle("-fx-background-color: transparent; -fx-font-family: Open Sans; -fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #000000");
                client.notification.put(sender, new Pair<>(client.notification.get(sender).getKey(), "seen"));
                try {
                    client.getServerOutput().writeObject("seen:" + sender + "," + client.getId());
                    client.getServerOutput().flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            else {
                textLabel.setStyle("-fx-background-color: transparent; -fx-font-family: Open Sans; -fx-font-size: 16; -fx-text-fill: #000000");
            }
            textLabel.setWrapText(true);
            textLabel.setAlignment(Pos.CENTER);
            HBox notificationInfo = new HBox(profilePictureView, textLabel);
            notificationInfo.setSpacing(5);
            notificationInfo.setStyle("-fx-background-color: transparent; -fx-background-radius: 10; -fx-padding: 5px;");

            notificationInfo.setOnMouseEntered(event -> {
                notificationInfo.setStyle("-fx-background-color: #d5d7db; -fx-background-radius: 10; -fx-padding: 5px;");
            });

            notificationInfo.setOnMouseExited(event -> {
                notificationInfo.setStyle("-fx-background-color: transparent; -fx-background-radius: 10; -fx-padding: 5px;");
            });

            notificationInfo.setOnMouseClicked(event -> {
                client.disconnectFromFeedServer();

                try {
                    client.getInbox().startInboxView(client, stage);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                client.getInbox().getInboxController().startChat(sender);
            });

            notificationBox.getChildren().add(0, notificationInfo);
        }

        Platform.runLater(() -> {
            stage.getScene().addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                Bounds boxBounds = boxContainer.localToScene(boxContainer.getBoundsInParent());
                double x = event.getSceneX();
                double y = event.getSceneY();

                if (!boxBounds.contains(x, y)) {
                    NewsFeedView.getChildren().remove(boxContainer);
                }
            });
        });

        NewsFeedView.getChildren().add(boxContainer);
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
                        Parent newPopupRoot = loader.load();
                        FriendFinderController controller = loader.getController();
                        controller.setClient(client);

                        Scene newScene = new Scene(newPopupRoot);
                        newScene.getStylesheets().add(getClass().getResource("/FeedStyles.css").toExternalForm());

                        Stage popupStage = new Stage();
                        popupStage.setTitle("Find Friends");
                        popupStage.setScene(newScene);
                        popupStage.initModality(Modality.APPLICATION_MODAL);
                        popupStage.show();
                    } catch (IOException e) {
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

        System.out.println("I am in newsFeed Controller trying to add it");

        client.fetchFriendStatusMap();

        System.out.println("I am in newsFeed Controller trying to add it");

        if (!packet.getAuthor().equals(client.getMyId()) && !"friends".equals(client.getCachedFriendStatus(packet.getAuthor()))) {
            System.out.println("rejecting "+packet.getAuthor()+"'s post for not being my friend "+ client.getCachedFriendStatus(packet.getAuthor()));
            return;
        }

        System.out.println("I am past the check up stuff");

        Vector<ClientInfo> myClients=client.getClients();

        int postId = packet.getPostId();

        VBox postBox = new VBox();
        postBox.setStyle("-fx-padding: 10; -fx-border-color: #ccc; -fx-border-width: 0 0 1px 0;");
        postBox.getStyleClass().add("post");


        ImageView profileView = null;
        String fullName = packet.getAuthor();

        for (ClientInfo thisClient : client.getClients()) {
            if (packet.getAuthor().equals(thisClient.getId())) {
                fullName = thisClient.getFirstName() + " " + thisClient.getLastName();
                Image pfp = new Image(new ByteArrayInputStream(thisClient.getProfilePicture()));
                profileView = new ImageView(pfp);
                profileView.setFitWidth(40);
                profileView.setFitHeight(40);
                profileView.setClip(new javafx.scene.shape.Circle(20, 20, 20));
                break;
            }
        }


        Label nameLabel = new Label(fullName);
        nameLabel.getStyleClass().add("username");

        Label timestampLabel = new Label(packet.getFormattedTimestamp());
        timestampLabel.getStyleClass().add("timestamp");

        VBox nameAndTime = new VBox(nameLabel, timestampLabel);
        nameAndTime.setAlignment(Pos.CENTER_LEFT);


        HBox header = new HBox(10, profileView, nameAndTime);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));


        Label contentLabel = new Label(packet.getContent());
        contentLabel.getStyleClass().add("content");
        contentLabel.setWrapText(true);

        VBox postBodyBox = new VBox(header, contentLabel);
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
        }


        //Reactions


        HBox reactionRow = new HBox(10);
        reactionRow.setStyle("-fx-alignment: center-left;");

        ImageView baseIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/rose_gold_heart.png"), 24, 24, true, true));
        StackPane iconStack = new StackPane(baseIcon);
        iconStackByPostId.put(postId, iconStack);


        if ("like".equals(packet.getUserReactedType())) {
            ImageView overlay = new ImageView(new Image(getClass().getResourceAsStream("/images/redHeart.png"), 24, 24, true, true));
            iconStack.getChildren().add(overlay);
        }

        Label countLabel = new Label(String.valueOf(packet.getReactionCounts().getOrDefault("like", 0)));

        HBox iconWithCount = new HBox(4);
        iconWithCount.setAlignment(Pos.CENTER_LEFT);
        iconWithCount.setPadding(new Insets(2));
        iconWithCount.setMinWidth(60);
        iconWithCount.getChildren().addAll(iconStack, countLabel);

        Button likeButton = new Button();
        likeButton.setGraphic(iconWithCount);
        likeButton.setStyle("-fx-background-color: transparent;");
        likeButton.setMinHeight(36);
        likeButton.setMinWidth(70);
        likeButton.setPadding(new Insets(4));



        likeButton.setOnAction(e -> sendReactionToServer(String.valueOf(postId), "like"));

        Map<String, Label> countLabels = new HashMap<>();
        countLabels.put("like", countLabel);
        reactionLabelsByPostId.put(postId, countLabels);

        Map<String, Button> reactionButtons = new HashMap<>();
        reactionButtons.put("like", likeButton);
        reactionButtonsByPostId.put(postId, reactionButtons);
        reactionRow.getChildren().add(likeButton);
        postBox.getChildren().add(reactionRow);

        System.out.println("Everything is done except you know who");


        //Comments

        ImageView commentIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/blue_comment.png")));
        commentIcon.setFitWidth(24);
        commentIcon.setFitHeight(24);

        Button commentButton = new Button();
        commentButton.setGraphic(commentIcon);
        commentButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

        allCommentsByPostId.put(postId,packet.getComments());
        commentButton.setOnAction(e -> openCommentPopup(postId,allCommentsByPostId.get(postId)));
        reactionRow.getChildren().add(commentButton);


        if (!displayedPostIds.contains(packet.getPostId())) {
            allPosts.add(packet);
            displayedPostIds.add(packet.getPostId());
        }


        feedContainer.getChildren().add(0, postBox);
    }

    private void openCommentPopup(int postId,List<String> pastComments) {
        //client.fetchCommentsForPost(postId, comments -> Platform.runLater(() -> openCommentPopup(postId, comments)));


        VBox commentsBox = new VBox();
        commentsBox.getStyleClass().add("comment-section");

        ScrollPane commentScrollPane = new ScrollPane(commentsBox);
        commentScrollPane.setPrefHeight(400);
        commentScrollPane.setMaxHeight(400);
        commentScrollPane.setFitToWidth(true);
        commentScrollPane.getStyleClass().add("comment-scroll-pane");


        commentScrollPaneByPostId.put(postId, commentScrollPane);
        commentBoxByPostId.put(postId, commentsBox);


        for (String comment : pastComments) {
            String[] parts = comment.split(":", 2);
            String sender = parts.length > 0 ? parts[0].trim() : "Unknown";
            String message = parts.length > 1 ? parts[1].trim() : "";


            Label senderLabel = new Label(sender);
            senderLabel.getStyleClass().add("comment-sender");

            Label messageLabel = new Label(message);
            messageLabel.getStyleClass().add("comment-label");

            VBox commentBlock = new VBox(2, senderLabel, messageLabel);
            commentBlock.getStyleClass().add("comment-block");

            commentsBox.getChildren().add(commentBlock);


            //System.out.println("Comment: "+comment);
        }


        // Input row
        HBox commentInputRow = new HBox(10);  // increased spacing between field and button
        commentInputRow.setPadding(new Insets(15, 10, 10, 10));
        commentInputRow.setAlignment(Pos.CENTER_LEFT);  // aligned better horizontally



        TextField commentField = new TextField();
        commentField.setPromptText("Write a comment...");
        commentField.getStyleClass().add("comment-input");
        HBox.setHgrow(commentField, Priority.ALWAYS);
        commentField.setMaxWidth(Double.MAX_VALUE);


        Image sendImage = new Image(getClass().getResourceAsStream("/images/send_comment_button.png"));
        ImageView sendIcon = new ImageView(sendImage);
        sendIcon.setFitWidth(40);
        sendIcon.setFitHeight(40);

        Button postCommentButton = new Button();
        postCommentButton.setGraphic(sendIcon);
        postCommentButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

        commentInputRow.getChildren().addAll(commentField, postCommentButton);

        String currentUsername = client.getMyId();



        Runnable postHandler = () -> {
            String text = commentField.getText().trim();
            if (!text.isEmpty()) {
                client.sendComment(postId, text);
                commentField.clear();

            }
        };

        postCommentButton.setOnAction(e -> postHandler.run());
        commentField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) postHandler.run();
        });


        VBox container = new VBox(15, commentScrollPane, commentInputRow);
        container.setPadding(new Insets(20));
        container.getStyleClass().add("popup-container");


        Scene popupScene = new Scene(container, 500, 500);
        popupScene.getStylesheets().add(getClass().getResource("/FeedStyles.css").toExternalForm());
        Stage popupStage = new Stage();
        popupStage.setTitle("Comments");
        popupStage.setScene(popupScene);
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.show();
    }

    public void addLiveComment(CommentPacket comment) {
        int postId = comment.getPostId();
        String sender = comment.getCommenter();
        String message = comment.getCommentText();
        String formatted = sender + ": " + message;

        allCommentsByPostId.putIfAbsent(postId, new ArrayList<>());
        allCommentsByPostId.get(postId).add(formatted);

        if (commentBoxByPostId.containsKey(postId)) {
            VBox commentsBox = commentBoxByPostId.get(postId);

            Label senderLabel = new Label(sender);
            senderLabel.getStyleClass().add("comment-sender");

            Label messageLabel = new Label(message);
            messageLabel.getStyleClass().add("comment-label");

            VBox commentBlock = new VBox(2, senderLabel, messageLabel);
            commentBlock.getStyleClass().add("comment-block");

            Platform.runLater(() -> {
                commentsBox.getChildren().add(commentBlock);

                ScrollPane scrollPane = commentScrollPaneByPostId.get(postId);
                scrollPane.layout();
                scrollPane.setVvalue(1.0);
            });

        } else {
            System.out.println("No comment box found for postId: " + postId);
        }
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
            StackPane iconStack = iconStackByPostId.get(postId);
            if (iconStack != null) {
                iconStack.getChildren().clear();
                ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(
                        "like".equals(newType) ? "/images/redHeart.png" : "/images/rose_gold_heart.png"
                ), 24, 24, true, true));
                iconStack.getChildren().add(icon);
            }

            if ("none".equals(newType)) {
                userReactionsByPostId.remove(postId);
            } else {
                userReactionsByPostId.put(postId, newType);
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

        client.fetchFriendStatusMap();
        loadCurrentFriends();
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

    private void showFilteredPosts(String authorId) {
        feedContainer.getChildren().clear();
        List<PostPacket> copy = new ArrayList<>(allPosts);
        for (PostPacket packet : copy) {
            if (packet.getAuthor().equals(authorId)) {
                addPostToFeed(packet);
            }
        }
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
    @FXML
    private void onPendingRequestsClicked() {
        List<ClientInfo> pending = client.getPendingRequests();

        VBox container = new VBox(10);
        container.setPadding(new Insets(20));
        container.setAlignment(Pos.TOP_CENTER);
        container.setStyle("-fx-background-color: white;");

        Label title = new Label("Pending Friend Requests");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        container.getChildren().add(title);

        if (pending == null || pending.isEmpty()) {
            Label emptyLabel = new Label("You have no pending requests.");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
            container.getChildren().add(emptyLabel);
        } else {
            for (ClientInfo requester : pending) {
                HBox row = createUserRow(requester, "pending");

                Button accept = new Button("Accept");
                Button reject = new Button("Reject");

                accept.setOnAction(e -> {
                    client.acceptFriendRequest(requester.getId());
                    loadFriendData();
                    ((Stage) ((Button) e.getSource()).getScene().getWindow()).close();
                });

                reject.setOnAction(e -> {
                    client.rejectFriendRequest(requester.getId());
                    loadFriendData();
                    ((Stage) ((Button) e.getSource()).getScene().getWindow()).close();
                });

                row.getChildren().addAll(accept, reject);
                container.getChildren().add(row);
            }
        }

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(400, 500);
        scrollPane.setStyle("-fx-background-color: white;");

        Stage popupStage = new Stage();
        popupStage.setTitle("Pending Requests");
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setScene(new Scene(scrollPane));
        popupStage.show();
    }

    public void loadCurrentFriends() {
        currentFriendsBox.getChildren().clear();
        allCurrentFriends = client.getFriendList();  // store for search filtering

        for (ClientInfo friend : allCurrentFriends) {
            HBox friendRow = createFriendRow(friend);
            currentFriendsBox.getChildren().add(friendRow);
        }

        // Set up search filter
        friendSearchBar.textProperty().addListener((obs, oldVal, newVal) -> {
            currentFriendsBox.getChildren().clear();
            for (ClientInfo friend : allCurrentFriends) {
                String search = newVal.toLowerCase();
                if (friend.getFullName().toLowerCase().contains(search) || friend.getId().toLowerCase().contains(search)) {
                    currentFriendsBox.getChildren().add(createFriendRow(friend));
                }
            }
        });
    }

    private HBox createFriendRow(ClientInfo friend) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        DropShadow softShadow = new DropShadow(3, Color.rgb(0, 0, 0, 0.08));
        row.setEffect(softShadow);

        ImageView pfp = new ImageView(new Image(new ByteArrayInputStream(friend.getProfilePicture())));
        pfp.setFitWidth(32);
        pfp.setFitHeight(32);
        pfp.setClip(new Circle(16, 16, 16));

        Label name = new Label(friend.getFullName());
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        row.getChildren().addAll(pfp, name);

        row.setOnMouseClicked(e -> {
            showFilteredPosts(friend.getId());
            showFriendPopup(friend);
        });

        row.setOnMouseEntered(e -> {
            DropShadow hoverShadow = new DropShadow(8, Color.rgb(0, 0, 0, 0.15));
            row.setEffect(hoverShadow);
            row.setCursor(Cursor.HAND);
        });

        row.setOnMouseExited(e -> {
            row.setEffect(softShadow);
            row.setCursor(Cursor.DEFAULT);
        });

        return row;
    }


    private void showFriendPopup(ClientInfo friend) {
        VBox popupContent = new VBox(10);
        popupContent.setAlignment(Pos.CENTER);
        popupContent.setPadding(new Insets(20));

        ImageView pfp = new ImageView(new Image(new ByteArrayInputStream(friend.getProfilePicture())));
        pfp.setFitWidth(80);
        pfp.setFitHeight(80);
        pfp.setClip(new Circle(40, 40, 40));

        Label fullName = new Label(friend.getFullName());
        fullName.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label username = new Label("@" + friend.getId());
        username.setStyle("-fx-text-fill: #888;");

        Button unfriendBtn = new Button("Unfriend");
        unfriendBtn.setStyle("-fx-background-color: #ffdddd; -fx-font-weight: bold;");
        unfriendBtn.setOnAction(e -> {
            client.unfriend(friend.getId());
            loadFriendData();
            ((Stage) unfriendBtn.getScene().getWindow()).close();
        });

        popupContent.getChildren().addAll(pfp, fullName, username, unfriendBtn);

        Scene popupScene = new Scene(popupContent, 300, 250);
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Friend Details");
        popupStage.setScene(popupScene);


        if (stage != null) {
            popupStage.setX(stage.getX() + stage.getWidth() - 320); // 20px margin
            popupStage.setY(stage.getY() + 150); // vertically down a bit
        }

        popupStage.setOnHidden(e -> {
            feedContainer.getChildren().clear();
            List<PostPacket> copy = new ArrayList<>(allPosts);
            for (PostPacket packet : copy) {
                addPostToFeed(packet);
            }
        });

        popupStage.show();

    }

    @FXML
    public void onShowAllClick(){
        feedContainer.getChildren().clear();
        List<PostPacket> copy = new ArrayList<>(allPosts);
        for (PostPacket packet : copy) {
            addPostToFeed(packet);

        }
    }



}