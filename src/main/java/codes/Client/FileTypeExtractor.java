package codes.Client;

public class FileTypeExtractor {
    public static String extract(String filename) {
        if (filename.toLowerCase().endsWith(".png")  || filename.toLowerCase().endsWith(".jpg")  || filename.toLowerCase().endsWith(".jpeg") || filename.toLowerCase().endsWith(".gif")  || filename.toLowerCase().endsWith(".bmp")  || filename.toLowerCase().endsWith(".wbmp")) {
            return "image";
        }
        else if (filename.toLowerCase().endsWith(".mp3") || filename.toLowerCase().endsWith(".wav") || filename.toLowerCase().endsWith(".ogg") || filename.toLowerCase().endsWith(".m4a") || filename.toLowerCase().endsWith(".flac") || filename.toLowerCase().endsWith(".aac")) {
            return "audio";
        }
        else if (filename.toLowerCase().endsWith(".mp4")) {
            return "video";
        }
        else {
            return "other";
        }
    }
}
