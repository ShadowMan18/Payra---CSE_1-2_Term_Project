package codes.Client;

import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class NotificationViewer {
    public static void viewNotifications(Client client, StackPane stage) {
        Pane root = new Pane();

        Rectangle background = new Rectangle(350, 350);
        background.setArcWidth(28);
        background.setArcHeight(28);
        background.setFill(Color.web("#f4f4f4"));
        background.setStroke(Color.BLACK);
        background.setStrokeWidth(0);
        background.setLayoutX(99);
        background.setLayoutY(458);

        VBox contentBox = new VBox(3);
        contentBox.setPrefSize(321, 334);
        contentBox.setStyle("-fx-background-color: transparent;");

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setLayoutX(113);
        scrollPane.setLayoutY(473);
        scrollPane.setPrefSize(321, 334);
        scrollPane.setStyle("-fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        Pane boxContainer = new Pane(background, scrollPane);
        root.getChildren().add(boxContainer);

        root.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (!boxContainer.localToScene(boxContainer.getBoundsInLocal()).contains(event.getSceneX(), event.getSceneY())) {
                root.getChildren().remove(boxContainer);
            }
        });

        stage.getChildren().add(root);
    }
}
