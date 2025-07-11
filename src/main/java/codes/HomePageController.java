package codes;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class HomePageController {
    @FXML
    public StackPane HomePageLayout;
    @FXML
    public Group HomePageView;

    private Client client;
    private Stage stage;
    private CountDownLatch latch;

    public void setHomePageController(Client client, Stage stage) {
        this.client = client;
        this.stage = stage;
        HomePageLayout.setPrefWidth(Screen.SCREENWIDTH);
        HomePageLayout.setPrefHeight(Screen.SCREENHEIGHT);
        HomePageView.scaleXProperty().bind(HomePageLayout.widthProperty().divide(1600));
        HomePageView.scaleYProperty().bind(HomePageLayout.heightProperty().divide(900));
    }

    @FXML
    public void onNewsFeedButtonClick(ActionEvent actionEvent) {
        // Loading news feed page

        try {
            latch = new CountDownLatch(1);
            client.setLatch(latch);

            client.getServerOutput().writeObject("NewsFeed: open");
            client.getServerOutput().flush();

            latch.await();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

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
}
