package codes.Client;

import codes.Wrappers.ClientInfo;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;
import java.util.List;

public class FriendFinderController {

    @FXML private TextField searchField;
    @FXML private VBox userListVBox;

    private List<ClientInfo> allClients;
    private String myId;
    private Client client;

    public void setClient(Client client) {
        this.client = client;
        this.myId = client.getMyId();
        this.allClients = client.getAllClients();
        System.out.println("Total clients received: " + allClients.size());
        refreshUserList("");
    }

    @FXML
    private void onSearch() {
        String query = searchField.getText().trim().toLowerCase();
        refreshUserList(query);
    }

    private void refreshUserList(String query) {
        userListVBox.getChildren().clear();

        client.fetchFriendStatusMap();

        for (ClientInfo user : allClients) {
            if (user.getId().equals(myId)) continue;
            String fullName = user.getFirstName() + " " + user.getLastName();
            if (!fullName.toLowerCase().contains(query) && !user.getId().toLowerCase().contains(query)) {
                continue;
            }

            HBox row = new HBox(15);
            row.setStyle("""
                -fx-padding: 10;
                -fx-background-color: #fdfdfd;
                -fx-border-color: #e0e0e0;
                -fx-border-radius: 10;
                -fx-background-radius: 10;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 3, 0, 0, 2);
            """);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            ImageView imageView = new ImageView(new Image(new ByteArrayInputStream(user.getProfilePicture())));
            imageView.setFitHeight(50);
            imageView.setFitWidth(50);
            imageView.setPreserveRatio(true);
            imageView.setClip(new javafx.scene.shape.Circle(25, 25, 25));

            Label nameLabel = new Label(fullName); // removed ID display
            nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

            Button statusButton = new Button("...");
            statusButton.getStyleClass().add("friend-button");

            String status = client.getCachedFriendStatus(user.getId());

            if ("send".equals(status)) {
                statusButton.setText("Send Request");
                statusButton.setOnAction(e -> {
                    client.sendFriendRequest(user.getId());
                    statusButton.setText("Sent");
                    statusButton.setDisable(true);
                });
            }
            else if ("pending".equals(status)) {
                statusButton.setText("Pending");
                statusButton.setDisable(true);
            }
            else if("sent".equals(status)){
                statusButton.setText("Sent");
                statusButton.setDisable(true);
            }
            else if ("friends".equals(status)) {
                statusButton.setText("Friends");
                statusButton.setDisable(true);
            }

            row.getChildren().addAll(imageView, nameLabel, statusButton);
            userListVBox.getChildren().add(row);
        }
    }
}
