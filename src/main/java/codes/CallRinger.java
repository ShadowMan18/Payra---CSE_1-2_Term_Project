package codes;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
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
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CallRinger {
    private static boolean isRinging;

    public static void startCallerEndRinger(Client client, ClientInfo caller, ClientInfo receiver, String callType, String receiverIPAddress) {
        isRinging = true;

        Platform.runLater(() -> {
            Stage ringerStage = new Stage();
            double windowWidth = Screen.SCREENWIDTH * 0.5;
            double windowHeight = Screen.SCREENHEIGHT * 0.5;
            Label receiverName = new Label(receiver.getFirstName() + " " + receiver.getLastName());
            receiverName.setStyle("-fx-background-color: transparent; -fx-font-family: Open Sans; -fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #0f2e4d;");
            Label ringingMessageLabel = new Label("Ringing...");
            ringingMessageLabel.setStyle("-fx-background-color: transparent; -fx-font-family: Open Sans; -fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #0f2e4d;");
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
            background.setFill(Color.web("#ffffff"));

            ImageView declineButton = new ImageView(new Image(String.valueOf(AudioVideoCall.class.getResource("/images/End_Call.png"))));

            for (ImageView button : new ImageView[]{declineButton}) {
                button.setFitWidth(50);
                button.setFitHeight(50);
                Circle clip = new Circle(25, 25, 25);
                button.setClip(clip);
            }

            HBox buttonContainer = new HBox(declineButton);
            buttonContainer.setSpacing(15);
            buttonContainer.setPadding(new Insets(8));
            buttonContainer.setAlignment(Pos.CENTER);
            buttonContainer.setMaxWidth(Region.USE_PREF_SIZE);
            buttonContainer.setMaxHeight(66);

            StackPane root = new StackPane(background, callerImageView, labelHolder, buttonContainer);

            background.widthProperty().bind(root.widthProperty());
            background.heightProperty().bind(root.heightProperty());

            StackPane.setAlignment(callerImageView, Pos.CENTER);
            StackPane.setMargin(callerImageView, new Insets(10));

            StackPane.setAlignment(labelHolder, Pos.TOP_CENTER);
            StackPane.setMargin(receiverName, new Insets(20));

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
                client.setCallStatus(false);

                calltone.stop();
                ringerStage.close();

                try {
                    client.getServerOutput().writeObject("call_declined:" + receiver.getId());
                    client.getServerOutput().flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            ringerStage.setOnCloseRequest(windowEvent -> {
                client.setCallStatus(false);

                calltone.stop();
                ringerStage.close();

                try {
                    client.getServerOutput().writeObject("call_declined:" + receiver.getId());
                    client.getServerOutput().flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            new Thread(() -> {
                long startTime = System.currentTimeMillis();
                long timeout = 60000;

                while (true) {
                    String status = client.getCallAcceptanceStatus();

                    if (status != null) {
                        if (status.equals("accepted")) {
                            client.setCallStatus(true);

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
                        } else if (status.equals("declined")) {
                            client.setCallStatus(false);

                            Platform.runLater(() -> {
                                root.getChildren().remove(buttonContainer);
                                ringingMessageLabel.setText("Call declined");
                                ringingMessageLabel.setTextFill(Color.web("#db0202"));
                                calltone.stop();
                                PauseTransition delay = new PauseTransition(Duration.seconds(3));
                                delay.setOnFinished(event -> ringerStage.close());
                                delay.play();
                            });
                            break;
                        }
                    }

                    if (System.currentTimeMillis() - startTime > timeout) {
                        client.setCallStatus(false);

                        Platform.runLater(() -> {
                            root.getChildren().remove(buttonContainer);
                            ringingMessageLabel.setText("No response");
                            ringingMessageLabel.setTextFill(Color.web("#db0202"));
                            calltone.stop();
                            PauseTransition delay = new PauseTransition(Duration.seconds(3));
                            delay.setOnFinished(event -> ringerStage.close());
                            delay.play();
                        });
                        break;
                    }

                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }).start();
        });
    }

    public static void startReceiverEndRinger(Client client, ClientInfo caller, ClientInfo receiver, String callType, String callerIPAddress) {
        isRinging = true;

        Platform.runLater(() -> {
            Stage ringerStage = new Stage();
            double windowWidth = Screen.SCREENWIDTH * 0.5;
            double windowHeight = Screen.SCREENHEIGHT * 0.5;
            Label receiverName = new Label(caller.getFirstName() + " " + caller.getLastName());
            receiverName.setStyle("-fx-background-color: transparent; -fx-font-family: Open Sans; -fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #0f2e4d;");
            Label ringingMessageLabel = new Label();
            ringingMessageLabel.setStyle("-fx-background-color: transparent; -fx-font-family: Open Sans; -fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #0f2e4d;");
            if (callType.equals("audio")) {
                ringingMessageLabel.setText("Started audio call...");
            }
            else {
                ringingMessageLabel.setText("Started video call...");
            }
            VBox labelHolder = new VBox(receiverName, ringingMessageLabel);
            labelHolder.setSpacing(5);
            labelHolder.setAlignment(Pos.TOP_CENTER);

            ImageView callerImageView = new ImageView(new Image(new ByteArrayInputStream(caller.getProfilePicture())));

            callerImageView.setFitWidth(windowHeight * 0.5);
            callerImageView.setFitHeight(windowHeight * 0.5);
            callerImageView.setPreserveRatio(true);
            Circle clip1 = new Circle(windowHeight * 0.25, windowHeight * 0.25, windowHeight * 0.25);
            callerImageView.setClip(clip1);

            Rectangle background = new Rectangle();
            background.setFill(Color.web("#ffffff"));

            ImageView acceptButton = new ImageView(new Image(String.valueOf(AudioVideoCall.class.getResource("/images/Call_Accept.png"))));
            ImageView declineButton = new ImageView(new Image(String.valueOf(AudioVideoCall.class.getResource("/images/Call_Reject.png"))));

            for (ImageView button : new ImageView[]{acceptButton, declineButton}) {
                button.setFitWidth(50);
                button.setFitHeight(50);
                Circle clip = new Circle(25, 25, 25);
                button.setClip(clip);
            }

            HBox buttonContainer = new HBox(acceptButton, declineButton);
            buttonContainer.setSpacing(30);
            buttonContainer.setPadding(new Insets(8));
            buttonContainer.setAlignment(Pos.CENTER);
            buttonContainer.setMaxWidth(Region.USE_PREF_SIZE);
            buttonContainer.setMaxHeight(66);

//            buttonContainer.setBackground(new Background(new BackgroundFill(Color.web("#e1ebf5"), new CornerRadii(20), Insets.EMPTY)));

            StackPane root = new StackPane(background, callerImageView, labelHolder, buttonContainer);

            background.widthProperty().bind(root.widthProperty());
            background.heightProperty().bind(root.heightProperty());

            StackPane.setAlignment(callerImageView, Pos.CENTER);
            StackPane.setMargin(callerImageView, new Insets(10));

            StackPane.setAlignment(labelHolder, Pos.TOP_CENTER);
            StackPane.setMargin(receiverName, new Insets(20));

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

            FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), scene.getRoot());
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();

            ringerStage.show();

            AudioClip ringtone = new AudioClip(CallRinger.class.getResource("/sounds/Ringtone.mp3").toString());
            ringtone.setCycleCount(AudioClip.INDEFINITE);
            ringtone.play();

            acceptButton.setOnMouseClicked(event -> {
                client.setCallStatus(true);

                if (callType.equals("audio")) {
                    new AudioVideoCall(client, caller).startAudioCall(callerIPAddress);
                }
                else if (callType.equals("video")) {
                    new AudioVideoCall(client, caller).startVideoCall(callerIPAddress);
                }

                ringtone.stop();
                ringerStage.close();
                isRinging = false;

                try {
                    client.getServerOutput().writeObject("call_accepted:" + caller.getId());
                    client.getServerOutput().flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            declineButton.setOnMouseClicked(event -> {
                client.setCallStatus(false);

                ringtone.stop();
                ringerStage.close();
                isRinging = false;

                try {
                    client.getServerOutput().writeObject("call_declined:" + caller.getId());
                    client.getServerOutput().flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            ringerStage.setOnCloseRequest(windowEvent -> {
                client.setCallStatus(false);

                ringtone.stop();
                ringerStage.close();
                isRinging = false;

                try {
                    client.getServerOutput().writeObject("call_declined:" + caller.getId());
                    client.getServerOutput().flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            new Thread(() -> {
                long startTime = System.currentTimeMillis();
                long timeout = 60000;

                while (isRinging) {
                    String status = client.getCallAcceptanceStatus();

                    if ((status != null && status.equals("declined")) || System.currentTimeMillis() - startTime > timeout) {
                        client.setCallStatus(false);

                        Platform.runLater(() -> {
                            ringtone.stop();
                            ringerStage.close();
                            isRinging = false;
                        });
                        break;
                    }

                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        });
    }
}
