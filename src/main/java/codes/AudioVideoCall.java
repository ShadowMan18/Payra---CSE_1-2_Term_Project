package codes;

import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class AudioVideoCall {
    public static void startAudioCall(String receiverIPAddress) {
        // Initiating audio sender thread

        new Thread(() -> {
            try {
                DatagramSocket audioSocket = new DatagramSocket();
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
                DatagramSocket audioSocket = new DatagramSocket(22223);

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
        // Initiating audio call

        startAudioCall(receiverIPAddress);

        // Initiating video sending thread

        new Thread(() -> {

        }).start();

        // Initiating video receiving thread

        new Thread(() -> {

        }).start();
    }
}
