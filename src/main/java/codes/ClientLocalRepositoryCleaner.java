package codes;

import java.io.File;

public class ClientLocalRepositoryCleaner {
    public static void clearChatMedia() {
        File chatMediaDirectory = new File("src/Client Local Repository/ChatMedia");
        File[] mediaFiles = chatMediaDirectory.listFiles();

        if (mediaFiles != null) {
            for (File file : mediaFiles) {
                file.delete();
            }
        }
    }

    public static void clearFeedMedia() {
        File feedMediaDirectory = new File("src/Client Local Repository/FeedMedia");
        File[] mediaFiles = feedMediaDirectory.listFiles();

        if (mediaFiles != null) {
            for (File file : mediaFiles) {
                file.delete();
            }
        }
    }
}
