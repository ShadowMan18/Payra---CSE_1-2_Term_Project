package codes;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.*;

public class AudioVideoCall {
    private static Stage videoStage;
    private static ImageView videoView;
    private static ImageView myVideoView;
    private static double windowWidth = Screen.SCREENWIDTH * 0.7;
    private static double windowHeight = Screen.SCREENHEIGHT * 0.8;

    static {
        System.load("C:/Program Files/opencv/build/java/x64/opencv_java4120.dll");
    }

    public static void startAudioCall(String receiverIPAddress) {
        System.out.println(receiverIPAddress);
        // Initiating audio sender thread

        new Thread(() -> {
            try (DatagramSocket audioSocket = new DatagramSocket()) {
                InetAddress receiverAddress = InetAddress.getByName(receiverIPAddress);
                int port = 22222;

                AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
                microphone.open(format);
                microphone.start();

                byte[] buffer = new byte[4096];

                System.out.println("🎤 Audio capture started...");

                while (true) {
                    int bufferSize = microphone.read(buffer, 0, buffer.length);
                    DatagramPacket packet = new DatagramPacket(buffer, bufferSize, receiverAddress, port);
                    audioSocket.send(packet);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Initiating audio receiver thread

        new Thread(() -> {
            try {
                DatagramSocket audioSocket = new DatagramSocket(22222);

                AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(info);
                speakers.open(format);
                speakers.start();

                byte[] buffer = new byte[4096];

                System.out.println("🔊 Audio receiver started...");

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    audioSocket.receive(packet);
                    speakers.write(packet.getData(), 0, packet.getLength());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void startVideoCall(String receiverIPAddress) {
        // Start audio call

        startAudioCall(receiverIPAddress);

        // Initiating video preview

        Platform.runLater(() -> {
            videoView = new ImageView();
            videoView.setFitWidth(windowWidth);
            videoView.setFitHeight(windowHeight);
            videoView.setPreserveRatio(true);
            Rectangle clip1 = new Rectangle();
            clip1.setArcWidth(20);
            clip1.setArcHeight(20);
            videoView.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
                clip1.setWidth(newBounds.getWidth());
                clip1.setHeight(newBounds.getHeight());
            });
            videoView.setClip(clip1);

            myVideoView = new ImageView();
            myVideoView.setFitWidth(windowWidth * 0.2);
            myVideoView.setFitHeight(windowHeight * 0.2);
            myVideoView.setPreserveRatio(true);
            Rectangle clip2 = new Rectangle();
            clip2.setArcWidth(20);
            clip2.setArcHeight(20);
            myVideoView.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
                clip2.setWidth(newBounds.getWidth());
                clip2.setHeight(newBounds.getHeight());
            });
            myVideoView.setClip(clip2);

            Rectangle background = new Rectangle();
            background.setFill(Color.web("#092038"));

            ImageView endCallButton = new ImageView(new Image(String.valueOf(AudioVideoCall.class.getResource("/images/End_Call_Button.jpg"))));
            ImageView muteButton = new ImageView(new Image(String.valueOf(AudioVideoCall.class.getResource("/images/Mute_Button.jpg"))));
            ImageView stopVideoButton = new ImageView(new Image(String.valueOf(AudioVideoCall.class.getResource("/images/Video_Button.jpg"))));

            for (ImageView button : new ImageView[]{endCallButton, muteButton, stopVideoButton}) {
                button.setFitWidth(50);
                button.setFitHeight(50);
                Circle clip = new Circle(25, 25, 25);
                button.setClip(clip);
            }

            endCallButton.setOnMouseClicked(event -> {

            });

            muteButton.setOnMouseClicked(event -> {

            });

            stopVideoButton.setOnMouseClicked(event -> {

            });


            HBox buttonContainer = new HBox(stopVideoButton, muteButton, endCallButton);
            buttonContainer.setSpacing(15);
            buttonContainer.setPadding(new Insets(8));
            buttonContainer.setAlignment(Pos.CENTER);
            buttonContainer.setMaxWidth(Region.USE_PREF_SIZE);
            buttonContainer.setMaxHeight(66);

            buttonContainer.setBackground(new Background(new BackgroundFill(Color.web("#000000", 0.6), new CornerRadii(20), Insets.EMPTY)));

            StackPane root = new StackPane(background, videoView, myVideoView, buttonContainer);

            background.widthProperty().bind(root.widthProperty());
            background.heightProperty().bind(root.heightProperty());

            // Layout positions inside root StackPane
            StackPane.setAlignment(videoView, Pos.CENTER);
            StackPane.setMargin(videoView, new Insets(10));

            StackPane.setAlignment(myVideoView, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(myVideoView, new Insets(10));

            StackPane.setAlignment(buttonContainer, Pos.BOTTOM_CENTER);
            StackPane.setMargin(buttonContainer, new Insets(10));

            // Create scene and stage
            Scene scene = new Scene(root, windowWidth + 20, windowHeight);

            videoStage = new Stage();
            videoStage.setTitle("Video Call");
            Image icon = new Image(String.valueOf(AudioVideoCall.class.getResource("/images/Payra.png")));
            videoStage.getIcons().add(icon);
            videoStage.setAlwaysOnTop(true);
            videoStage.setResizable(false);
            videoStage.setScene(scene);

            videoStage.show();
        });





        // Initiating video sender thread

        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress receiverAddress = InetAddress.getByName(receiverIPAddress);
                int port = 22223;

                VideoCapture webcam = new VideoCapture(0, Videoio.CAP_DSHOW);
                webcam.set(Videoio.CAP_PROP_FRAME_WIDTH, windowWidth);
                webcam.set(Videoio.CAP_PROP_FRAME_HEIGHT, windowHeight);

                if (!webcam.isOpened()) {
                    System.out.println("❌ Cannot open webcam!");
                    return;
                }

                MatOfInt jpegParams = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 80);
                final int CHUNK_SIZE = 1400;

                while (true) {
                    Mat frame = new Mat();

                    if (webcam.read(frame) && !frame.empty()) {
                        Core.flip(frame, frame, 1);

                        MatOfByte buffer = new MatOfByte();
                        Imgcodecs.imencode(".jpg", frame, buffer, jpegParams);
                        byte[] frameBytes = buffer.toArray();

                        Image videoImage = new Image(new ByteArrayInputStream(frameBytes));

                        Platform.runLater(() -> {
                            if (myVideoView != null) {
                                myVideoView.setImage(videoImage);
                            }
                        });

                        int totalChunks = (int) Math.ceil((double) frameBytes.length / CHUNK_SIZE);
                        int frameId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);

                        for (int i = 0; i < totalChunks; i++) {
                            int start = i * CHUNK_SIZE;
                            int length = Math.min(CHUNK_SIZE, frameBytes.length - start);
                            byte[] chunkData = Arrays.copyOfRange(frameBytes, start, start + length);

                            ByteBuffer packetBuffer = ByteBuffer.allocate(8 + chunkData.length);
                            packetBuffer.putInt(frameId);
                            packetBuffer.putShort((short) i);
                            packetBuffer.putShort((short) totalChunks);
                            packetBuffer.put(chunkData);

                            DatagramPacket packet = new DatagramPacket(packetBuffer.array(), packetBuffer.capacity(), receiverAddress, port);
                            socket.send(packet);
                        }
                    }

                    // Optional delay or frame rate control can be added here
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Initiating video receiver thread

        new Thread(() -> {
            try (DatagramSocket receiverSocket = new DatagramSocket(22223)) {
                byte[] buffer = new byte[1500];
                Map<Integer, List<byte[]>> frameChunks = new HashMap<>();
                Map<Integer, Integer> chunkCounts = new HashMap<>();

                System.out.println("📥 Listening on port 22223...");

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    receiverSocket.receive(packet);

                    ByteBuffer byteBuffer = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
                    int frameId = byteBuffer.getInt();
                    int chunkIndex = byteBuffer.getShort() & 0xFFFF;
                    int totalChunks = byteBuffer.getShort() & 0xFFFF;

                    byte[] chunkData = new byte[packet.getLength() - 8];
                    byteBuffer.get(chunkData);

                    frameChunks.putIfAbsent(frameId, new ArrayList<>(Collections.nCopies(totalChunks, null)));
                    chunkCounts.putIfAbsent(frameId, 0);

                    List<byte[]> chunks = frameChunks.get(frameId);
                    if (chunks.get(chunkIndex) == null) {
                        chunks.set(chunkIndex, chunkData);
                        chunkCounts.put(frameId, chunkCounts.get(frameId) + 1);
                    }

                    if (chunkCounts.get(frameId).equals(totalChunks)) {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        for (byte[] chunk : chunks) outputStream.write(chunk);

                        byte[] fullFrame = outputStream.toByteArray();
                        Mat frame = Imgcodecs.imdecode(new MatOfByte(fullFrame), Imgcodecs.IMREAD_COLOR);

                        if (!frame.empty()) {
                            MatOfByte bufferMat = new MatOfByte();
                            Imgcodecs.imencode(".jpg", frame, bufferMat);
                            Image videoImage = new Image(new ByteArrayInputStream(bufferMat.toArray()));

                            Platform.runLater(() -> {
                                if (videoView != null) {
                                    videoView.setImage(videoImage);
                                }
                            });
                        }

                        frameChunks.remove(frameId);
                        chunkCounts.remove(frameId);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }
}
