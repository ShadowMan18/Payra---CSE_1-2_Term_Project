package codes.Server;

import codes.Wrappers.CommentPacket;
import codes.Wrappers.PostPacket;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class NewsFeedServer implements Runnable {

    // Shared broadcast list — every connected NewsFeedThread registers here
    public static final CopyOnWriteArrayList<ObjectOutputStream> connectedOutputs
            = new CopyOnWriteArrayList<>();

    private final ServerSocket serverSocket;
    private final Thread serverThread;

    // -----------------------------------------------------------------------
    // Singleton lifecycle
    // -----------------------------------------------------------------------

    public NewsFeedServer() {
        try {
            serverSocket = new ServerSocket(4351);
            System.out.println("NewsFeedServer running on port 4351");
        } catch (IOException e) {
            throw new RuntimeException("Could not start NewsFeedServer on port 4351", e);
        }

        serverThread = new Thread(this);
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                new NewsFeedThread(clientSocket);
            } catch (IOException e) {
                System.out.println("NewsFeedServer accept error: " + e.getMessage());
                break;
            }
        }
    }

    // -----------------------------------------------------------------------
    // Broadcast helpers (called from NewsFeedThread and MainServerThread)
    // -----------------------------------------------------------------------

    public static void addClient(ObjectOutputStream out) {
        connectedOutputs.add(out);
    }

    public static void removeClient(ObjectOutputStream out) {
        connectedOutputs.remove(out);
    }

    public static int clientListSize() {
        return connectedOutputs.size();
    }

    public static void broadcast(Object obj) {
        for (ObjectOutputStream out : connectedOutputs) {
            try {
                synchronized (out) {
                    out.writeObject(obj);
                    out.flush();
                }
            } catch (IOException e) {
                connectedOutputs.remove(out);
            }
        }
    }

    public static void broadcastToAll(int postId, String reactor, String oldType, String newType) {
        broadcast("REACTION|" + postId + "|" + reactor + "|" + oldType + "|" + newType);
    }
}