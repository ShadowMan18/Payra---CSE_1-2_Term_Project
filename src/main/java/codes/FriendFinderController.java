package codes;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

            HBox row = new HBox(10);
            row.setStyle("-fx-padding: 8; -fx-border-color: lightgray; -fx-border-radius: 5;");

            ImageView imageView = new ImageView(new Image(new ByteArrayInputStream(user.getProfilePicture())));
            imageView.setFitHeight(50);
            imageView.setFitWidth(50);
            imageView.setPreserveRatio(true);

            Label nameLabel = new Label(fullName + " (" + user.getId() + ")");
            nameLabel.setStyle("-fx-font-size: 14px;");

            Button statusButton = new Button("...");
            String status = client.getCachedFriendStatus(user.getId());

            if ("send".equals(status)) {
                statusButton.setText("Send Request");
                statusButton.setOnAction(e -> {
                    client.sendFriendRequest(user.getId());
                    statusButton.setText("Pending");
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
