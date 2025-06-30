package codes;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

class ChatServer implements Runnable {
    private ServerSocket serverSocket;
    private Socket clientSocket1;
    private Socket clientSocket2;
    private Thread chatThread;
    ObjectOutputStream output1;
    ObjectInputStream input1;
    ObjectOutputStream output2;
    ObjectInputStream input2;

    Object message;

    public ChatServer(int port) {
        chatThread = new Thread(this);

        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Chat server activated");

        chatThread.start();
    }

    @Override
    public void run() {
        try {
            clientSocket1 = serverSocket.accept();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            clientSocket2 = serverSocket.accept();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            output1 = new ObjectOutputStream(clientSocket1.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            input1 = new ObjectInputStream(clientSocket1.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            output2 = new ObjectOutputStream(clientSocket2.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            input2 = new ObjectInputStream(clientSocket2.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        new Thread(() -> {
            while (true){
                try {
                    message = input1.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

                System.out.println((String) message);

                try {
                    output2.writeObject(message);
                    output2.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        new Thread(() -> {
            while(true) {
                try {
                    message = input2.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

                System.out.println((String) message);

                try {
                    output1.writeObject(message);
                    output1.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }
}

