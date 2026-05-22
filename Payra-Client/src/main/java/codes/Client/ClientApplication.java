package codes.Client;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.*;

public class ClientApplication extends Application{
    static Client client;

    @Override
    public void start(Stage stage) throws Exception {
        client.setStage(stage);
        client.getIntroPage().startIntroPageView(client, stage);
    }

    public static void main(String[] args) {
        // IP discovery

        String ipAddress = null;

        while(ipAddress == null) {
            try (DatagramSocket clientDatagramSocket = new DatagramSocket()){
                clientDatagramSocket.setBroadcast(true);
                clientDatagramSocket.setSoTimeout(2000);

                byte[] message = "DISCOVER_SERVER".getBytes();
                DatagramPacket packet = new DatagramPacket(message, message.length, InetAddress.getByName("255.255.255.255"), 22222);
                clientDatagramSocket.send(packet);

                byte[] responseBytes = new byte[256];
                DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length);

                clientDatagramSocket.receive(responsePacket);
                String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
                ipAddress = response.split(":")[1];
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println(ipAddress);

        client = new Client(ipAddress);

        String userHome = System.getProperty("user.home");

        File chatMediaDirectory = new File(userHome + "/Payra/Chat Media");

        if(!chatMediaDirectory.exists()) {
            chatMediaDirectory.mkdirs();
        }

        File feedMediaDirectory = new File(userHome + "/Payra/Feed Media");

        if(!feedMediaDirectory.exists()) {
            feedMediaDirectory.mkdirs();
        }

        launch();
    }
}