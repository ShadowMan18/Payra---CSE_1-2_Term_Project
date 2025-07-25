package codes;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

public class InboxController {
    @FXML
    public StackPane InboxLayout;
    @FXML
    public Group InboxView;
    @FXML
    public ImageView userProfilePictureView;
    @FXML
    public TextField SearchBar;
    @FXML
    public Label noUserFoundLabel;
    @FXML
    public ImageView ChatBox;
    @FXML
    public ImageView receiverProfilePictureView;
    @FXML
    public Label ReceiverName;
    @FXML
    public TextField Message;
    @FXML
    public ScrollPane MessageScroller;
    @FXML
    public VBox MessageContainer;
    @FXML
    public ScrollPane InboxScroller;
    @FXML
    public VBox InboxContainer;
    @FXML
    public ScrollPane emojiScroller;
    @FXML
    public VBox emojiPallet;
    @FXML
    public Rectangle emojiContainer;
    @FXML
    public Circle notificationDot;


    private Client client;
    private Stage stage;
    private String receiverId;
    private String receiverFirstName;
    private Image receiverProfilePicture;
    private String lastSender;
    private String lastMessageDate;
    private String filePath;
    private CountDownLatch latch;
    private boolean isEmojiPalletOpen;

    private Vector<String> emojiHexCodes = new Vector(Arrays.asList(
            "1f600", "1f601", "1f602", "1f603", "1f604", "1f605", "1f606", "1f609", "1f60a", "1f60b",
            "1f60c", "1f60d", "1f60e", "1f60f", "1f610", "1f611", "1f612", "1f613", "1f614", "1f615",
            "1f616", "1f617", "1f618", "1f619", "1f61a", "1f61b", "1f61c", "1f61d", "1f61e", "1f61f",
            "1f620", "1f621", "1f622", "1f623", "1f624", "1f625", "1f626", "1f627", "1f628", "1f629",
            "1f62a", "1f62b", "1f62c", "1f62d", "1f62e", "1f62f", "1f630", "1f631", "1f632", "1f633",
            "1f634", "1f635", "1f636", "1f637", "1f638", "1f639", "1f63a", "1f63b", "1f63c", "1f63d",
            "1f63e", "1f63f", "1f640", "1f641", "1f642", "1f643", "1f644", "1f910", "1f911", "1f912",
            "1f913", "1f914", "1f915", "1f916", "1f917", "1f918", "1f919", "1f91a", "1f91b", "1f91c",
            "1f91d", "1f91e", "1f91f", "1f920", "1f921", "1f922", "1f923", "1f924", "1f925", "1f926",
            "1f927", "1f928", "1f929", "1f92a", "1f92b", "1f92c", "1f92d", "1f92e", "1f92f", "1f930"
    ));


    public void setInboxController(Client client, Stage stage) {
        this.client = client;
        this.stage = stage;

        try {
            latch = new CountDownLatch(1);
            client.setLatch(latch);

            client.getServerOutput().writeObject("load_clients");
            client.getServerOutput().flush();

            latch.await();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        userProfilePictureView.setImage(client.getProfilePicture());
        Circle clip = new Circle(35, 35, 35);
        userProfilePictureView.setClip(clip);

        client.setChatStatus(false);
        noUserFoundLabel.setOpacity(0);
        Message.setOpacity(0);
        Message.setDisable(true);
        InboxLayout.setPrefWidth(Screen.SCREENWIDTH);
        InboxLayout.setPrefHeight(Screen.SCREENHEIGHT);
        InboxView.scaleXProperty().bind(InboxLayout.widthProperty().divide(1600));
        InboxView.scaleYProperty().bind(InboxLayout.heightProperty().divide(900));
        InboxScroller.setFitToWidth(true);
        InboxContainer.setFillWidth(true);
        InboxContainer.setMinHeight(Region.USE_PREF_SIZE);
        MessageScroller.setFitToWidth(true);
        MessageContainer.setFillWidth(true);
        MessageContainer.setMinHeight(Region.USE_PREF_SIZE);
        MessageContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            MessageScroller.setVvalue(1.0);
        });

        displayUsers("###");

        for (ClientInfo c : client.getClients()) {
            System.out.println(c.getFirstName());
        }

        new Thread(() -> {
            while (true) {
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
    public void onNewsFeedButtonClicked(ActionEvent mouseEvent) {
        // Loading news feed page

        try {
            latch = new CountDownLatch(1);
            client.setLatch(latch);

            client.getServerOutput().writeObject("close_chat");
            client.getServerOutput().flush();

            latch.await();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

        try {
            client.getNewsFeed().startNewsFeedView(client, stage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void onHomeButtonClicked(ActionEvent mouseEvent) {
        // Loading home page

        try {
            latch = new CountDownLatch(1);
            client.setLatch(latch);

            client.getServerOutput().writeObject("close_chat");
            client.getServerOutput().flush();

            latch.await();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

        try {
            client.getHomePage().startHomePageView(client, stage);
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
                startChat(sender);
            });

            notificationBox.getChildren().add(0, notificationInfo);
        }

        Platform.runLater(() -> {
            stage.getScene().addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                Bounds boxBounds = boxContainer.localToScene(boxContainer.getBoundsInParent());
                double x = event.getSceneX();
                double y = event.getSceneY();

                if (!boxBounds.contains(x, y)) {
                    InboxView.getChildren().remove(boxContainer);
                }
            });
        });

        InboxView.getChildren().add(boxContainer);
    }

    @FXML
    public void onEmojiButtonClick(ActionEvent actionEvent) {
        Rectangle background = new Rectangle(295, 250);
        background.setArcWidth(28);
        background.setArcHeight(28);
        background.setFill(Color.web("#f4f4f4"));
        background.setStroke(Color.LIGHTGRAY);
        background.setStrokeWidth(1);
        background.setLayoutX(0);
        background.setLayoutY(0);

        VBox emojiBox = new VBox(3);
        emojiBox.setPrefSize(265, 240);
        emojiBox.setStyle("-fx-background-color: transparent;");

        ScrollPane scrollPane = new ScrollPane(emojiBox);
        scrollPane.setLayoutX(14);
        scrollPane.setLayoutY(5);
        scrollPane.setPrefSize(265, 240);
        scrollPane.setStyle("-fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        Pane emojiContainer = new Pane(background, scrollPane);
        emojiContainer.setLayoutX(552);
        emojiContainer.setLayoutY(530);

        emojiContainer.setOpacity(0);
        emojiContainer.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.15)));
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), emojiContainer);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        for (int i = 0; i < 14; i++) {
            HBox emojiRow = new HBox();
            emojiRow.setPrefWidth(300);
            emojiRow.setPrefHeight(35);
            emojiRow.setSpacing(3);
            emojiRow.setStyle("-fx-background-color: transparent");

            for (int j = 0; j < 7; j++) {
                String emojiCode = emojiHexCodes.get((i * 7) + j);
                ImageView emoji = new ImageView(String.valueOf(getClass().getResource("/images/emojis/" + emojiHexCodes.get((i * 7) + j) + ".png")));
                emoji.setFitWidth(35);
                emoji.setFitHeight(35);

                emoji.setOnMouseEntered(event -> {
                    emoji.setStyle("-fx-background-color: gray; -fx-background-radius: 5");
                });

                emoji.setOnMouseExited(event -> {
                    emoji.setStyle("-fx-background-color: transparent;");
                });

                emoji.setOnMouseClicked(event -> {
                    sendEmoji(emojiCode);
                });

                emojiRow.getChildren().add(emoji);
            }
            emojiBox.getChildren().add(emojiRow);
        }

        Platform.runLater(() -> {
            stage.getScene().addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                Bounds bounds = emojiContainer.localToScene(emojiContainer.getBoundsInParent());
                double x = event.getSceneX();
                double y = event.getSceneY();
                if (!bounds.contains(x, y)) {
                    InboxView.getChildren().remove(emojiContainer);
                }
            });
        });

        InboxView.getChildren().add(emojiContainer);
    }

    public void resetChat() {
        if (client.getChatStatus()) {
            try {
                latch = new CountDownLatch(1);
                client.setLatch(latch);

                client.getServerOutput().writeObject("close_chat");
                client.getServerOutput().flush();

                latch.await();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        MessageContainer.getChildren().clear();
        lastSender = null;
        lastMessageDate = null;
    }

    public void searchUser(KeyEvent actionEvent) {
        String prefix = SearchBar.getText();

        if (prefix == null || prefix.isEmpty()) {
            displayUsers("###");
        }
        else {
            prefix = prefix.replaceAll("\\s+", "").toLowerCase();
            displayUsers(prefix);
        }
    }

    public void startChat(String id) {
        SearchBar.clear();
        displayUsers("###");

        resetChat();

        this.receiverId = id;

        // Sending chat command with recipient id to the server

        try {
            Thread.sleep(100);

            CountDownLatch latch = new CountDownLatch(1);
            client.setLatch(latch);

            client.getServerOutput().writeObject("chat_with:" + receiverId);
            client.getServerOutput().flush();

            latch.await();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (!receiverId.equals(client.getReceiverId())) {
            System.out.println("Unable to connect");
            return;
        }

        client.setChatStatus(true);

        Image chatBox = new Image(String.valueOf(getClass().getResource("/images/ChatBox.png")));
        ChatBox.setImage(chatBox);
        Message.setOpacity(1);
        Message.setDisable(false);
        
        for (ClientInfo clientInfo : client.getClients()) {
            if (clientInfo.getId().equals(receiverId)) {
                ReceiverName.setText(clientInfo.getFirstName() + " " + clientInfo.getLastName());
                receiverFirstName = clientInfo.getFirstName();
                receiverProfilePicture = new Image(new ByteArrayInputStream(clientInfo.getProfilePicture()));
            }
        }
        
        receiverProfilePictureView.setImage(receiverProfilePicture);
        Circle clip = new Circle(25, 25, 25);
        receiverProfilePictureView.setClip(clip);


        // Starting chat reader thread (Receives message from the chat server and shows it in the chat box)

        new Thread(() -> {
            while (true) {
                MessagePacket inMessage;

                try {
                    inMessage = (MessagePacket) client.getChatInput().readObject();
                } catch (IOException | ClassNotFoundException e) {
                    break;
                }

//                String sender = ((String) messageInfo).split(",")[0];
//                String timestamp = ((String) messageInfo).split(",")[1];
//                String message = ((String) messageInfo).substring(sender.length() + timestamp.length() + 2);

                String message = inMessage.getMessage();
                String sender = inMessage.getSender();
                String filename = inMessage.getFilename();
                byte[] fileBytes = inMessage.getFiledata();
                LocalDateTime datetime = inMessage.getDatatime();
                String dateFormat = "";
                if (datetime.getDayOfMonth() < 10) {
                    dateFormat = dateFormat + "0";
                }
                dateFormat = dateFormat + datetime.getDayOfMonth() + "/";
                if (datetime.getMonthValue() < 10) {
                    dateFormat = dateFormat + "0";
                }
                dateFormat = dateFormat + datetime.getMonthValue() + "/" + datetime.getYear();

                String timeFormat = "";
                if (datetime.getHour() < 10) {
                    timeFormat = timeFormat + "0";
                }
                timeFormat = timeFormat + datetime.getHour() + ":";
                if (datetime.getMinute() < 10) {
                    timeFormat = timeFormat + "0";
                }
                timeFormat = timeFormat + datetime.getMinute();

                String date = dateFormat;
                String time = timeFormat;

                if (filename != null) {
                    File chatMediaDirectory = new File("src/Client Local Repository/Chat Media");

                    if(!chatMediaDirectory.exists()) {
                        chatMediaDirectory.mkdir();
                    }

                    File mediaFile = new File(chatMediaDirectory, filename);

                    try {
                        FileOutputStream fos = new FileOutputStream(mediaFile);
                        fos.write(fileBytes);
                        fos.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                Platform.runLater(() -> {
                    if (!date.equals(lastMessageDate)) {
                        Label DateLabel = new Label();
                        DateLabel.setText(date);
                        DateLabel.setStyle("-fx-background-color: #c9d8f2; -fx-background-radius: 3; -fx-font-family: Open Sans; -fx-font-size: 12; -fx-text-fill: #000000; -fx-font-weight: bold; -fx-padding:3px");
                        DateLabel.setAlignment(Pos.CENTER);
                        HBox DateBox = new HBox(DateLabel);
                        DateBox.setAlignment(Pos.CENTER);
                        VBox.setMargin(DateBox, new Insets(20, 0, 15, 0));
                        MessageContainer.getChildren().add(DateBox);

                        lastMessageDate = date;
                    }
                    if (message != null) {
                        addMessage(message, sender, time);
                    }
                    else if (filename != null) {
                        addMedia(filename, sender, time);
                    }
                });
            }
        }).start();
    }

    void addMessage(String message, String sender, String time) {
        ImageView profilePicture = new ImageView(String.valueOf(getClass().getResource("/images/TransparentBackground.png")));

        profilePicture.setFitWidth(40);
        profilePicture.setFitHeight(40);
        profilePicture.setSmooth(true);
        Circle circularClip = new Circle(20, 20, 20);
        profilePicture.setClip(circularClip);

        Label senderLabel = new Label();
        senderLabel.setStyle("-fx-font-family: Open Sans; -fx-font-size: 12; -fx-text-fill: #000000;");
        HBox SenderBox = new HBox(senderLabel);

        Label MessageLabel = new Label(message);
        MessageLabel.setMaxWidth(450);
        MessageLabel.setPrefWidth(Region.USE_COMPUTED_SIZE);
        MessageLabel.setPrefHeight(Region.USE_COMPUTED_SIZE);
        MessageLabel.setWrapText(true);
        MessageLabel.setStyle("-fx-font-family: Open Sans; -fx-font-size: 18; -fx-text-fill: #ffffff; -fx-font-weight: bold;");

        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-font-family: Open Sans; -fx-font-size: 10; -fx-text-fill: #ffffff;");
        HBox timeBox = new HBox(timeLabel);
        timeBox.setAlignment(Pos.BASELINE_RIGHT);

        VBox MessageBox = new VBox(MessageLabel, timeBox);
        MessageBox.setSpacing(2);

        if (sender.equals(client.getId())) {
            if (!sender.equals(lastSender)) {
                MessageBox.setStyle("-fx-padding: 8; -fx-background-color: #386b7a; -fx-background-radius: 10 0 10 10;");
            }
            else {
                MessageBox.setStyle("-fx-padding: 8; -fx-background-color: #386b7a; -fx-background-radius: 10 10 10 10;");
            }
        }
        else {
            if (!sender.equals(lastSender)) {
                MessageBox.setStyle("-fx-padding: 8; -fx-background-color: #534c9c; -fx-background-radius: 0 10 10 10;");
            }
            else {
                MessageBox.setStyle("-fx-padding: 8; -fx-background-color: #534c9c; -fx-background-radius: 10 10 10 10;");
            }
        }

        VBox TextContainer;

        if (sender.equals(client.getId()) && !sender.equals(lastSender)) {
            profilePicture.setImage(client.getProfilePicture());
            senderLabel.setText(client.getFirstName());
            SenderBox.setAlignment(Pos.CENTER_RIGHT);
            TextContainer = new VBox(SenderBox, MessageBox);
            lastSender = client.getId();
        }
        else if (sender.equals(receiverId) && !sender.equals(lastSender)) {
            profilePicture.setImage(receiverProfilePicture);
            senderLabel.setText(receiverFirstName);
            SenderBox.setAlignment(Pos.CENTER_LEFT);
            TextContainer = new VBox(SenderBox, MessageBox);
            lastSender = receiverId;
        }
        else {
            TextContainer = new VBox(MessageBox);
        }

        TextContainer.setSpacing(5);

        HBox MessageBubble;

        if (sender.equals(client.getId())) {
            MessageBubble = new HBox(TextContainer, profilePicture);
        }
        else {
            MessageBubble = new HBox(profilePicture, TextContainer);
        }

        MessageBubble.setSpacing(10);

        if (sender.equals(client.getId())) {
            MessageContainer.setAlignment(Pos.CENTER_RIGHT);
            MessageBubble.setAlignment(Pos.CENTER_RIGHT);
        }
        else {
            MessageContainer.setAlignment(Pos.CENTER_LEFT);
            MessageBubble.setAlignment(Pos.CENTER_LEFT);
        }

        MessageContainer.getChildren().add(MessageBubble);

        Platform.runLater(() -> {
            MessageContainer.layout();
            MessageScroller.layout();
            Platform.runLater(() -> {
                MessageScroller.setVvalue(1.0);
            });
        });
    }

    void addMedia(String filename, String sender, String time) {
        String mediaType = FileTypeExtractor.extract(filename);

        ImageView profilePicture = new ImageView(String.valueOf(getClass().getResource("/images/TransparentBackground.png")));
        Label senderLabel = new Label();

        profilePicture.setFitWidth(40);
        profilePicture.setFitHeight(40);
        profilePicture.setSmooth(true);
        Circle circularClip = new Circle(20, 20, 20);
        profilePicture.setClip(circularClip);

        senderLabel.setStyle("-fx-font-family: Open Sans; -fx-font-size: 12; -fx-text-fill: #000000");
        HBox SenderBox = new HBox(senderLabel);

        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-font-family: Open Sans; -fx-font-size: 10; -fx-text-fill: #000000; -fx-padding: 8;");
        HBox timeBox = new HBox(timeLabel);
        timeBox.setAlignment(Pos.BASELINE_RIGHT);

        StackPane media = null;

        if (mediaType.equals("image")) {
            File mediaFile = new File("src/Client Local Repository/Chat Media", filename);
            Image img = new Image(mediaFile.toURI().toString());
            ImageView image = new ImageView(img);
            image.setPreserveRatio(true);
            VBox imageBox;

            if (filename.endsWith("emoji.png")) {
                image.setFitWidth(60);
                imageBox = new VBox(image, timeBox);
            }
            else {
                image.setFitWidth(300);
                double width = img.getWidth();
                double height = img.getHeight();
                Rectangle rectangularClip = new Rectangle(300, height * (300 / width));
                rectangularClip.setArcWidth(20);
                rectangularClip.setArcHeight(20);
                image.setClip(rectangularClip);
                Rectangle border = new Rectangle(303, height * (300 / width) + 3);
                border.setArcWidth(20);
                border.setArcHeight(20);
                imageBox = new VBox(new StackPane(border, image), timeBox);
            }

            media = new StackPane(imageBox);

            media.setOnMouseClicked(event -> {
                try {
                    Desktop.getDesktop().open(mediaFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        else if (mediaType.equals("audio")) {
            File mediaFile = new File("src/Client Local Repository/Chat Media", filename);
            Image img = new Image(String.valueOf(getClass().getResource("/images/Audio_Icon.png")));
            ImageView audioIcon = new ImageView(img);
            audioIcon.setFitWidth(100);
            audioIcon.setPreserveRatio(true);
            double width = img.getWidth();
            double height = img.getHeight();
            Label filenameLabel = new Label();
            filenameLabel.setText(filename);
            filenameLabel.setPrefWidth(100);
            filenameLabel.setPrefHeight(20);
            filenameLabel.setWrapText(true);
            filenameLabel.setAlignment(Pos.CENTER);
            filenameLabel.setStyle("-fx-font-family: Open Sans; -fx-font-size: 12; -fx-text-fill: #000000");

            VBox fileBox = new VBox(audioIcon, filenameLabel, timeBox);

            media = new StackPane(fileBox);

            media.setOnMouseClicked(event -> {
//                playVideo(img, filename);
                try {
                    Desktop.getDesktop().open(mediaFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        else if (mediaType.equals("video")) {
            File mediaFile = new File("src/Client Local Repository/Chat Media", filename);
            Image img = new Image(String.valueOf(getClass().getResource("/images/Video_Icon.png")));
            ImageView videoIcon = new ImageView(img);
            videoIcon.setFitWidth(100);
            videoIcon.setPreserveRatio(true);
            double width = img.getWidth();
            double height = img.getHeight();

            Label filenameLabel = new Label();
            filenameLabel.setText(filename);
            filenameLabel.setPrefWidth(100);
            filenameLabel.setPrefHeight(20);
            filenameLabel.setWrapText(true);
            filenameLabel.setAlignment(Pos.CENTER);
            filenameLabel.setStyle("-fx-font-family: Open Sans; -fx-font-size: 12; -fx-text-fill: #000000");


            VBox fileBox = new VBox(videoIcon, filenameLabel, timeBox);

            media = new StackPane(fileBox);

            media.setOnMouseClicked(event -> {
//                showImage(img, filename);
                try {
                    Desktop.getDesktop().open(mediaFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        else {
            File mediaFile = new File("src/Client Local Repository/Chat Media", filename);
            Image img = new Image(String.valueOf(getClass().getResource("/images/File_Icon.png")));
            ImageView fileIcon = new ImageView(img);
            fileIcon.setFitWidth(100);
            fileIcon.setPreserveRatio(true);
            double width = img.getWidth();
            double height = img.getHeight();
            Label filenameLabel = new Label();
            filenameLabel.setText(filename);
            filenameLabel.setPrefWidth(100);
            filenameLabel.setPrefHeight(20);
            filenameLabel.setWrapText(true);
            filenameLabel.setAlignment(Pos.CENTER);
            filenameLabel.setStyle("-fx-font-family: Open Sans; -fx-font-size: 12; -fx-text-fill: #000000");

            VBox fileBox = new VBox(fileIcon, filenameLabel, timeBox);
            
            media = new StackPane(fileBox);

            media.setOnMouseClicked(event -> {
//                showImage(img, filename);
                try {
                    Desktop.getDesktop().open(mediaFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        media.setMaxHeight(Region.USE_PREF_SIZE);
        media.setPrefHeight(Region.USE_COMPUTED_SIZE);

//        if (sender.equals(client.getId())) {
//            if (!sender.equals(lastSender)) {
//                MessageLabel.setStyle("-fx-font-family: Open Sans; -fx-font-size: 18; -fx-font-weight: bold; -fx-padding: 8; -fx-background-color: #75baff; -fx-background-radius: 20 0 20 20;");
//            }
//            else {
//                MessageLabel.setStyle("-fx-font-family: Open Sans; -fx-font-size: 18; -fx-font-weight: bold; -fx-padding: 8; -fx-background-color: #75baff; -fx-background-radius: 20 20 20 20;");
//            }
//        }
//        else {
//            if (!sender.equals(lastSender)) {
//                MessageLabel.setStyle("-fx-font-family: Open Sans; -fx-font-size: 18; -fx-font-weight: bold; -fx-padding: 8; -fx-background-color: #b6b9c0; -fx-background-radius: 0 20 20 20;");
//            }
//            else {
//                MessageLabel.setStyle("-fx-font-family: Open Sans; -fx-font-size: 18; -fx-font-weight: bold; -fx-padding: 8; -fx-background-color: #b6b9c0; -fx-background-radius: 20 20 20 20;");
//            }
//        }

        VBox MediaContainer = null;

        if (sender.equals(client.getId()) && !sender.equals(lastSender)) {
            profilePicture.setImage(client.getProfilePicture());
            senderLabel.setText(client.getFirstName());
            SenderBox.setAlignment(Pos.CENTER_RIGHT);
            MediaContainer = new VBox(SenderBox, media);
            lastSender = client.getId();
        }
        else if (sender.equals(receiverId) && !sender.equals(lastSender)) {
            profilePicture.setImage(receiverProfilePicture);
            senderLabel.setText(receiverFirstName);
            SenderBox.setAlignment(Pos.CENTER_LEFT);
            MediaContainer = new VBox(SenderBox, media);
            lastSender = receiverId;
        }
        else {
            MediaContainer = new VBox(media);
        }

        MediaContainer.setSpacing(5);

        HBox MessageBubble;

        if (sender.equals(client.getId())) {
            MessageBubble = new HBox(MediaContainer, profilePicture);
        }
        else {
            MessageBubble = new HBox(profilePicture, MediaContainer);
        }

        MessageBubble.setSpacing(10);

        if (sender.equals(client.getId())) {
            MessageContainer.setAlignment(Pos.CENTER_RIGHT);
            MessageBubble.setAlignment(Pos.TOP_RIGHT);
        }
        else {
            MessageContainer.setAlignment(Pos.CENTER_LEFT);
            MessageBubble.setAlignment(Pos.TOP_LEFT);
        }

        MessageContainer.getChildren().add(MessageBubble);

        Platform.runLater(() -> {
            MessageContainer.layout();
            MessageScroller.layout();
            Platform.runLater(() -> {
                MessageScroller.setVvalue(1.0);
            });
        });
    }

    public void displayUsers(String prefix) {
        Vector<ClientInfo> displayableUsers = new Vector<>();
        
        if (prefix.equals("###")) {
            displayableUsers = client.getClients();
        }
        else {
            for (ClientInfo clientInfo : client.getClients()) {
                String name = clientInfo.getFirstName().toLowerCase() + clientInfo.getLastName().toLowerCase();
                if (name.startsWith(prefix)) {
                    displayableUsers.add(clientInfo);
                }
            }
        }

        InboxContainer.getChildren().clear();

        char lastChar = '*';
        
        for (ClientInfo clientInfo : displayableUsers) {
            Image profilePicture = new Image(new ByteArrayInputStream(clientInfo.getProfilePicture()));
            ImageView profilePictureView = new ImageView(profilePicture);
            profilePictureView.setFitWidth(40);
            profilePictureView.setFitHeight(40);
            Circle clip = new Circle(20, 20, 20);
            profilePictureView.setClip(clip);
            Label usernameLabel = new Label(clientInfo.getFirstName() + " " + clientInfo.getLastName());
            usernameLabel.setStyle("-fx-font-family: Open Sans; -fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #000000");
            usernameLabel.setPrefHeight(40);
            HBox userInfo = new HBox(profilePictureView, usernameLabel);
            userInfo.setSpacing(15);
            userInfo.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 5px; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.08), 4, 0.2, 0, 2);");

            userInfo.setOnMouseEntered(event -> {
                userInfo.setStyle("-fx-background-color: #ebf3fa; -fx-background-radius: 10; -fx-padding: 5px; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.08), 4, 0.2, 0, 2);");
            });

            userInfo.setOnMouseExited(event -> {
                userInfo.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 5px; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.08), 4, 0.2, 0, 2);");
            });

            userInfo.setOnMouseClicked(event -> {
                startChat(clientInfo.getId());
            });

            Label firstLetterLabel = new Label(String.valueOf(clientInfo.getFirstName().charAt(0)));
            firstLetterLabel.setStyle("-fx-background-color: transparent; -fx-font-family: Open Sans; -fx-font-weight: bold; -fx-font-size: 18; -fx-text-fill: #000000; -fx-padding: 8px;");

            if (clientInfo.getFirstName().charAt(0) != lastChar) {
                InboxContainer.getChildren().add(firstLetterLabel);
                lastChar = clientInfo.getFirstName().charAt(0);
            }

            InboxContainer.getChildren().add(userInfo);
        }

        if (displayableUsers.isEmpty()) {
            noUserFoundLabel.setOpacity(1);
        }
        else {
            noUserFoundLabel.setOpacity(0);
        }
    }

    public void onSendButtonClicked(ActionEvent actionEvent) {
        sendMessage();
    }

    public void onEnterKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.ENTER)) {
            sendMessage();
        }
    }

    public void onAttachButtonClicked(ActionEvent actionEvent) {
        if (client.getChatStatus()) {
            filePath = FileExplorer.openFileExplorer(stage);
            System.out.println("File selected: " + filePath);
            Message.setText(filePath);
            Message.setEditable(false);
            Platform.runLater(() -> Message.requestFocus());
        }
    }

    public void onAudioCallButtonClicked(ActionEvent actionEvent) {
        if (client.getChatStatus()) {
            client.sendToServer("call:audio," + receiverId);

            if (!client.getReceiverIPAddress().equals("n/a")) {
                ClientInfo receiver = null;

                for (ClientInfo c : client.getClients()) {
                    if (c.getId().equals(receiverId)) {
                        receiver = c;
                        break;
                    }
                }

                new AudioVideoCall(client.getInfo(), receiver).startAudioCall(client.getReceiverIPAddress());
            }
            else {
                Platform.runLater(() -> {
                    System.out.println("User not active");

                    Rectangle background = new Rectangle(200, 80);
                    background.setArcWidth(20);
                    background.setArcHeight(20);
                    background.setFill(Color.web("#f4f4f4"));
                    background.setStroke(Color.BLACK);
                    background.setStrokeWidth(0);

                    Label message = new Label("User is not active.");
                    message.setPrefWidth(background.getWidth());
                    message.setPrefHeight(background.getHeight());
                    message.setStyle("-fx-background-color: transparent; -fx-font-family: 'Open Sans'; -fx-font-size: 18; -fx-text-fill: black; -fx-font-weight: bold;");
                    message.setAlignment(Pos.CENTER);

                    StackPane popup = new StackPane(background, message);
                    popup.setOpacity(0);
                    popup.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.15)));

                    double centerX = (Screen.SCREENWIDTH - background.getWidth()) / 2;
                    double centerY = (Screen.SCREENHEIGHT - background.getHeight()) / 2;
                    popup.setLayoutX(centerX);
                    popup.setLayoutY(centerY);

                    FadeTransition fadeIn = new FadeTransition(Duration.millis(200), popup);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();

                    PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
                    delay.setOnFinished(e -> {
                        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), popup);
                        fadeOut.setFromValue(1.0);
                        fadeOut.setToValue(0.0);
                        fadeOut.setOnFinished(ev -> InboxView.getChildren().remove(popup));
                        fadeOut.play();
                    });
                    delay.play();

                    InboxView.getChildren().add(popup);

                    stage.getScene().addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                        Bounds bounds = popup.localToScene(popup.getBoundsInParent());
                        if (!bounds.contains(event.getSceneX(), event.getSceneY())) {
                            InboxView.getChildren().remove(popup);
                        }
                    });
                });
            }
        }
    }

    public void onVideoCallButtonClicked(ActionEvent actionEvent) {
        if (client.getChatStatus()) {
            client.sendToServer("call:video," + receiverId);

            if (!client.getReceiverIPAddress().equals("n/a")) {
                ClientInfo receiver = null;

                for (ClientInfo c : client.getClients()) {
                    if (c.getId().equals(receiverId)) {
                        receiver = c;
                        break;
                    }
                }

                new AudioVideoCall(client.getInfo(), receiver).startVideoCall(client.getReceiverIPAddress());
            }
            else {
                Platform.runLater(() -> {
                    System.out.println("User not active");

                    Rectangle background = new Rectangle(200, 80);
                    background.setArcWidth(20);
                    background.setArcHeight(20);
                    background.setFill(Color.web("#f4f4f4"));
                    background.setStroke(Color.BLACK);
                    background.setStrokeWidth(0);

                    Label message = new Label("User is not active.");
                    message.setPrefWidth(background.getWidth());
                    message.setPrefHeight(background.getHeight());
                    message.setStyle("-fx-background-color: transparent; -fx-font-family: 'Open Sans'; -fx-font-size: 18; -fx-text-fill: black; -fx-font-weight: bold;");
                    message.setAlignment(Pos.CENTER);

                    StackPane popup = new StackPane(background, message);
                    popup.setOpacity(0);
                    popup.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.15)));

                    double centerX = (Screen.SCREENWIDTH - background.getWidth()) / 2;
                    double centerY = (Screen.SCREENHEIGHT - background.getHeight()) / 2;
                    popup.setLayoutX(centerX);
                    popup.setLayoutY(centerY);

                    FadeTransition fadeIn = new FadeTransition(Duration.millis(200), popup);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();

                    PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
                    delay.setOnFinished(e -> {
                        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), popup);
                        fadeOut.setFromValue(1.0);
                        fadeOut.setToValue(0.0);
                        fadeOut.setOnFinished(ev -> InboxView.getChildren().remove(popup));
                        fadeOut.play();
                    });
                    delay.play();

                    InboxView.getChildren().add(popup);

                    stage.getScene().addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                        Bounds bounds = popup.localToScene(popup.getBoundsInParent());
                        if (!bounds.contains(event.getSceneX(), event.getSceneY())) {
                            InboxView.getChildren().remove(popup);
                        }
                    });
                });
            }
        }
    }

    public void sendMessage() {
        if (client.getChatStatus()) {
            String message = null;
            String filename = null;
            byte[] fileBytes = null;

            if (filePath == null) {
                message = Message.getText().trim();
                Message.clear();
            }
            else {
                File file = new File(filePath);

                try {
                    filename = file.getName();
                    fileBytes = Files.readAllBytes(file.toPath());
                    System.out.println("read");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                filePath = null;
                Message.clear();
                Message.setEditable(true);
                Message.requestFocus();
            }

            if ((message == null || message.isEmpty()) && filename == null) {
                return;
            }

            try {
                client.getChatOutput().writeObject(new MessagePacket(client.getId(), receiverId, message, filename, fileBytes));
                client.getChatOutput().flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void sendEmoji(String emojiCode) {
        File emojiFile = new File("src/main/resources/images/emojis", emojiCode + ".png");
        byte[] emojiBytes;

        try {
            emojiBytes = Files.readAllBytes(emojiFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            client.getChatOutput().writeObject(new MessagePacket(client.getId(), receiverId, null, emojiCode + "emoji.png", emojiBytes));
            client.getChatOutput().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    public void showImage(Image image, String filename) {
//        Stage imageStage = new Stage();
//        ImageView imageView = new ImageView(image);
//        double width = image.getWidth();
//        double height = image.getHeight();
//        imageView.setFitHeight(Screen.SCREENHEIGHT * 0.75);
//        imageView.setPreserveRatio(true);
//        StackPane root = new StackPane(imageView);
//        Scene scene = new Scene(root, width * (imageView.getFitHeight() / height) + 30, imageView.getFitHeight() + 30);
//        Image icon = new Image(String.valueOf(getClass().getResource("/images/Payra.png")));
//        imageStage.getIcons().add(icon);
//        imageStage.setTitle(filename);
//        imageStage.setScene(scene);
//        imageStage.show();
//
//        imageStage.setOnCloseRequest(event ->{
//            imageStage.close();
//        });
//    }
}
