package codes;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerThread implements Runnable{
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private final Socket socket;
    private Client user;
    private Client currentTarget;

    // Constructor

    public ServerThread(Socket clientSocket) {
        this.socket=clientSocket;

    }
    @Override
    public void run() {
        try {
            this.output = new ObjectOutputStream(socket.getOutputStream());
            this.input = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        try {
            //Login UI should handle this
            output.writeObject("Enter your username:");
            String username = (String) input.readObject();
            user =findClientByUsername(username);


            //Mark this client as an active user
            Server.activeUsers.put(user, this);

            // Find who we want to talk to rn
            //Selecting inbox by this client on their interface should do it
            output.writeObject("Enter who you want to talk to:");
            String targetUsername = (String) input.readObject();
            currentTarget = findClientByUsername(targetUsername);

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }




        //Now this thread will handle this client sending messages and getting target client's messages

        while(true){
            try {
                String msg = (String) input.readObject();

                //If my current client wants to talk to another client. UI should be integrated.
                if (msg.startsWith("switch_target:")) {
                    String newTargetUsername = msg.substring("switch_target:".length());
                    currentTarget = findClientByUsername(newTargetUsername);

                    if (currentTarget != null) {
                        output.writeObject("Switched to " + newTargetUsername);
                    } else {
                        output.writeObject("User not found: " + newTargetUsername);
                    }
                    continue;
                }

                ServerThread targetThread = Server.activeUsers.get(currentTarget);

                if (targetThread != null) {
                    targetThread.sendMessage(msg);
                } else {
                    output.writeObject("User " + currentTarget + " is not online.");
                }

            } catch (IOException | ClassNotFoundException e) {
                System.out.println("User " + user.getUserName() + " disconnected.");
                Server.activeUsers.remove(user);
                try {
                    socket.close();
                    input.close();
                    output.close();
                }
                catch (IOException ignored) {
                }
                break;
            }

        }

    }
    private Client findClientByUsername(String username) {
        for (Client c : Server.activeUsers.keySet()) {
            if (c.getUserName().equals(username)) {
                return c;
            }
        }
        return null;
    }
    public void sendMessage(String msg){
        try {
            output.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

