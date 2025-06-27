package codes;

import javax.sound.sampled.*;
import java.io.*;

public class MicrophoneCapture {
    private static TargetDataLine microphone;

    public MicrophoneCapture(){}

    private static AudioFormat getAudioFormat() {
        float sampleRate = 16000.0f; // 16 kHz
        int sampleSizeInBits = 16;
        int channels = 1; // mono
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    public static void startMicrophone() {
        AudioFormat format = getAudioFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        try
        {
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Line not supported");
                return;
            }
            TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();
            System.out.println("Recording... Press Ctrl+C to stop.");
            AudioInputStream audioStream = new AudioInputStream(microphone);
            File wavFile = new File("recording.wav");
            Thread recordingThread = new Thread(() -> {
                try
                {
                    AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, wavFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            recordingThread.start();
        }
        catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public static void stopMicrophone()
    {
        microphone.stop();
        microphone.close();
        System.out.println("Recording saved as 'recording.wav'");
    }
}
