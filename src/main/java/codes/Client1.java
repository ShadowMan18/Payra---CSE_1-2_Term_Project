package codes;

import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class Client1 extends Application{
    static Client client;

    @Override
    public void start(Stage stage) throws Exception {
        client.getLoginPage().startLoginPageView(client, stage);

    }

    public static void main(String[] args) {
        client = new Client();
        launch();
    }
}