package com.example.javafx_project;

import javax.sound.sampled.*;
import java.io.*;

public class MicrophoneCapture {
    private static AudioFormat format;
    private static DataLine.Info info;
    private static TargetDataLine microphone;
    private static AudioInputStream audioStream;
    private static File wavFile;

    private static AudioFormat getAudioFormat() {
        float sampleRate = 16000.0f; // 16 kHz
        int sampleSizeInBits = 16;
        int channels = 1; // mono
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    public static void startMicrophone() {
        format = getAudioFormat();
        info = new DataLine.Info(TargetDataLine.class, format);
        try
        {
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Line not supported");
                return;
            }
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();
            System.out.println("Recording... Press Ctrl+C to stop.");
            audioStream = new AudioInputStream(microphone);
            wavFile = new File("recording.wav");
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
