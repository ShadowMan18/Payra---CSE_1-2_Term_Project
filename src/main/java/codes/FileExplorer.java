package codes;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;

public class FileExplorer {
    public static String openFileExplorer(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select file");

        // Set initial directory
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        // Setting extension filter
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Files", "*.*"));

        // Show open dialog
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            return selectedFile.getAbsolutePath();
        } else {
            return null;
        }

    }
}

