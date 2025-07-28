package codes;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.control.ToggleButton;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;

public class HomePageController {
    @FXML
    public StackPane HomePageLayout;
    @FXML
    public Group HomePageView;
    @FXML
    public ImageView userProfilePictureView;

    private Client client;
    private Stage stage;
    private String filepath;


    public void setHomePageController(Client client, Stage stage) {
        this.client = client;
        this.stage = stage;
        HomePageLayout.setPrefWidth(Screen.SCREENWIDTH);
        HomePageLayout.setPrefHeight(Screen.SCREENHEIGHT);
        HomePageView.scaleXProperty().bind(HomePageLayout.widthProperty().divide(1600));
        HomePageView.scaleYProperty().bind(HomePageLayout.heightProperty().divide(900));

        Platform.runLater(() -> {
                userProfilePictureView.setImage(client.getProfilePicture());
                Circle clip = new Circle(35, 35, 35);
                userProfilePictureView.setClip(clip);
        });
    }

    @FXML
    public void onNewsFeedButtonClick(ActionEvent actionEvent) {
        // Loading news feed page
        try {
            client.getNewsFeed().startNewsFeedView(client, stage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void onChatButtonClick(ActionEvent actionEvent) throws IOException {
        // Loading inbox page

        client.getInbox().startInboxView(client, stage);
    }

    @FXML
    public void onProfileButtonClick(MouseEvent mouseEvent) {
        startProfileSettingsView();
    }

    private Pane profileSettingsContainer;

    private void startProfileSettingsView() {
        if (profileSettingsContainer != null && HomePageView.getChildren().contains(profileSettingsContainer)) {
            return;
        }

        Rectangle background = new Rectangle(350, 400);
        background.setArcWidth(28);
        background.setArcHeight(28);
        background.setFill(Color.WHITE);
        background.setStroke(Color.rgb(200, 200, 200));
        background.setStrokeWidth(1);
        background.setLayoutX(0);
        background.setLayoutY(0);

        VBox profileBox = new VBox();
        profileBox.setPrefSize(322, 380);
        profileBox.setStyle("-fx-background-color: transparent;");
        profileBox.setSpacing(12);
        profileBox.setPadding(new Insets(20));

        Button closeButton = new Button("×");
        closeButton.setPrefSize(30, 30);
        closeButton.setFocusTraversable(false);
        closeButton.setStyle("""
                -fx-font-size: 20px;
                -fx-font-weight: bold;
                -fx-background-color: transparent;
                -fx-text-fill: #888;
            """);

        closeButton.setOnMouseEntered(e -> closeButton.setStyle("""
                -fx-font-size: 20px;
                -fx-font-weight: bold;
                -fx-background-color: transparent;
                -fx-text-fill: #e74c3c;
            """));

        closeButton.setOnMouseExited(e -> closeButton.setStyle("""
                -fx-font-size: 20px;
                -fx-font-weight: bold;
                -fx-background-color: transparent;
                -fx-text-fill: #888;
            """));

        closeButton.setOnAction(e -> {
            HomePageView.getChildren().remove(profileSettingsContainer);
            profileSettingsContainer = null;
        });

        Label titleLabel = new Label("Profile Settings");
        titleLabel.setStyle("""
                -fx-font-family: Open Sans;
                -fx-font-size: 26px;
                -fx-text-fill: #2c3e50;
            """);

        HBox titleBox = new HBox(titleLabel);
        titleBox.setAlignment(Pos.CENTER);
        VBox.setMargin(titleBox, new Insets(10, 0, 20, 0));

        Label myProfileLabel = new Label("My profile");
        myProfileLabel.setPrefWidth(350);
        myProfileLabel.setStyle("""
            -fx-font-size: 20px;
            -fx-text-fill: #333;
            """);

        Label updateAccountLabel = new Label("Update account");
        updateAccountLabel.setPrefWidth(350);
        updateAccountLabel.setStyle("""
            -fx-font-size: 20px;
            -fx-text-fill: #333;
            """);

        Label notificationSoundLabel = new Label("Notification sound");
        notificationSoundLabel.setStyle("""
            -fx-font-size: 20px;
            -fx-text-fill: #333;
            """);

        Pane toggleSwitch = new Pane();
        double toggleWidth = 50;
        double toggleHeight = 15;

        toggleSwitch.setPrefSize(toggleWidth, toggleHeight);
        toggleSwitch.setStyle("-fx-background-color: #ccc; -fx-background-radius: 20;");
        toggleSwitch.setCursor(Cursor.HAND);

        double circleRadius = 12;
        Circle thumb = new Circle(circleRadius);
        thumb.setFill(Color.WHITE);
        thumb.setEffect(new DropShadow(2, Color.gray(0, 0.3)));

        thumb.setLayoutX(circleRadius);
        thumb.setLayoutY(15);

        toggleSwitch.getChildren().add(thumb);

        BooleanProperty toggled = new SimpleBooleanProperty(true);

        if (toggled.get()) {
            thumb.setLayoutX(toggleWidth - circleRadius);
            toggleSwitch.setStyle("-fx-background-color: #4cd964; -fx-background-radius: 20");
        } else {
            thumb.setLayoutX(circleRadius);
            toggleSwitch.setStyle("-fx-background-color: #ccc; -fx-background-radius: 20");
        }

        toggled.addListener((obs, oldVal, newVal) -> {
            double startX = newVal ? circleRadius : toggleWidth - circleRadius;
            double endX = newVal ? toggleWidth - circleRadius : circleRadius;

            Timeline slide = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(thumb.layoutXProperty(), startX)),
                    new KeyFrame(Duration.millis(200), new KeyValue(thumb.layoutXProperty(), endX))
            );
            slide.play();

            toggleSwitch.setStyle(newVal
                    ? "-fx-background-color: #4cd964; -fx-background-radius: 20"
                    : "-fx-background-color: #ccc; -fx-background-radius: 20");
        });


        toggleSwitch.setOnMouseClicked(e -> toggled.set(!toggled.get()));

        HBox notificationSoundBox = new HBox(15, notificationSoundLabel, toggleSwitch);
        notificationSoundBox.setAlignment(Pos.CENTER_LEFT);

        Label logoutLabel = new Label("Log out");
        logoutLabel.setPrefWidth(350);
        logoutLabel.setStyle("""
            -fx-font-size: 20px;
            -fx-text-fill: #db0202;
            """);

        Label deleteAccountLabel = new Label("Delete account");
        deleteAccountLabel.setPrefWidth(350);
        deleteAccountLabel.setStyle("""
            -fx-font-size: 20px;
            -fx-text-fill: #db0202;
            """);

        VBox.setMargin(deleteAccountLabel, new Insets(30, 0, 0, 0));

        profileBox.getChildren().addAll(
                titleBox,
                myProfileLabel,
                updateAccountLabel,
                notificationSoundBox,
                logoutLabel,
                deleteAccountLabel
        );

        AnchorPane popup = new AnchorPane(background, profileBox, closeButton);
        AnchorPane.setTopAnchor(closeButton, 10.0);
        AnchorPane.setRightAnchor(closeButton, 10.0);
        AnchorPane.setTopAnchor(profileBox, 10.0);
        AnchorPane.setLeftAnchor(profileBox, 14.0);

        profileSettingsContainer = new Pane(popup);
        profileSettingsContainer.setLayoutX(1210);
        profileSettingsContainer.setLayoutY(120);
        profileSettingsContainer.setOpacity(0);
        profileSettingsContainer.setEffect(new DropShadow(15, Color.rgb(0, 0, 0, 0.1)));

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), profileSettingsContainer);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        closeButton.setOnAction(e -> {
            HomePageView.getChildren().remove(profileSettingsContainer);
            profileSettingsContainer = null;
        });

        myProfileLabel.setOnMouseClicked(mouseEvent -> {
            HomePageView.getChildren().remove(profileSettingsContainer);
            profileSettingsContainer = null;
            startMyProfileView();
        });

        updateAccountLabel.setOnMouseClicked(mouseEvent -> {
            HomePageView.getChildren().remove(profileSettingsContainer);
            profileSettingsContainer = null;
            startUpdateAccountView();
        });

        logoutLabel.setOnMouseClicked(mouseEvent -> {
            try {
                HomePageView.getChildren().remove(profileSettingsContainer);
                profileSettingsContainer = null;
                client = new Client();
                client.getLoginPage().startLoginPageView(client, stage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        deleteAccountLabel.setOnMouseClicked(mouseEvent -> {

        });

        HomePageView.getChildren().add(profileSettingsContainer);
    }


    public void startMyProfileView() {
        Rectangle background = new Rectangle(350, 400);
        background.setArcWidth(28);
        background.setArcHeight(28);
        background.setFill(Color.WHITE);
        background.setStroke(Color.rgb(200, 200, 200));
        background.setStrokeWidth(1);
        background.setLayoutX(0);
        background.setLayoutY(0);

        VBox profileBox = new VBox();
        profileBox.setPrefSize(322, 380);
        profileBox.setStyle("-fx-background-color: transparent;");
        profileBox.setSpacing(12);
        profileBox.setPadding(new Insets(20));

        Button backButton = new Button("←");
        backButton.setPrefSize(30, 30);
        backButton.setFocusTraversable(false);
        backButton.setStyle("""
            -fx-font-family: Segoe UI Symbol;
            -fx-font-size: 20px;
            -fx-font-weight: bold;
            -fx-background-color: transparent;
            -fx-text-fill: #888;
        """);

        backButton.setOnMouseEntered(e -> backButton.setStyle("""
            -fx-font-size: 20px;
            -fx-font-weight: bold;
            -fx-background-color: transparent;
            -fx-text-fill: #e74c3c;
        """));

        backButton.setOnMouseExited(e -> backButton.setStyle("""
            -fx-font-size: 20px;
            -fx-font-weight: bold;
            -fx-background-color: transparent;
            -fx-text-fill: #888;
        """));

        backButton.setOnAction(e -> {
            HomePageView.getChildren().remove(profileSettingsContainer);
            profileSettingsContainer = null;
        });

        ImageView profilePictureView = new ImageView(client.getProfilePicture());
        profilePictureView.setFitWidth(100);
        profilePictureView.setFitHeight(100);
        Circle clip = new Circle(50, 50, 50);
        profilePictureView.setClip(clip);

        HBox profilePictureBox = new HBox(profilePictureView);
        profilePictureBox.setAlignment(Pos.CENTER);
        VBox.setMargin(profilePictureBox, new Insets(10, 0, 20, 0));

        Label firstNameLabel = new Label("Firstname: " + client.getFirstName());
        firstNameLabel.setPrefWidth(350);
        firstNameLabel.setStyle("""
                -fx-font-size: 20px;
                -fx-text-fill: #333;
                """);

        Label lastNameLabel = new Label("Lastname: " + client.getLastName());
        lastNameLabel.setPrefWidth(350);
        lastNameLabel.setStyle("""
                -fx-font-size: 20px;
                -fx-text-fill: #333;
                """);

        Label emailLabel = new Label("Email: " + client.getEmail());
        emailLabel.setPrefWidth(350);
        emailLabel.setWrapText(true);
        emailLabel.setStyle("""
                -fx-font-size: 20px;
                -fx-text-fill: #333;
                """);

        profileBox.getChildren().addAll(
                profilePictureBox,
                firstNameLabel,
                lastNameLabel,
                emailLabel
        );

        AnchorPane popup = new AnchorPane(background, profileBox, backButton);
        AnchorPane.setTopAnchor(backButton, 10.0);
        AnchorPane.setLeftAnchor(backButton, 10.0);
        AnchorPane.setTopAnchor(profileBox, 10.0);
        AnchorPane.setLeftAnchor(profileBox, 14.0);

        profileSettingsContainer = new Pane(popup);
        profileSettingsContainer.setLayoutX(1210);
        profileSettingsContainer.setLayoutY(120);
        profileSettingsContainer.setOpacity(0);
        profileSettingsContainer.setEffect(new DropShadow(15, Color.rgb(0, 0, 0, 0.1)));

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), profileSettingsContainer);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        backButton.setOnAction(e -> {
            HomePageView.getChildren().remove(profileSettingsContainer);
            profileSettingsContainer = null;
            startProfileSettingsView();
        });

        HomePageView.getChildren().add(profileSettingsContainer);
    }

    public void startUpdateAccountView() {
        Rectangle background = new Rectangle(350, 400);
        background.setArcWidth(28);
        background.setArcHeight(28);
        background.setFill(Color.WHITE);
        background.setStroke(Color.rgb(200, 200, 200));
        background.setStrokeWidth(1);
        background.setLayoutX(0);
        background.setLayoutY(0);

        VBox profileBox = new VBox();
        profileBox.setPrefSize(322, 380);
        profileBox.setStyle("-fx-background-color: transparent;");
        profileBox.setSpacing(12);
        profileBox.setPadding(new Insets(20));

        Button backButton = new Button("←");
        backButton.setPrefSize(30, 30);
        backButton.setFocusTraversable(false);
        backButton.setStyle("""
            -fx-font-family: Segoe UI Symbol;
            -fx-font-size: 20px;
            -fx-font-weight: bold;
            -fx-background-color: transparent;
            -fx-text-fill: #888;
        """);

        backButton.setOnMouseEntered(e -> backButton.setStyle("""
            -fx-font-size: 20px;
            -fx-font-weight: bold;
            -fx-background-color: transparent;
            -fx-text-fill: #e74c3c;
        """));

        backButton.setOnMouseExited(e -> backButton.setStyle("""
            -fx-font-size: 20px;
            -fx-font-weight: bold;
            -fx-background-color: transparent;
            -fx-text-fill: #888;
        """));

        backButton.setOnAction(e -> {
            HomePageView.getChildren().remove(profileSettingsContainer);
            profileSettingsContainer = null;
            startProfileSettingsView();
        });

        Label titleLabel = new Label("Update Account");
        titleLabel.setStyle("""
                -fx-font-family: Open Sans;
                -fx-font-size: 26px;
                -fx-text-fill: #2c3e50;
            """);

        HBox titleBox = new HBox(titleLabel);
        titleBox.setAlignment(Pos.CENTER);
        VBox.setMargin(titleBox, new Insets(10, 0, 20, 0));

        Label changeNameLabel = new Label("Change name");
        changeNameLabel.setPrefWidth(350);
        changeNameLabel.setStyle("""
            -fx-font-size: 20px;
            -fx-text-fill: #333;
            """);

        Label changeProfilePictureLabel = new Label("Change profile picture");
        changeProfilePictureLabel.setPrefWidth(350);
        changeProfilePictureLabel.setStyle("""
            -fx-font-size: 20px;
            -fx-text-fill: #333;
            """);

        Label changePasswordLabel = new Label("Change password");
        changePasswordLabel.setStyle("""
            -fx-font-size: 20px;
            -fx-text-fill: #333;
            """);

        profileBox.getChildren().addAll(
                titleBox,
                changeNameLabel,
                changeProfilePictureLabel,
                changePasswordLabel
        );

        AnchorPane popup = new AnchorPane(background, profileBox, backButton);
        AnchorPane.setTopAnchor(backButton, 10.0);
        AnchorPane.setLeftAnchor(backButton, 10.0);
        AnchorPane.setTopAnchor(profileBox, 10.0);
        AnchorPane.setLeftAnchor(profileBox, 14.0);

        profileSettingsContainer = new Pane(popup);
        profileSettingsContainer.setLayoutX(1210);
        profileSettingsContainer.setLayoutY(120);
        profileSettingsContainer.setOpacity(0);
        profileSettingsContainer.setEffect(new DropShadow(15, Color.rgb(0, 0, 0, 0.1)));

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), profileSettingsContainer);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        backButton.setOnAction(e -> {
            HomePageView.getChildren().remove(profileSettingsContainer);
            profileSettingsContainer = null;
            startProfileSettingsView();
        });

        changeNameLabel.setOnMouseClicked(mouseEvent -> {
            HomePageView.getChildren().remove(profileSettingsContainer);
            profileSettingsContainer = null;
            startChangeNameView();
        });

        changeProfilePictureLabel.setOnMouseClicked(mouseEvent -> {
            HomePageView.getChildren().remove(profileSettingsContainer);
            profileSettingsContainer = null;
            startChangeProfilePictureView();
        });

        changePasswordLabel.setOnMouseClicked(mouseEvent -> {
            HomePageView.getChildren().remove(profileSettingsContainer);
            profileSettingsContainer = null;
            startChangePasswordView();
        });

        HomePageView.getChildren().add(profileSettingsContainer);
    }

    public void startChangeNameView() {
        Rectangle background = new Rectangle(350, 400);
        background.setArcWidth(28);
        background.setArcHeight(28);
        background.setFill(Color.WHITE);
        background.setStroke(Color.rgb(200, 200, 200));
        background.setStrokeWidth(1);
        background.setLayoutX(0);
        background.setLayoutY(0);

        VBox profileBox = new VBox();
        profileBox.setPrefSize(322, 380);
        profileBox.setStyle("-fx-background-color: transparent;");
        profileBox.setSpacing(12);
        profileBox.setPadding(new Insets(20));

        Button backButton = new Button("←");
        backButton.setPrefSize(30, 30);
        backButton.setFocusTraversable(false);
        backButton.setStyle("-fx-font-family: Segoe UI Symbol; -fx-font-size: 20px; -fx-font-weight: bold; -fx-background-color: transparent; -fx-text-fill: #888;");

        backButton.setOnMouseEntered(e -> backButton.setStyle(" -fx-font-size: 20px; -fx-font-weight: bold; -fx-background-color: transparent; -fx-text-fill: #e74c3c;"));
        backButton.setOnMouseExited(e -> backButton.setStyle(" -fx-font-size: 20px; -fx-font-weight: bold; -fx-background-color: transparent; -fx-text-fill: #888;"));

        backButton.setOnAction(e -> {
            HomePageView.getChildren().remove(profileSettingsContainer);
            profileSettingsContainer = null;
            startUpdateAccountView();
        });

        Label firstNameLabel = new Label("Firstname:");
        firstNameLabel.setPrefWidth(350);
        firstNameLabel.setStyle(" -fx-font-size: 20px; -fx-text-fill: #333; -fx-font-family: Open Sans;");

        TextField firstNameField = new TextField();
        firstNameField.setPrefWidth(350);
        firstNameField.setStyle(" -fx-font-family: Open Sans; -fx-font-size: 18px; -fx-text-fill: #333; -fx-background-color: white; -fx-border-color: #0f2e4d; -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 8 14;");

        firstNameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            firstNameField.setStyle(" -fx-font-family: Open Sans; -fx-font-size: 18px; -fx-text-fill: #333; -fx-background-color: white; -fx-border-color: #0f2e4d; -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 8 14;");
        });

        Label firstNameFieldLabel = new Label();
        firstNameFieldLabel.setPrefWidth(350);
        firstNameFieldLabel.setStyle(" -fx-font-size: 15px; -fx-text-fill: red; -fx-font-weight: bold; -fx-font-style: italic; -fx-font-family: Open Sans;");

        Label lastNameLabel = new Label("Lastname:");
        lastNameLabel.setPrefWidth(350);
        lastNameLabel.setStyle(" -fx-font-size: 20px; -fx-text-fill: #333; -fx-font-family: Open Sans;");

        TextField lastNameField = new TextField();
        lastNameField.setPrefWidth(350);
        lastNameField.setStyle(" -fx-font-family: Open Sans; -fx-font-size: 18px; -fx-text-fill: #333; -fx-background-color: white; -fx-border-color: #0f2e4d; -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 8 14;");

        lastNameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            lastNameField.setStyle(" -fx-font-family: Open Sans; -fx-font-size: 18px; -fx-text-fill: #333; -fx-background-color: white; -fx-border-color: #0f2e4d; -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 8 14;");
        });

        Label lastNameFieldLabel = new Label();
        lastNameFieldLabel.setPrefWidth(350);
        lastNameFieldLabel.setStyle(" -fx-font-size: 15px; -fx-text-fill: red; -fx-font-weight: bold; -fx-font-style: italic; -fx-font-family: Open Sans;");

        Button submitButton = new Button("Submit");
        submitButton.setPrefWidth(120);
        submitButton.setPrefHeight(25);
        submitButton.setStyle(" -fx-background-color: #0f2e4d; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-family: Open Sans; -fx-background-radius: 12px; -fx-cursor: hand; -fx-border-color: transparent;");

        submitButton.setOnMouseEntered(e -> submitButton.setStyle(" -fx-background-color: #133a5e; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-family: 'Open Sans'; -fx-background-radius: 12px; -fx-cursor: hand; -fx-border-color: transparent;"));

        submitButton.setOnMouseExited(e -> submitButton.setStyle(" -fx-background-color: #0f2e4d; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-family: 'Open Sans'; -fx-background-radius: 12px; -fx-cursor: hand; -fx-border-color: transparent;"));

        HBox submitButtonBox = new HBox(submitButton);
        submitButtonBox.setAlignment(Pos.CENTER);

        profileBox.getChildren().addAll(
                firstNameLabel,
                firstNameField,
                firstNameFieldLabel,
                lastNameLabel,
                lastNameField,
                lastNameFieldLabel,
                submitButtonBox
        );

        VBox.setMargin(firstNameLabel, new Insets(20, 0, 0, 0));
        VBox.setMargin(submitButtonBox, new Insets(5, 0, 0, 0));

        AnchorPane popup = new AnchorPane(background, profileBox, backButton);
        AnchorPane.setTopAnchor(backButton, 10.0);
        AnchorPane.setLeftAnchor(backButton, 10.0);
        AnchorPane.setTopAnchor(profileBox, 10.0);
        AnchorPane.setLeftAnchor(profileBox, 14.0);

        profileSettingsContainer = new Pane(popup);
        profileSettingsContainer.setLayoutX(1210);
        profileSettingsContainer.setLayoutY(120);
        profileSettingsContainer.setOpacity(0);
        profileSettingsContainer.setEffect(new DropShadow(15, Color.rgb(0, 0, 0, 0.1)));

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), profileSettingsContainer);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        backButton.setOnAction(e -> {
            HomePageView.getChildren().remove(profileSettingsContainer);
            profileSettingsContainer = null;
            startUpdateAccountView();
        });

        submitButton.setOnAction(actionEvent -> {
            String firstName = firstNameField.getText();
            String lastName = lastNameField.getText();

            String verdict;
            boolean b1 = false;
            boolean b2 = false;

            if (firstName.isEmpty()) {
                firstNameFieldLabel.setText("This field can't be empty");
            }
            else {
                verdict = Validator.validateName(firstName);
                firstNameFieldLabel.setText(verdict);
                if (verdict.isEmpty()) {
                    b1 = true;
                }
                else {
                    b1 = false;
                }
            }

            if (lastName.isEmpty()) {
                lastNameFieldLabel.setText("This field can't be empty");
            }
            else {
                verdict = Validator.validateName(lastName);
                lastNameFieldLabel.setText(verdict);
                if (verdict.isEmpty()) {
                    b2 = true;
                }
                else {
                    b2 = false;
                }
            }

            if (b1 && b2) {
                firstNameField.clear();
                lastNameField.clear();
                client.setFirstName(firstName);
                client.setLastName(lastName);
                try {
                    client.getServerOutput().writeObject("update:name," + firstName + "," + lastName);
                    client.getServerOutput().flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        HomePageView.getChildren().add(profileSettingsContainer);
    }

    public void startChangeProfilePictureView() {
        Rectangle background = new Rectangle(350, 400);
        background.setArcWidth(28);
        background.setArcHeight(28);
        background.setFill(Color.WHITE);
        background.setStroke(Color.rgb(200, 200, 200));
        background.setStrokeWidth(1);
        background.setLayoutX(0);
        background.setLayoutY(0);

        VBox profileBox = new VBox();
        profileBox.setPrefSize(322, 380);
        profileBox.setStyle("-fx-background-color: transparent;");
        profileBox.setSpacing(12);
        profileBox.setPadding(new Insets(20));

        Button backButton = new Button("←");
        backButton.setPrefSize(30, 30);
        backButton.setFocusTraversable(false);
        backButton.setStyle("-fx-font-family: Segoe UI Symbol; -fx-font-size: 20px; -fx-font-weight: bold; -fx-background-color: transparent; -fx-text-fill: #888;");

        backButton.setOnMouseEntered(e -> backButton.setStyle(" -fx-font-size: 20px; -fx-font-weight: bold; -fx-background-color: transparent; -fx-text-fill: #e74c3c;"));
        backButton.setOnMouseExited(e -> backButton.setStyle(" -fx-font-size: 20px; -fx-font-weight: bold; -fx-background-color: transparent; -fx-text-fill: #888;"));

        backButton.setOnAction(e -> {
            HomePageView.getChildren().remove(profileSettingsContainer);
            profileSettingsContainer = null;
            startUpdateAccountView();
        });

        ImageView profilePictureView = new ImageView(new Image(String.valueOf(getClass().getResource("/images/DefaultProfilePicture.png"))));
        profilePictureView.setFitWidth(200);
        profilePictureView.setFitHeight(200);
        Circle clip = new Circle(100, 100, 100);
        profilePictureView.setClip(clip);

        Label uploadImageLabel = new Label();
        uploadImageLabel.setPrefWidth(350);
        uploadImageLabel.setWrapText(true);
        uploadImageLabel.setStyle(" -fx-font-size: 15px; -fx-text-fill: red; -fx-font-weight: bold; -fx-font-style: italic; -fx-font-family: Open Sans;");

        Button uploadButton = new Button("Upload");
        uploadButton.setPrefWidth(120);
        uploadButton.setPrefHeight(25);
        uploadButton.setStyle(" -fx-background-color: #0f2e4d; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-family: Open Sans; -fx-background-radius: 12px; -fx-cursor: hand; -fx-border-color: transparent;");

        uploadButton.setOnMouseEntered(e -> uploadButton.setStyle(" -fx-background-color: #133a5e; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-family: 'Open Sans'; -fx-background-radius: 12px; -fx-cursor: hand; -fx-border-color: transparent;"));

        uploadButton.setOnMouseExited(e -> uploadButton.setStyle(" -fx-background-color: #0f2e4d; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-family: 'Open Sans'; -fx-background-radius: 12px; -fx-cursor: hand; -fx-border-color: transparent;"));

        Button submitButton = new Button("Submit");
        submitButton.setPrefWidth(120);
        submitButton.setPrefHeight(25);
        submitButton.setStyle(" -fx-background-color: #0f2e4d; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-family: Open Sans; -fx-background-radius: 12px; -fx-cursor: hand; -fx-border-color: transparent;");

        submitButton.setOnMouseEntered(e -> submitButton.setStyle(" -fx-background-color: #133a5e; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-family: 'Open Sans'; -fx-background-radius: 12px; -fx-cursor: hand; -fx-border-color: transparent;"));

        submitButton.setOnMouseExited(e -> submitButton.setStyle(" -fx-background-color: #0f2e4d; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-family: 'Open Sans'; -fx-background-radius: 12px; -fx-cursor: hand; -fx-border-color: transparent;"));

        HBox buttonBox = new HBox(uploadButton, submitButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setSpacing(10);

        profileBox.getChildren().addAll(
                profilePictureView,
                uploadImageLabel,
                buttonBox
        );

        VBox.setMargin(profilePictureView, new Insets(20, 0, 0, 0));
        VBox.setMargin(buttonBox, new Insets(5, 0, 0, 0));

        AnchorPane popup = new AnchorPane(background, profileBox, backButton);
        AnchorPane.setTopAnchor(backButton, 10.0);
        AnchorPane.setLeftAnchor(backButton, 10.0);
        AnchorPane.setTopAnchor(profileBox, 10.0);
        AnchorPane.setLeftAnchor(profileBox, 14.0);

        profileSettingsContainer = new Pane(popup);
        profileSettingsContainer.setLayoutX(1210);
        profileSettingsContainer.setLayoutY(120);
        profileSettingsContainer.setOpacity(0);
        profileSettingsContainer.setEffect(new DropShadow(15, Color.rgb(0, 0, 0, 0.1)));

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), profileSettingsContainer);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        backButton.setOnAction(e -> {
            HomePageView.getChildren().remove(profileSettingsContainer);
            profileSettingsContainer = null;
            startUpdateAccountView();
        });

        uploadButton.setOnAction(actionEvent -> {
            String path = FileExplorer.openFileExplorer(stage);
            filepath = path;

            if (!(path.toLowerCase().endsWith(".png")  || path.toLowerCase().endsWith(".jpg")  || path.toLowerCase().endsWith(".jpeg") || path.toLowerCase().endsWith(".bmp"))) {
                uploadImageLabel.setText("Select a valid image file (png/jpg/jpeg/bmp)");
            }
            else  {
                uploadImageLabel.setText("");
                File image = new File(path);
                profilePictureView.setImage(new Image(image.toURI().toString()));
            }
        });

        submitButton.setOnAction(actionEvent -> {
            if (filepath == null) {
                uploadImageLabel.setText("Select a valid image file (png/jpg/jpeg/bmp)");
            }
            else {
                File image = new File(filepath);
                try {
                    byte[] imageBytes = Files.readAllBytes(image.toPath());
                    client.getServerOutput().writeObject(imageBytes);
                    client.getServerOutput().flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                client.setProfilePicture(new Image(image.toURI().toString()));
                userProfilePictureView.setImage(new Image(image.toURI().toString()));
                profilePictureView.setImage(new Image(String.valueOf(getClass().getResource("/images/DefaultProfilePicture.png"))));
                filepath = null;
                uploadImageLabel.setText("");
            }
        });

        HomePageView.getChildren().add(profileSettingsContainer);
    }

    public void startChangePasswordView() {
        Rectangle background = new Rectangle(350, 400);
        background.setArcWidth(28);
        background.setArcHeight(28);
        background.setFill(Color.WHITE);
        background.setStroke(Color.rgb(200, 200, 200));
        background.setStrokeWidth(1);
        background.setLayoutX(0);
        background.setLayoutY(0);

        VBox profileBox = new VBox();
        profileBox.setPrefSize(322, 380);
        profileBox.setStyle("-fx-background-color: transparent;");
        profileBox.setSpacing(12);
        profileBox.setPadding(new Insets(20));

        Button backButton = new Button("←");
        backButton.setPrefSize(30, 30);
        backButton.setFocusTraversable(false);
        backButton.setStyle("-fx-font-family: Segoe UI Symbol; -fx-font-size: 20px; -fx-font-weight: bold; -fx-background-color: transparent; -fx-text-fill: #888;");

        backButton.setOnMouseEntered(e -> backButton.setStyle(" -fx-font-size: 20px; -fx-font-weight: bold; -fx-background-color: transparent; -fx-text-fill: #e74c3c;"));
        backButton.setOnMouseExited(e -> backButton.setStyle(" -fx-font-size: 20px; -fx-font-weight: bold; -fx-background-color: transparent; -fx-text-fill: #888;"));

        backButton.setOnAction(e -> {
            HomePageView.getChildren().remove(profileSettingsContainer);
            profileSettingsContainer = null;
            startUpdateAccountView();
        });

        Label newPasswordLabel = new Label("New password:");
        newPasswordLabel.setPrefWidth(350);
        newPasswordLabel.setStyle(" -fx-font-size: 20px; -fx-text-fill: #333; -fx-font-family: Open Sans;");

        TextField newPasswordField = new TextField();
        newPasswordField.setPrefWidth(350);
        newPasswordField.setStyle(" -fx-font-family: Open Sans; -fx-font-size: 18px; -fx-text-fill: #333; -fx-background-color: white; -fx-border-color: #0f2e4d; -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 8 14;");

        newPasswordField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            newPasswordField.setStyle(" -fx-font-family: Open Sans; -fx-font-size: 18px; -fx-text-fill: #333; -fx-background-color: white; -fx-border-color: #0f2e4d; -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 8 14;");
        });

        Label newPasswordFieldLabel = new Label();
        newPasswordFieldLabel.setPrefWidth(350);
        newPasswordFieldLabel.setWrapText(true);
        newPasswordFieldLabel.setStyle(" -fx-font-size: 15px; -fx-text-fill: red; -fx-font-weight: bold; -fx-font-style: italic; -fx-font-family: Open Sans;");

        Label confirmPasswordLabel = new Label("Confirm new password:");
        confirmPasswordLabel.setPrefWidth(350);
        confirmPasswordLabel.setStyle(" -fx-font-size: 20px; -fx-text-fill: #333; -fx-font-family: Open Sans;");

        TextField confirmPasswordField = new TextField();
        confirmPasswordField.setPrefWidth(350);
        confirmPasswordField.setStyle(" -fx-font-family: Open Sans; -fx-font-size: 18px; -fx-text-fill: #333; -fx-background-color: white; -fx-border-color: #0f2e4d; -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 8 14;");

        confirmPasswordField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            confirmPasswordField.setStyle(" -fx-font-family: Open Sans; -fx-font-size: 18px; -fx-text-fill: #333; -fx-background-color: white; -fx-border-color: #0f2e4d; -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 8 14;");
        });

        Label confirmPasswordFieldLabel = new Label();
        confirmPasswordFieldLabel.setPrefWidth(350);
        confirmPasswordFieldLabel.setStyle(" -fx-font-size: 15px; -fx-text-fill: red; -fx-font-weight: bold; -fx-font-style: italic; -fx-font-family: Open Sans;");

        Button submitButton = new Button("Submit");
        submitButton.setPrefWidth(120);
        submitButton.setPrefHeight(25);
        submitButton.setStyle(" -fx-background-color: #0f2e4d; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-family: Open Sans; -fx-background-radius: 12px; -fx-cursor: hand; -fx-border-color: transparent;");

        submitButton.setOnMouseEntered(e -> submitButton.setStyle(" -fx-background-color: #133a5e; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-family: 'Open Sans'; -fx-background-radius: 12px; -fx-cursor: hand; -fx-border-color: transparent;"));

        submitButton.setOnMouseExited(e -> submitButton.setStyle(" -fx-background-color: #0f2e4d; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-family: 'Open Sans'; -fx-background-radius: 12px; -fx-cursor: hand; -fx-border-color: transparent;"));

        HBox submitButtonBox = new HBox(submitButton);
        submitButtonBox.setAlignment(Pos.CENTER);

        profileBox.getChildren().addAll(
                newPasswordLabel,
                newPasswordField,
                newPasswordFieldLabel,
                confirmPasswordLabel,
                confirmPasswordField,
                confirmPasswordFieldLabel,
                submitButtonBox
        );

        VBox.setMargin(newPasswordLabel, new Insets(20, 0, 0, 0));
        VBox.setMargin(submitButtonBox, new Insets(5, 0, 0, 0));

        AnchorPane popup = new AnchorPane(background, profileBox, backButton);
        AnchorPane.setTopAnchor(backButton, 10.0);
        AnchorPane.setLeftAnchor(backButton, 10.0);
        AnchorPane.setTopAnchor(profileBox, 10.0);
        AnchorPane.setLeftAnchor(profileBox, 14.0);

        profileSettingsContainer = new Pane(popup);
        profileSettingsContainer.setLayoutX(1210);
        profileSettingsContainer.setLayoutY(120);
        profileSettingsContainer.setOpacity(0);
        profileSettingsContainer.setEffect(new DropShadow(15, Color.rgb(0, 0, 0, 0.1)));

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), profileSettingsContainer);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        backButton.setOnAction(e -> {
            HomePageView.getChildren().remove(profileSettingsContainer);
            profileSettingsContainer = null;
            startUpdateAccountView();
        });

        submitButton.setOnAction(actionEvent -> {
            String newPassword = newPasswordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            String verdict;
            boolean b1 = false;
            boolean b2 = false;

            if (newPassword.isEmpty()) {
                newPasswordFieldLabel.setText("This field can't be empty");
            } 
            else {
                verdict = Validator.validatePassword(newPassword);
                newPasswordFieldLabel.setText(verdict);
                if (verdict.isEmpty()) {
                    b1 = true;
                } else {
                    b1 = false;
                }
            }

            if (confirmPassword.isEmpty()) {
                confirmPasswordFieldLabel.setText("This field can't be empty");
            } 
            else {
                if (!confirmPassword.equals(newPassword)) {
                    confirmPasswordFieldLabel.setText("Password doesn't match");
                    b2 = false;
                }
                else {
                    confirmPasswordFieldLabel.setText("");
                    b2 = true;
                }
            }

            if (b1 && b2) {
                newPasswordField.clear();
                confirmPasswordField.clear();
                client.setPassword(newPassword);
                try {
                    client.getServerOutput().writeObject("update:password," + newPassword);
                    client.getServerOutput().flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        HomePageView.getChildren().add(profileSettingsContainer);
    }
}
