package codes;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Scanner;

public class WriterThread implements Runnable{
    private Thread writerThread;
    private ObjectOutputStream output;

    public WriterThread(ObjectOutputStream output)
    {
        writerThread = new Thread(this);
        this.output = output;
        writerThread.start();
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        while(true)
        {
            String message = scanner.nextLine();
            try {
                output.writeObject(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
