package codes;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;

public class InboxController {
    @FXML
    public StackPane InboxLayout;
    @FXML
    public Group InboxView;
    @FXML
    public Label User;
    @FXML
    public TextField SearchBar;
    @FXML
    public ImageView ChatBox;
    @FXML
    public ImageView ReceiverProfilePicture;
    @FXML
    public Label ReceiverName;
    @FXML
    public Circle ReceiverProfilePictureBackground;
    @FXML
    public TextField Message;
    @FXML
    public ScrollPane MessageScroller;
    @FXML
    public VBox MessageContainer;


    private Client client;
    private Stage stage;
    private String receiverId;
    private Image receiverProfilePicture;
    private String lastSender;
    private String lastMessageDate;
    private String filePath;


    public void setInboxController(Client client, Stage stage) {
        this.client = client;
        this.stage = stage;
        client.setChatStatus(false);
        ReceiverProfilePictureBackground.setOpacity(0);
        Message.setOpacity(0);
        Message.setDisable(true);
        InboxLayout.setPrefWidth(Screen.SCREENWIDTH);
        InboxLayout.setPrefHeight(Screen.SCREENHEIGHT);
        InboxView.scaleXProperty().bind(InboxLayout.widthProperty().divide(1600));
        InboxView.scaleYProperty().bind(InboxLayout.heightProperty().divide(900));
        MessageScroller.setFitToWidth(true);
        MessageContainer.setFillWidth(true);
        MessageContainer.setMinHeight(Region.USE_PREF_SIZE);
        MessageContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            MessageScroller.setVvalue(1.0);
        });
    }

    @FXML
    public void onNewsFeedButtonClicked(ActionEvent mouseEvent) throws IOException {
        // Loading news feed page

        client.getServerOutput().writeObject("close_chat");
        client.getServerOutput().flush();

        client.getNewsFeed().startNewsFeedView(client, stage);
    }

    @FXML
    public void onHomeButtonClicked(ActionEvent mouseEvent) throws IOException {
        // Loading home page

        client.getServerOutput().writeObject("close_chat");
        client.getServerOutput().flush();

        client.getHomePage().startHomePageView(client, stage);
    }

    @FXML
    public void onNotificationButtonClick(ActionEvent actionEvent) throws IOException {
        // Loading notification page

        client.getServerOutput().writeObject("close_chat");
        client.getServerOutput().flush();

        client.getNotificationPage().startNotificationPageView(client, stage);
    }

    public void onProfileButtonClick(ActionEvent actionEvent) throws IOException {
        // Loading profile page

        client.getProfilePage().startProfilePageView(client, stage);
    }

    public void onSearchButtonClicked(ActionEvent actionEvent) {
        // Taking recipient id from the input field

        receiverId = SearchBar.getText().trim();
        receiverId = receiverId.substring(0, receiverId.length() - "@gmail.com".length());

        SearchBar.clear();

        // Sending chat command with recipient id to the server

        try {
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
        ReceiverProfilePictureBackground.setOpacity(1);
        Message.setOpacity(1);
        Message.setDisable(false);

        ReceiverName.setText(client.getReceiverName());

        receiverProfilePicture = new Image(String.valueOf(getClass().getResource("/images/DefaultprofilePicture.png")));
        ReceiverProfilePicture.setImage(receiverProfilePicture);
        Circle clip = new Circle(25, 25, 25);
        ReceiverProfilePicture.setClip(clip);


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
                    File mediaFile = new File("src/Client Local Repository/ChatMedia", filename);

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
                        DateLabel.setStyle("-fx-background-color: #c9d8f2; -fx-background-radius: 3; -fx-font-family: Open Sans; -fx-font-size: 12; -fx-text-fill: #000000; -fx-font-weight: bold");
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
            senderLabel.setText(client.getReceiverFirstName());
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

        senderLabel.setStyle("-fx-font-family: Arial Rounded MT Bold; -fx-font-size: 12; -fx-text-fill: #000000");
        HBox SenderBox = new HBox(senderLabel);

        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-font-family: Open Sans; -fx-font-size: 10; -fx-text-fill: #000000; -fx-padding: 8;");
        HBox timeBox = new HBox(timeLabel);
        timeBox.setAlignment(Pos.BASELINE_RIGHT);

        StackPane media = null;

        if (mediaType.equals("image")) {
            File mediaFile = new File("src/Client Local Repository/ChatMedia", filename);
            Image img = new Image(mediaFile.toURI().toString());
            ImageView image = new ImageView(img);
            image.setFitWidth(300);
            image.setPreserveRatio(true);
            double width = img.getWidth();
            double height = img.getHeight();
            Rectangle rectangularClip = new Rectangle(300, height * (300 / width));
            rectangularClip.setArcWidth(20);
            rectangularClip.setArcHeight(20);
            image.setClip(rectangularClip);
            Rectangle border = new Rectangle(303, height * (300 / width) + 3);
            border.setArcWidth(20);
            border.setArcHeight(20);

            VBox imageBox = new VBox(new StackPane(border, image), timeBox);

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
            File mediaFile = new File("src/Client Local Repository/ChatMedia", filename);
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
            filenameLabel.setStyle("-fx-font-family: Arial Rounded MT Bold; -fx-font-size: 12; -fx-text-fill: #000000");

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
            File mediaFile = new File("src/Client Local Repository/ChatMedia", filename);
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
            filenameLabel.setStyle("-fx-font-family: Arial Rounded MT Bold; -fx-font-size: 12; -fx-text-fill: #000000");


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
            File mediaFile = new File("src/Client Local Repository/ChatMedia", filename);
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
            filenameLabel.setStyle("-fx-font-family: Arial Rounded MT Bold; -fx-font-size: 12; -fx-text-fill: #000000");

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
//                MessageLabel.setStyle("-fx-font-family: Arial Rounded MT Bold; -fx-font-size: 18; -fx-font-weight: bold; -fx-padding: 8; -fx-background-color: #75baff; -fx-background-radius: 20 0 20 20;");
//            }
//            else {
//                MessageLabel.setStyle("-fx-font-family: Arial Rounded MT Bold; -fx-font-size: 18; -fx-font-weight: bold; -fx-padding: 8; -fx-background-color: #75baff; -fx-background-radius: 20 20 20 20;");
//            }
//        }
//        else {
//            if (!sender.equals(lastSender)) {
//                MessageLabel.setStyle("-fx-font-family: Arial Rounded MT Bold; -fx-font-size: 18; -fx-font-weight: bold; -fx-padding: 8; -fx-background-color: #b6b9c0; -fx-background-radius: 0 20 20 20;");
//            }
//            else {
//                MessageLabel.setStyle("-fx-font-family: Arial Rounded MT Bold; -fx-font-size: 18; -fx-font-weight: bold; -fx-padding: 8; -fx-background-color: #b6b9c0; -fx-background-radius: 20 20 20 20;");
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
            senderLabel.setText(client.getReceiverFirstName());
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
        }
    }

    public void onAudioCallButtonClicked(ActionEvent actionEvent) {
        if (client.getChatStatus()) {

        }
    }

    public void onVideoCallButtonClicked(ActionEvent actionEvent) {
        if (client.getChatStatus()) {

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

    public void showImage(Image image, String filename) {
        Stage imageStage = new Stage();
        ImageView imageView = new ImageView(image);
        double width = image.getWidth();
        double height = image.getHeight();
        imageView.setFitHeight(Screen.SCREENHEIGHT * 0.75);
        imageView.setPreserveRatio(true);
        StackPane root = new StackPane(imageView);
        Scene scene = new Scene(root, width * (imageView.getFitHeight() / height) + 30, imageView.getFitHeight() + 30);
        Image icon = new Image(String.valueOf(getClass().getResource("/images/Payra.png")));
        imageStage.getIcons().add(icon);
        imageStage.setTitle(filename);
        imageStage.setScene(scene);
        imageStage.show();

        imageStage.setOnCloseRequest(event ->{
            imageStage.close();
        });
    }
}
