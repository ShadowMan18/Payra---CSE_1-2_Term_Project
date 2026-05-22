package codes.Client;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

public class ImageCropper {
    public static Image cropImageToSquare(Image image) {
        double width = image.getWidth();
        double height = image.getHeight();

        double side = Math.min(width, height);

        double startX = (width - side) / 2;
        double startY = (height - side) /2;

        PixelReader pixelReader = image.getPixelReader();

        return new WritableImage(pixelReader, (int) startX, (int) startY, (int) side, (int) side);
    }
}
