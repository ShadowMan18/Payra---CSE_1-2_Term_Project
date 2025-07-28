package codes;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.File;

public class ClientWindow extends Application{
    static Client client;

    @Override
    public void start(Stage stage) throws Exception {
        client.setStage(stage);
        client.getLoginPage().startLoginPageView(client, stage);
    }

    public static void main(String[] args) {
        client = new Client();

        File chatMediaDirectory = new File("src/Client Local Repository/Chat Media");

        if(!chatMediaDirectory.exists()) {
            chatMediaDirectory.mkdir();
        }

        File feedMediaDirectory = new File("src/Client Local Repository/Feed Media");

        if(!feedMediaDirectory.exists()) {
            feedMediaDirectory.mkdir();
        }

        launch();
    }
}
