package codes;

import java.io.IOException;
import java.io.ObjectInputStream;

public class ReaderThread implements Runnable{
    private Thread readerThread;
    private ObjectInputStream input;

    public ReaderThread(ObjectInputStream input)
    {
        readerThread = new Thread(this);
        this.input = input;
        readerThread.start();
    }

    @Override
    public void run() {
        while(true)
        {
            String message;
            try {
                message = (String) (input.readObject());
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Received: " + message);
        }
    }
}
