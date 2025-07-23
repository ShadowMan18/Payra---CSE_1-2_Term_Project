package codes;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class AudioVideoCall {
    public static void startAudioCall(String receiverIPAddress) {
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
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Initiating audio call

//        startAudioCall(receiverIPAddress);

        // Initiating video sending thread

        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress receiverAddress = InetAddress.getByName(receiverIPAddress);
                int port = 22223;

                VideoCapture webcam = new VideoCapture(0, Videoio.CAP_DSHOW);

                webcam.set(Videoio.CAP_PROP_FRAME_WIDTH, 1500);
                webcam.set(Videoio.CAP_PROP_FRAME_HEIGHT, 600);

                if (!webcam.isOpened()) {
                    System.out.println("❌ Cannot open webcam!");
                    return;
                }

                MatOfInt jpegParams = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 30);

                while (true) {
                    Mat frame = new Mat();

                    if (webcam.read(frame) && !frame.empty()) {
                        Core.flip(frame, frame, 1);

                        MatOfByte buffer = new MatOfByte();
                        Imgcodecs.imencode(".jpg", frame, buffer, jpegParams);
                        byte[] frameBytes = buffer.toArray();

                        // Check frame size to avoid UDP packet size error (max safe ~60KB)
                        if (frameBytes.length > 60000) {
                            System.out.println("⚠️ Frame too large (" + frameBytes.length + " bytes), skipping...");
                        } else {
                            DatagramPacket packet = new DatagramPacket(frameBytes, frameBytes.length, receiverAddress, port);
                            socket.send(packet);
                        }
                    }

                    // Display frame in window (optional)
//                     HighGui.imshow("Webcam Feed", frame);

//                    if (HighGui.waitKey(15) == 27) break; // ESC to exit
                }

//                webcam.release();
//                HighGui.destroyAllWindows();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Initiating video receiving thread

        new Thread(() -> {
            try (DatagramSocket receiverSocket = new DatagramSocket(22223)) {
                byte[] buffer = new byte[65535];

                System.out.println("📥 Listening on port 22223...");

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    receiverSocket.receive(packet);

                    byte[] frameBytes = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
                    Mat frame = Imgcodecs.imdecode(new MatOfByte(frameBytes), Imgcodecs.IMREAD_COLOR);

                    if (!frame.empty()) {
                        HighGui.imshow("Receiver Feed1", frame);
                        HighGui.waitKey(1);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
