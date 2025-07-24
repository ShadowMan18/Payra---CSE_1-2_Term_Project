package codes;

import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.*;

public class AudioVideoCall {
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

        // Initiating video sender thread

        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress receiverAddress = InetAddress.getByName(receiverIPAddress);
                int port = 22223;

                VideoCapture webcam = new VideoCapture(0, Videoio.CAP_DSHOW);
                webcam.set(Videoio.CAP_PROP_FRAME_WIDTH, Screen.SCREENWIDTH * 0.5);
                webcam.set(Videoio.CAP_PROP_FRAME_HEIGHT, Screen.SCREENHEIGHT * 0.5);

                if (!webcam.isOpened()) {
                    System.out.println("❌ Cannot open webcam!");
                    return;
                }

                MatOfInt jpegParams = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 70);
                final int CHUNK_SIZE = 1400;

                while (true) {
                    Mat frame = new Mat();

                    if (webcam.read(frame) && !frame.empty()) {
                        Core.flip(frame, frame, 1);

                        MatOfByte buffer = new MatOfByte();
                        Imgcodecs.imencode(".jpg", frame, buffer, jpegParams);
                        byte[] frameBytes = buffer.toArray();

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
                            Mat resizedView = new Mat();
                            Size videoResolution = new Size(Screen.SCREENWIDTH * 0.5, Screen.SCREENHEIGHT * 0.5);
                            Imgproc.resize(frame, resizedView, videoResolution);

                            HighGui.imshow("Receiver Feed", resizedView);
                            HighGui.waitKey(1);
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
