package codes;

public class FileTypeExtractor {
    public String extract(String filename) {
        if (filename.toLowerCase().endsWith(".png")  || filename.toLowerCase().endsWith(".jpg")  || filename.toLowerCase().endsWith(".jpeg") || filename.toLowerCase().endsWith(".gif")  || filename.toLowerCase().endsWith(".bmp")  || filename.toLowerCase().endsWith(".wbmp")) {
            return "image";
        }
        else if (filename.toLowerCase().endsWith(".mp4")) {
            return "video";
        }
        else {
            return "other";
        }
    }
}
