package codes.Client;

import javafx.animation.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class AboutUsPageController {
    @FXML
    public StackPane AboutUsPageLayout;
    @FXML
    public Group AboutUsPageView;
    @FXML
    public Label description;
    @FXML
    public Label developerLabel;
    @FXML
    public Label supervisorLabel;
    @FXML
    public StackPane developer1Pane;
    @FXML
    public StackPane developer2Pane;
    @FXML
    public StackPane supervisorPane;
    @FXML
    public ImageView dev1Image;
    @FXML
    public ImageView dev2Image;

    private Client client;
    private Stage stage;

    private final String fullText = "This app is developed as a term project for \"CSE 108: Object Oriented Programming Language Sessional\" course of level-1, term-2 using Java and JavaFX for core development and SQLite for database.";

    public void setAboutUsPageController(Client client, Stage stage) {
        this.client = client;
        this.stage = stage;

        AboutUsPageLayout.setPrefWidth(Screen.SCREENWIDTH);
        AboutUsPageLayout.setPrefHeight(Screen.SCREENHEIGHT);
        AboutUsPageView.scaleXProperty().bind(AboutUsPageLayout.widthProperty().divide(1600));
        AboutUsPageView.scaleYProperty().bind(AboutUsPageLayout.heightProperty().divide(900));

        clipCircular(dev1Image);
        clipCircular(dev2Image);

        developerLabel.setOpacity(0);
        developer1Pane.setOpacity(0);
        developer2Pane.setOpacity(0);
        supervisorPane.setOpacity(0);
        supervisorLabel.setOpacity(0);

        description.setFont(Font.font("Open Sans", 30));
        playTypewriterEffect();
    }

    private void clipCircular(ImageView imageView) {
        double radius = Math.min(imageView.getFitWidth(), imageView.getFitHeight()) / 2;
        Circle circle = new Circle(radius, radius, radius);
        imageView.setClip(circle);
    }

    public void onBackButtonClick(ActionEvent actionEvent) {
        try {
            client.getLoginPage().startLoginPageView(client, stage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void playTypewriterEffect() {
        Timeline typingTimeline = new Timeline();
        Duration delay = Duration.ZERO;
        Duration interval = Duration.millis(15);

        for (int i = -100; i < fullText.length(); i++) {
            if (i >= 0) {
                final int index = i;
                KeyFrame keyFrame = new KeyFrame(delay, event -> {
                    description.setText(fullText.substring(0, index + 1));
                });
                typingTimeline.getKeyFrames().add(keyFrame);
                delay = delay.add(interval);
            }
        }

        typingTimeline.setOnFinished(e -> {
            fadeIn(developerLabel);
            fadeIn(developer1Pane);
            fadeIn(developer2Pane);
            fadeIn(supervisorPane);
            fadeIn(supervisorLabel);
        });

        typingTimeline.play();
    }

    private void fadeIn(javafx.scene.Node node) {
        FadeTransition fade = new FadeTransition(Duration.millis(1000), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }
}
