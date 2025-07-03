package codes;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.*;

// Main for Shanon

public class Main {
    public static void main(String[] args) {
        BufferedReader reader;

        {
            try {
                reader = new BufferedReader(new FileReader("database/clients/Hello.txt"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter("database/clients/Hello.txt", true))){
                writer.write("Hello\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String s;

            while (true) {
                try {
                    s = reader.readLine();
                    if (s == null) break;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                System.out.println(s);
            }

            System.out.println(s);
        }
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

