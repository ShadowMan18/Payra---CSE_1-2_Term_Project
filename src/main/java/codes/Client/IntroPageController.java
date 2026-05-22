package codes.Client;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.media.AudioClip;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class IntroPageController {
    @FXML
    public StackPane IntroPageLayout;
    @FXML
    public Group IntroPageView;
    @FXML
    public Text title;
    @FXML
    public Rectangle lightLine;

    private Client client;
    private Stage stage;
    private AudioClip typesound;
    private final String fullText = "Fly to connect";

    public void setIntroPageController(Client client, Stage stage) {
        this.client = client;
        this.stage = stage;

        IntroPageLayout.setPrefWidth(Screen.SCREENWIDTH);
        IntroPageLayout.setPrefHeight(Screen.SCREENHEIGHT);
        IntroPageView.scaleXProperty().bind(IntroPageLayout.widthProperty().divide(1600));
        IntroPageView.scaleYProperty().bind(IntroPageLayout.heightProperty().divide(900));

        typesound = new AudioClip(CallRinger.class.getResource("/sounds/TypewriterSound.mp3").toString());
        typesound.setCycleCount(AudioClip.INDEFINITE);
        typesound.play();

        title.setFont(Font.font("Hello Paris Sans Regular", 75));
        Timeline startDelay = new Timeline(new KeyFrame(Duration.millis(800), e -> playTypewriterEffect()));
        startDelay.play();
    }

    private void playTypewriterEffect() {
        Timeline typingTimeline = new Timeline();
        Duration delay = Duration.ZERO;
        Duration interval = Duration.millis(150);


        for (int i = -100; i < fullText.length(); i++) {
            if (i >= 0) {
                final int index = i;
                KeyFrame keyFrame = new KeyFrame(delay, event -> {
                    title.setText(fullText.substring(0, index + 1));
                });
                typingTimeline.getKeyFrames().add(keyFrame);
                delay = delay.add(interval);
            }
        }

        typingTimeline.setOnFinished(e -> {
            typesound.stop();

            Timeline afterTypingDelay = new Timeline(new KeyFrame(Duration.millis(1500), ev -> {
                try {
                    client.getLoginPage().startLoginPageView(client, stage);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }));
            afterTypingDelay.play();
        });

        typingTimeline.play();
    }

}
