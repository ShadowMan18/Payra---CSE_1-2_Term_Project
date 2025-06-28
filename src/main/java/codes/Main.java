package codes;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

// Main for Shanon

public class Main extends Application {
    public static Stage stage;

    @Override
    public void start(Stage stage) throws IOException {
        this.stage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("HomePage.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), Screen.SCREENWIDTH, Screen.SCREENHEIGHT);
        stage.setTitle("Main");
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args) {
        launch();
    }
}

// Main for Arana

//public class Main extends Application {
//    public static final int Screen.SCREENWIDTH = Screen.getWidth() - 5;
//    public static final int Screen.SCREENHEIGHT = Screen.getHeight() - 35;
//    public static Stage stage;
//
//    @Override
//    public void start(Stage stage) throws IOException {
//        this.stage = stage;
//        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("HomePage.fxml"));
//        Scene scene = new Scene(fxmlLoader.load(), Screen.SCREENWIDTH, Screen.SCREENHEIGHT);
//        stage.setTitle("Main");
//        stage.setScene(scene);
//        stage.show();
//    }
//    public static void main(String[] args) {
//        launch();
//    }
//}

