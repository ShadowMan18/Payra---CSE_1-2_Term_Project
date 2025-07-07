package codes;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class FormattedTimeExample {
    public static String getTime() {
        LocalDateTime now = LocalDateTime.now();
        String formatted = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return formatted;
    }
}


public class PostController {

    @FXML
    private TextArea postContent;

    private Client client;

    public void setClient(Client client) {
        this.client = client;
    }

    @FXML
    public void onPostClick() {
        String content = postContent.getText().trim();
        if (content.isEmpty() || client == null || client.getEmail() == null) return;


        String email = client.getEmail();
        String username = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;

        File folder = new File("database/clients/"+username+"/posts");
        if (!folder.exists()) folder.mkdirs();

        File[] files = folder.listFiles((dir, name) -> name.matches("post-\\d+\\.txt"));
        Set<Integer> existingNumbers = new HashSet<>();
        if (files != null) {
            for (File f : files) {
                String name = f.getName();
                int dashIndex = name.indexOf('-');
                int dotIndex = name.indexOf('.');
                try {
                    int number = Integer.parseInt(name.substring(dashIndex + 1, dotIndex));
                    existingNumbers.add(number);
                } catch (NumberFormatException ignored) {}
            }
        }

        int postNum = 0;
        while (existingNumbers.contains(postNum)) {
            postNum++;
        }

        String filename = "post-" + postNum + ".txt";
        File postFile = new File(folder, filename);

        File folder2= new File("database");
        String filename2="GLobalPosts.txt";
        String globalContent=username+" "+FormattedTimeExample.getTime()+ " post: "+postNum+'\n'+content+'\n'+'\n';
        File GlobalpostFile = new File(folder2, filename2);


        try (BufferedWriter writer = new BufferedWriter(new FileWriter(postFile))) {
            writer.write(content + "\n");
            System.out.println("Post saved to " + postFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(GlobalpostFile, true))) {
            writer.write(globalContent);
            System.out.println("Global Post saved to " + GlobalpostFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        Stage stage = (Stage) postContent.getScene().getWindow();
        if (stage != null) stage.close();
    }
}
