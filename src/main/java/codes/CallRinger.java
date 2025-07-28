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
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CallRinger {
    public static void startCallerEndRinger(Client client, ClientInfo caller, ClientInfo receiver, String callType, String receiverIPAddress) {
        Platform.runLater(() -> {
            Stage ringerStage = new Stage();
            double windowWidth = Screen.SCREENWIDTH * 0.5;
            double windowHeight = Screen.SCREENHEIGHT * 0.5;
            Label receiverName = new Label(receiver.getFirstName() + " " + receiver.getLastName());
            receiverName.setStyle("-fx-background-color: transparent; -fx-font-family: Open Sans; -fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
            Label ringingMessageLabel = new Label("Ringing...");
            ringingMessageLabel.setStyle("-fx-background-color: transparent; -fx-font-family: Open Sans; -fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
            VBox labelHolder = new VBox(receiverName, ringingMessageLabel);
            labelHolder.setSpacing(5);
            labelHolder.setAlignment(Pos.TOP_CENTER);

            ImageView callerImageView = new ImageView(new Image(new ByteArrayInputStream(receiver.getProfilePicture())));

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

            StackPane root = new StackPane(background, callerImageView, labelHolder, buttonContainer);

            background.widthProperty().bind(root.widthProperty());
            background.heightProperty().bind(root.heightProperty());

            StackPane.setAlignment(callerImageView, Pos.CENTER);
            StackPane.setMargin(callerImageView, new Insets(10));

            StackPane.setAlignment(labelHolder, Pos.TOP_CENTER);
            StackPane.setMargin(receiverName, new Insets(10));

            StackPane.setAlignment(buttonContainer, Pos.BOTTOM_CENTER);
            StackPane.setMargin(buttonContainer, new Insets(10));

            Scene scene = new Scene(root, windowWidth, windowHeight);

            if (callType.equals("audio")) {
                ringerStage.setTitle("Audio call");
            }
            else {
                ringerStage.setTitle("Video call");
            }
            Image icon = new Image(String.valueOf(CallRinger.class.getResource("/images/Payra.png")));
            ringerStage.getIcons().add(icon);
            ringerStage.setAlwaysOnTop(true);
            ringerStage.setResizable(false);
            ringerStage.setScene(scene);

            ringerStage.show();

            AudioClip calltone = new AudioClip(CallRinger.class.getResource("/sounds/Calltone.mp3").toString());
            calltone.setCycleCount(AudioClip.INDEFINITE);
            calltone.play();

            declineButton.setOnMouseClicked(event -> {
                calltone.stop();
                ringerStage.close();
            });

            new Thread(() -> {
                long startTime = System.currentTimeMillis();
                long timeout = 60000; // 60 seconds
                long pollInterval = 200; // check every 200 ms

                while (true) {
                    String status = client.getCallAcceptanceStatus();

                    if ("accepted".equals(status)) {
                        Platform.runLater(() -> {
                            calltone.stop();
                            ringerStage.close();
                            if ("audio".equals(callType)) {
                                new AudioVideoCall(client, receiver).startAudioCall(receiverIPAddress);
                            } else if ("video".equals(callType)) {
                                new AudioVideoCall(client, receiver).startVideoCall(receiverIPAddress);
                            }
                        });
                        break;
                    } else if ("declined".equals(status)) {
                        Platform.runLater(() -> {
                            root.getChildren().remove(buttonContainer);
                            ringingMessageLabel.setText("Call declined");
                            ringingMessageLabel.setTextFill(Color.web("#db0202"));
                            calltone.stop();
                        });
                        break;
                    } else if (System.currentTimeMillis() - startTime > timeout) {
                        Platform.runLater(() -> {
                            root.getChildren().remove(buttonContainer);
                            ringingMessageLabel.setText("No response");
                            ringingMessageLabel.setTextFill(Color.web("#db0202"));
                            calltone.stop();
                        });
                        break;
                    }

                    try {
                        Thread.sleep(pollInterval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }).start();
        });
    }

    public static void startReceiverEndRinger(Client client, ClientInfo caller, ClientInfo receiver, String callType, String callerIPAddress) {
        Platform.runLater(() -> {
            Stage ringerStage = new Stage();
            ImageView callerImageView = new ImageView(new Image(new ByteArrayInputStream(caller.getProfilePicture())));
            Label receiverName = new Label(caller.getFirstName() + " " + caller.getLastName());
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

            receiverName.setStyle("-fx-background-color: transparent; -fx-font-family: Open Sans; -fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

            StackPane root = new StackPane(background, callerImageView, receiverName, buttonContainer);

            background.widthProperty().bind(root.widthProperty());
            background.heightProperty().bind(root.heightProperty());

            StackPane.setAlignment(callerImageView, Pos.CENTER);
            StackPane.setMargin(callerImageView, new Insets(10));

            StackPane.setAlignment(receiverName, Pos.TOP_CENTER);
            StackPane.setMargin(receiverName, new Insets(10));

            StackPane.setAlignment(buttonContainer, Pos.BOTTOM_CENTER);
            StackPane.setMargin(buttonContainer, new Insets(10));

            Scene scene = new Scene(root, windowWidth, windowHeight);

            if (callType.equals("audio")) {
                ringerStage.setTitle("Audio call");
            }
            else {
                ringerStage.setTitle("Video call");
            }
            Image icon = new Image(String.valueOf(CallRinger.class.getResource("/images/Payra.png")));
            ringerStage.getIcons().add(icon);
            ringerStage.setAlwaysOnTop(true);
            ringerStage.setResizable(false);
            ringerStage.setScene(scene);

            ringerStage.show();

            AudioClip ringtone = new AudioClip(CallRinger.class.getResource("/sounds/Ringtone.mp3").toString());
            ringtone.setCycleCount(AudioClip.INDEFINITE);
            ringtone.play();

            acceptButton.setOnMouseClicked(event -> {
                if (callType.equals("audio")) {
                    new AudioVideoCall(client, caller).startAudioCall(callerIPAddress);
                }
                else if (callType.equals("video")) {
                    new AudioVideoCall(client, caller).startVideoCall(callerIPAddress);
                }

                ringtone.stop();
                ringerStage.close();

                try {
                    client.getServerOutput().writeObject("call_accepted:" + caller.getId());
                    client.getServerOutput().flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            declineButton.setOnMouseClicked(event -> {
                ringtone.stop();
                ringerStage.close();

                try {
                    client.getServerOutput().writeObject("call_declined:" + caller.getId());
                    client.getServerOutput().flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }
}
