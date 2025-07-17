package codes;

import java.io.*;
import java.net.URL;
import java.util.*;

public class EmojiDownloader {
    public static void main(String[] args) {
        List<String> emojiHexCodes = Arrays.asList(
                "1f600", "1f601", "1f602", "1f603", "1f604", "1f605", "1f606", "1f609", "1f60a", "1f60b",
                "1f60c", "1f60d", "1f60e", "1f60f", "1f610", "1f611", "1f612", "1f613", "1f614", "1f615",
                "1f616", "1f617", "1f618", "1f619", "1f61a", "1f61b", "1f61c", "1f61d", "1f61e", "1f61f",
                "1f620", "1f621", "1f622", "1f623", "1f624", "1f625", "1f626", "1f627", "1f628", "1f629",
                "1f62a", "1f62b", "1f62c", "1f62d", "1f62e", "1f62f", "1f630", "1f631", "1f632", "1f633",
                "1f634", "1f635", "1f636", "1f637", "1f638", "1f639", "1f63a", "1f63b", "1f63c", "1f63d",
                "1f63e", "1f63f", "1f640", "1f641", "1f642", "1f643", "1f644", "1f910", "1f911", "1f912",
                "1f913", "1f914", "1f915", "1f916", "1f917", "1f918", "1f919", "1f91a", "1f91b", "1f91c",
                "1f91d", "1f91e", "1f91f", "1f920", "1f921", "1f922", "1f923", "1f924", "1f925", "1f926",
                "1f927", "1f928", "1f929", "1f92a", "1f92b", "1f92c", "1f92d", "1f92e", "1f92f", "1f930"
        );

        String baseUrl = "https://raw.githubusercontent.com/googlefonts/noto-emoji/main/png/128/emoji_u";
        File outputDir = new File("src/main/resources/images/emojis");
        if (!outputDir.exists()) outputDir.mkdir();

        for (String code : emojiHexCodes) {
            String url = baseUrl + code + ".png";
            try (InputStream in = new URL(url).openStream();
                 OutputStream out = new FileOutputStream("src/main/resources/images/emojis/" + code + ".png")) {

                byte[] buffer = new byte[4096];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }

                System.out.println("✅ Downloaded: " + code + ".png");
            } catch (IOException e) {
                System.err.println("❌ Failed: " + code + " (" + e.getMessage() + ")");
            }
        }
    }
}
