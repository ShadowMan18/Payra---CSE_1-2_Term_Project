package codes;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;

public class CallRinger {
    public static void start(ClientInfo caller, ClientInfo receiver, String callType, String callerIPAddress) {
        Platform.runLater(() -> {
            Stage ringingStage = new Stage();
            ImageView callerImageView = new ImageView(new Image(new ByteArrayInputStream(caller.getProfilePicture())));
            Label callerName = new Label(caller.getFirstName() + " " + caller.getLastName());
            double windowWidth = Screen.SCREENWIDTH * 0.5;
            double windowHeight = Screen.SCREENHEIGHT * 0.5;

            callerImageView.setFitWidth(windowHeight * 0.5);
            callerImageView.setFitHeight(windowHeight * 0.5);
            callerImageView.setPreserveRatio(true);
            Circle clip1 = new Circle(windowHeight * 0.25, windowHeight * 0.25, windowHeight * 0.25);
            callerImageView.setClip(clip1);

            Rectangle background = new Rectangle();
            background.setFill(Color.web("#092038"));

            ImageView acceptButton = new ImageView(new Image(String.valueOf(AudioVideoCall.class.getResource("/images/Mute_Button.jpg"))));
            ImageView declineButton = new ImageView(new Image(String.valueOf(AudioVideoCall.class.getResource("/images/End_Call_Button.jpg"))));

            for (ImageView button : new ImageView[]{acceptButton, declineButton}) {
                button.setFitWidth(50);
                button.setFitHeight(50);
                Circle clip = new Circle(25, 25, 25);
                button.setClip(clip);
            }

            HBox buttonContainer = new HBox(acceptButton, declineButton);
            buttonContainer.setSpacing(15);
            buttonContainer.setPadding(new Insets(8));
            buttonContainer.setAlignment(Pos.CENTER);
            buttonContainer.setMaxWidth(Region.USE_PREF_SIZE);
            buttonContainer.setMaxHeight(66);

            buttonContainer.setBackground(new Background(new BackgroundFill(Color.web("#000000", 0.6), new CornerRadii(20), Insets.EMPTY)));

            callerName.setStyle("-fx-background-color: transparent; -fx-font-family: Open Sans; -fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

            StackPane root = new StackPane(background, callerImageView, callerName, buttonContainer);

            background.widthProperty().bind(root.widthProperty());
            background.heightProperty().bind(root.heightProperty());

            StackPane.setAlignment(callerImageView, Pos.CENTER);
            StackPane.setMargin(callerImageView, new Insets(10));

            StackPane.setAlignment(callerName, Pos.TOP_CENTER);
            StackPane.setMargin(callerName, new Insets(10));

            StackPane.setAlignment(buttonContainer, Pos.BOTTOM_CENTER);
            StackPane.setMargin(buttonContainer, new Insets(10));

            Scene scene = new Scene(root, windowWidth, windowHeight);

            Image icon = new Image(String.valueOf(CallRinger.class.getResource("/images/Payra.png")));
            ringingStage.getIcons().add(icon);
            ringingStage.setAlwaysOnTop(true);
            ringingStage.setResizable(false);
            ringingStage.setScene(scene);

            ringingStage.show();

            AudioClip ringtone = new AudioClip(CallRinger.class.getResource("/sounds/Ringtone.mp3").toString());
            ringtone.setCycleCount(AudioClip.INDEFINITE);
            ringtone.play();

            acceptButton.setOnMouseClicked(event -> {
                if (callType.equals("audio")) {
                    new AudioVideoCall(receiver, caller).startAudioCall(callerIPAddress);
                }
                else if (callType.equals("video")) {
                    new AudioVideoCall(receiver, caller).startVideoCall(callerIPAddress);
                }

                ringtone.stop();
                ringingStage.close();
            });

            declineButton.setOnMouseClicked(event -> {
                ringtone.stop();
                ringingStage.close();
            });
        });
    }
}
