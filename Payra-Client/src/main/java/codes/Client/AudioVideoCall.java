package codes.Client;

import codes.Wrappers.ClientInfo;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.*;

public class AudioVideoCall {
    private Client sender;
    private ClientInfo receiver;
    private Image senderImage;
    private Image receiverImage;
    private String receiverIPAddress;
    private volatile String callType;

    private TargetDataLine microphone;
    private SourceDataLine speaker;
    private DatagramSocket audioSenderSocket;
    private DatagramSocket audioReceiverSocket;
    private DatagramSocket videoSenderSocket;
    private DatagramSocket videoReceiverSocket;
    private VideoCapture webcam;
    private boolean isCallRunning;
    private boolean isAudioRunning;
    private boolean isVideoRunning;
    private volatile long lastFrameReceivedTime;
    private volatile long lastAudioReceivedTime;

    private Stage videoStage;
    private StackPane root;
    private ImageView videoView;
    private ImageView myVideoView;
    private Rectangle background;
    private Label receiverName;
    private Label timeLabel;
    private VBox labelHolder;
    private HBox buttonContainer;
    private Rectangle border1;
    private Rectangle border2;
    private double windowWidth = Screen.SCREENWIDTH * 0.7;
    private double windowHeight = Screen.SCREENHEIGHT * 0.8;

    static {
        System.load("C:/Program Files/opencv/build/java/x64/opencv_java4120.dll");
    }

    public AudioVideoCall(Client sender, ClientInfo receiver) {
        this.sender = sender;
        this.receiver = receiver;

        senderImage = new Image(new ByteArrayInputStream(sender.getInfo().getProfilePicture()));
        receiverImage = new Image(new ByteArrayInputStream(receiver.getProfilePicture()));

        isCallRunning = true;

        new Thread(() -> {
            while (true) {
                if (sender.getCallAcceptanceStatus() != null && sender.getCallAcceptanceStatus().equals("ended")) {
                    sender.setCallStatus(false);
                    System.out.println("call ended");
                    Platform.runLater(() -> {
                        endCallS();
                        root.getChildren().remove(buttonContainer);
                        if (myVideoView != null) {
                            root.getChildren().remove(myVideoView);
                        }
                        videoView.setFitWidth(windowHeight * 0.5);
                        videoView.setFitHeight(windowHeight * 0.5);
                        videoView.setImage(receiverImage);
                        Circle clip = new Circle(windowHeight * 0.25, windowHeight * 0.25, windowHeight * 0.25);
                        videoView.setClip(clip);
                        videoView.setClip(clip);
                        background.setStyle("-fx-fill: #ffffff");
                        receiverName.setStyle("-fx-background-color: transparent; -fx-font-family: Open Sans; -fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #0f2e4d;");
                        labelHolder.getChildren().remove(timeLabel);
                        if (border1 != null) {
                            root.getChildren().remove(border1);
                        }
                        if (border2 != null) {
                            root.getChildren().remove(border2);
                        }
                        Label endingMessage = new Label("Call ended");
                        endingMessage.setStyle("-fx-background-color: transparent; -fx-font-family: Open Sans; -fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #db0202;");
                        labelHolder.getChildren().add(endingMessage);
                        PauseTransition delay = new PauseTransition(Duration.seconds(3));
                        delay.setOnFinished(event -> videoStage.close());
                        delay.play();
                    });
                    break;
                }
                else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }

    public void startAudioCall(String receiverIPAddress) {
        this.receiverIPAddress = receiverIPAddress;
        callType = "audio";
        isAudioRunning = true;

        loadCallSetup();
        startAudioThread();
    }

    public void startVideoCall(String receiverIPAddress) {
        this.receiverIPAddress = receiverIPAddress;
        callType = "video";
        isAudioRunning = true;
        isVideoRunning = true;

        loadCallSetup();
        startAudioThread();
        startVideoThread();
    }

    private void startAudioThread() {
        // Initiating audio sender thread

        new Thread(() -> {
            try {
                audioSenderSocket = new DatagramSocket();
                InetAddress receiverAddress = InetAddress.getByName(receiverIPAddress);
                int port = 22222;

                AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                microphone = (TargetDataLine) AudioSystem.getLine(info);
                microphone.open(format);
                microphone.start();

                byte[] buffer = new byte[4096];

                while (isCallRunning) {
                    int bufferSize = microphone.read(buffer, 0, buffer.length);

                    if (isAudioRunning) {
                        DatagramPacket packet = new DatagramPacket(buffer, bufferSize, receiverAddress, port);
                        audioSenderSocket.send(packet);
                    }
                    else {
                        Thread.sleep(500);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Initiating audio receiver thread

        new Thread(() -> {
            try {
                audioReceiverSocket = new DatagramSocket(22222);

                AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                speaker = (SourceDataLine) AudioSystem.getLine(info);
                speaker.open(format);
                speaker.start();

                byte[] buffer = new byte[4096];

                while (isCallRunning) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    audioReceiverSocket.receive(packet);
                    speaker.write(packet.getData(), 0, packet.getLength());

                    lastAudioReceivedTime = System.currentTimeMillis();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void startVideoThread() {
        // Initiating video sender thread

        new Thread(() -> {
            try {
                videoSenderSocket = new DatagramSocket();
                InetAddress receiverAddress = InetAddress.getByName(receiverIPAddress);
                int port = 22223;

                webcam = new VideoCapture(0, Videoio.CAP_DSHOW);
                webcam.set(Videoio.CAP_PROP_FRAME_WIDTH, windowWidth);
                webcam.set(Videoio.CAP_PROP_FRAME_HEIGHT, windowHeight);

                if (!webcam.isOpened()) {
                    return;
                }

                MatOfInt jpegParams = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 80);
                final int CHUNK_SIZE = 1400;

                while (isCallRunning) {
                    Mat frame = new Mat();

                    if (webcam.read(frame) && !frame.empty()) {
                        if (isVideoRunning) {
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
                                videoSenderSocket.send(packet);
                            }
                        }
                        else {
                            Thread.sleep(500);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Initiating video receiver thread

        new Thread(() -> {
            try {
                videoReceiverSocket = new DatagramSocket(22223);
                byte[] buffer = new byte[1500];
                Map<Integer, List<byte[]>> frameChunks = new HashMap<>();
                Map<Integer, Integer> chunkCounts = new HashMap<>();

                while (isCallRunning) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    videoReceiverSocket.receive(packet);

                    lastFrameReceivedTime = System.currentTimeMillis();

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
                            Mat resizedMat = new Mat();
                            Size newSize = new Size(windowWidth, windowHeight);
                            Imgproc.resize(frame, resizedMat, newSize);
                            MatOfByte bufferMat = new MatOfByte();
                            Imgcodecs.imencode(".jpg", resizedMat, bufferMat);
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

        // Initiating video receiver monitoring thread

        new Thread(() -> {
            while (isCallRunning) {
                long currentTime = System.currentTimeMillis();

                if (currentTime - lastFrameReceivedTime > 2000) {
                    Platform.runLater(() -> {
                        videoView.setImage(receiverImage);
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }

    private void loadCallSetup() {
        // Initiating video preview

        Platform.runLater(() -> {
            videoView = new ImageView();

            if (callType.equals("audio")) {
                videoView.setFitWidth(windowHeight * 0.5);
                videoView.setFitHeight(windowHeight * 0.5);
            } else {
                videoView.setFitWidth(windowWidth);
                videoView.setFitHeight(windowHeight);
            }

            videoView.setPreserveRatio(true);

            if (callType.equals("audio")) {
                Circle clip1 = new Circle(windowHeight * 0.25, windowHeight * 0.25, windowHeight * 0.25);
                videoView.setClip(clip1);

                DropShadow shadow = new DropShadow();
                shadow.setRadius(15);
                shadow.setOffsetX(0);
                shadow.setOffsetY(5);
                shadow.setColor(Color.rgb(0, 0, 0, 0.3));
                videoView.setEffect(shadow);
            }
            else {
                border1 = new Rectangle();
                Rectangle clip1 = new Rectangle();
                clip1.setArcWidth(20);
                clip1.setArcHeight(20);
                videoView.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
                    clip1.setWidth(newBounds.getWidth());
                    clip1.setHeight(newBounds.getHeight());
                    border1.setArcWidth(20);
                    border1.setArcHeight(20);
                    border1.setFill(Color.BLACK);
                    border1.setWidth(newBounds.getWidth() + 6);
                    border1.setHeight(newBounds.getHeight() + 6);
                });
                videoView.setClip(clip1);
                videoView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 20, 0, 0, 0);");
            }

            videoView.setImage(receiverImage);

            if (callType.equals("video")) {
                myVideoView = new ImageView();
                myVideoView.setFitWidth(windowWidth * 0.2);
                myVideoView.setFitHeight(windowHeight * 0.2);
                myVideoView.setPreserveRatio(true);
                border2 = new Rectangle();
                Rectangle clip2 = new Rectangle();
                clip2.setArcWidth(20);
                clip2.setArcHeight(20);
                myVideoView.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
                    clip2.setWidth(newBounds.getWidth());
                    clip2.setHeight(newBounds.getHeight());
                    border2.setArcWidth(20);
                    border2.setArcHeight(20);
                    border2.setFill(Color.BLACK);
                    border2.setWidth(newBounds.getWidth() + 4);
                    border2.setHeight(newBounds.getHeight() + 4);
                });
                myVideoView.setClip(clip2);
                myVideoView.setStyle("-fx-border-color: black; -fx-border-width: 5px; -fx-background-radius: 10px; -fx-border-radius: 10px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 10, 0.5, 0, 0);");
            }

            background = new Rectangle();
            if (callType.equals("audio")) {
                background.setFill(Color.web("#ffffff"));
            } else {
                background.setFill(Color.web("#ffffff"));
            }

            ImageView endCallButton = new ImageView(new Image(String.valueOf(AudioVideoCall.class.getResource("/images/End_Call.png"))));
            ImageView microphoneButton = new ImageView(new Image(String.valueOf(AudioVideoCall.class.getResource("/images/Unmute_Button.png"))));
            ImageView videoButton = new ImageView();
            if (callType.equals("audio")) {
                videoButton.setImage(new Image(String.valueOf(AudioVideoCall.class.getResource("/images/Stop_Video.png"))));
            } else {
                videoButton.setImage(new Image(String.valueOf(AudioVideoCall.class.getResource("/images/Resume_Video.png"))));
            }

            for (ImageView button : new ImageView[]{endCallButton, microphoneButton, videoButton}) {
                button.setFitWidth(50);
                button.setFitHeight(50);
                Circle clip = new Circle(25, 25, 25);
                button.setClip(clip);
                button.setStyle("-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.2), 10, 0.5, 0, 0);");
                button.setOnMouseEntered(e -> {
                    button.setScaleX(1.1);
                    button.setScaleY(1.1);
                });
                button.setOnMouseExited(e -> {
                    button.setScaleX(1.0);
                    button.setScaleY(1.0);
                });
            }

            endCallButton.setOnMouseClicked(event -> {
                sender.setCallStatus(false);
                try {
                    sender.getServerOutput().writeObject("call_ended:" + receiver.getId());
                    sender.getServerOutput().flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                endCall();
            });

            microphoneButton.setOnMouseClicked(event -> {
                if (isAudioRunning) {
                    System.out.println("mic turned off");
                    microphoneButton.setImage(new Image(String.valueOf(AudioVideoCall.class.getResource("/images/Mute_Button.png"))));
                    isAudioRunning = false;
                } else {
                    System.out.println("mic turned on");
                    microphoneButton.setImage(new Image(String.valueOf(AudioVideoCall.class.getResource("/images/Unmute_Button.png"))));
                    isAudioRunning = true;
                }
            });

            videoButton.setOnMouseClicked(event -> {
                if (isVideoRunning) {
                    System.out.println("video turned off");
                    isVideoRunning = false;
                    videoButton.setImage(new Image(String.valueOf(AudioVideoCall.class.getResource("/images/Stop_Video.png"))));
                    myVideoView.setImage(senderImage);
                } else {
                    System.out.println("video turned on");
                    videoButton.setImage(new Image(String.valueOf(AudioVideoCall.class.getResource("/images/Resume_Video.png"))));
                    isVideoRunning = true;
                    if (callType.equals("audio")) {
                        callType = "video";
                        videoStage.close();
                        loadCallSetup();
                        startVideoThread();
                    }
                }
            });

            buttonContainer = new HBox(videoButton, microphoneButton, endCallButton);
            buttonContainer.setSpacing(15);
            buttonContainer.setPadding(new Insets(8));
            buttonContainer.setAlignment(Pos.CENTER);
            buttonContainer.setMaxWidth(Region.USE_PREF_SIZE);
            buttonContainer.setMaxHeight(66);
            buttonContainer.setBackground(new Background(new BackgroundFill(Color.web("#000000", 0.6), new CornerRadii(20), Insets.EMPTY)));

            if (callType.equals("audio")) {
                receiverName = new Label(receiver.getFirstName() + " " + receiver.getLastName());
                receiverName.setStyle("-fx-background-color: transparent; -fx-font-family: Open Sans; -fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #0f2e4d;");
                timeLabel = new Label();
                timeLabel.setStyle("-fx-background-color: rgba(15, 46, 77, 0.15); -fx-font-family: Open Sans; -fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #0f2e4d; -fx-padding: 4 10; -fx-background-radius: 20;");
            } else {
                receiverName = new Label(receiver.getFirstName() + " " + receiver.getLastName());
                receiverName.setStyle("-fx-background-color: transparent; -fx-font-family: Open Sans; -fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
                timeLabel = new Label();
                timeLabel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5); -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 20; -fx-font-size: 14;");
            }

            new Thread(() -> {
                long hr = 0;
                long min = 0;
                long sec = 0;
                while (isCallRunning) {
                    StringBuilder time = new StringBuilder();
                    if (hr > 0) {
                        if (hr < 10) {
                            time.append('0').append(hr);
                        } else {
                            time.append(hr);
                        }
                        time.append(':');
                    }
                    if (min < 10) {
                        time.append('0').append(min);
                    } else {
                        time.append(min);
                    }
                    time.append(':');
                    if (sec < 10) {
                        time.append('0').append(sec);
                    } else {
                        time.append(sec);
                    }

                    Platform.runLater(() -> {
                        timeLabel.setText(time.toString());
                    });

                    sec++;
                    if (sec == 60) {
                        min++;
                        sec = 0;
                    }

                    if (min == 60) {
                        hr++;
                        min = 0;
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();

            labelHolder = new VBox(receiverName, timeLabel);
            labelHolder.setSpacing(5);
            labelHolder.setAlignment(Pos.TOP_CENTER);

            if (callType.equals("audio")) {
                root = new StackPane(background, videoView, labelHolder, buttonContainer);
            } else {
                root = new StackPane(background, border1, videoView, border2, myVideoView, labelHolder, buttonContainer);

                StackPane.setAlignment(myVideoView, Pos.BOTTOM_RIGHT);
                StackPane.setMargin(myVideoView, new Insets(12));

                StackPane.setAlignment(border2, Pos.BOTTOM_RIGHT);
                StackPane.setMargin(border2, new Insets(10));
            }

            background.widthProperty().bind(root.widthProperty());
            background.heightProperty().bind(root.heightProperty());

            StackPane.setAlignment(videoView, Pos.CENTER);
            StackPane.setMargin(videoView, new Insets(10));

            StackPane.setAlignment(labelHolder, Pos.TOP_CENTER);
            StackPane.setMargin(labelHolder, new Insets(10));

            StackPane.setAlignment(buttonContainer, Pos.BOTTOM_CENTER);
            StackPane.setMargin(buttonContainer, new Insets(10));

            Scene scene = new Scene(root, windowWidth + 20, windowHeight + 20);

            videoStage = new Stage();
            if (callType.equals("audio")) {
                videoStage.setTitle("Audio Call");
            } else {
                videoStage.setTitle("Video Call");
            }
            Image icon = new Image(String.valueOf(AudioVideoCall.class.getResource("/images/Payra.png")));
            videoStage.getIcons().add(icon);
            videoStage.setAlwaysOnTop(true);
            videoStage.setResizable(false);
            videoStage.setScene(scene);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), scene.getRoot());
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();

            videoStage.show();

            videoStage.setOnCloseRequest(windowEvent -> {
                sender.setCallStatus(false);
                try {
                    sender.getServerOutput().writeObject("call_ended:" + receiver.getId());
                    sender.getServerOutput().flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                endCall();
            });
        });
    }

    public void endCall() {
        if (videoStage != null) {
            videoStage.close();
        }

        endCallS();
    }

    public void endCallS() {
        if (microphone != null) {
            microphone.close();
        }

        if (speaker != null) {
            speaker.close();
        }

        if (audioSenderSocket != null && !audioSenderSocket.isClosed()) {
            audioSenderSocket.close();
        }

        if (audioReceiverSocket != null && !audioReceiverSocket.isClosed()) {
            audioReceiverSocket.close();
        }

        if (videoSenderSocket != null && !videoSenderSocket.isClosed()) {
            videoSenderSocket.close();
        }

        if (videoReceiverSocket != null && !videoReceiverSocket.isClosed()) {
            videoReceiverSocket.close();
        }

        if (webcam != null && webcam.isOpened()) {
            webcam.release();
        }

        isCallRunning = false;
        isAudioRunning = false;
        isVideoRunning = false;
    }
}
