package codes;

import com.almasb.fxgl.io.FileExtension;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.*;
import java.util.concurrent.CountDownLatch;

public class Inbox {
    public Inbox(){}

    public void startInboxView(Client client, Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Inbox.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), Screen.SCREENWIDTH, Screen.SCREENHEIGHT);
        InboxController inboxController = fxmlLoader.getController();
        inboxController.setInboxController(client, stage);
        Image icon = new Image(String.valueOf(getClass().getResource("/images/Payra.png")));
        stage.getIcons().add(icon);
        stage.setTitle("Inbox");
        stage.setScene(scene);

        inboxController.User.setText(client.getEmail());

        // Creating input field for the recipient id

        inboxController.Recipient = new TextField();
        inboxController.Recipient.setLayoutX(100);
        inboxController.Recipient.setLayoutY(500);
        inboxController.Recipient.setPrefWidth(200);
        inboxController.Recipient.setPrefHeight(50);
        inboxController.Recipient.requestFocus();
        inboxController.Recipient.setFont(new Font(20));

        inboxController.Recipient.setOnKeyPressed(event1 -> {
            if (event1.getCode() == KeyCode.ENTER) {
                // Taking the recipient id

                String recipientId = inboxController.getRecipientId();

                // Sending chat command with recipient id to the server

                try {
                    CountDownLatch latch = new CountDownLatch(1);
                    client.setLatch(latch);

                    client.getServerOutput().writeObject("chat_with:" + recipientId);
                    client.getServerOutput().flush();

                    latch.await();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

                if (!recipientId.equals(client.getReceiverId())) {
                    System.out.println("Unable to connect");
                    return;
                }

                // Creating text area for showing the chat

                inboxController.Chat = new TextArea();
                inboxController.Chat.setEditable(false);
                inboxController.Chat.setText("Chat with " + recipientId + "\n\n");
                inboxController.Chat.setLayoutX(800);
                inboxController.Chat.setLayoutY(150);
                inboxController.Chat.setPrefWidth(600);
                inboxController.Chat.setPrefHeight(500);
                inboxController.Chat.setFont(new Font(20));

                // Creating text area for writing message

                inboxController.Message = new TextArea();
                inboxController.Message.setLayoutX(800);
                inboxController.Message.setLayoutY(700);
                inboxController.Message.setPrefWidth(600);
                inboxController.Message.setPrefHeight(50);
                inboxController.Message.setFont(new Font(20));

//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }

                // Starting chat reader thread (Receives message from the chat server and shows it in the chat box)

                new Thread(() -> {
                    while (true) {
                        Object messageInfo;

                        try {
                            messageInfo = client.getChatInput().readObject();
                        } catch (IOException | ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }

                        String sender = ((String) messageInfo).split(",")[0];
                        String timestamp = ((String) messageInfo).split(",")[1];
                        String message = ((String) messageInfo).substring(sender.length() + timestamp.length() + 2);

                        if (sender.equals(client.getId())) {
                            Platform.runLater(() -> inboxController.Chat.appendText("                    " + sender + ": " + message + "\n\n"));
                        }
                        else {
                            Platform.runLater(() -> inboxController.Chat.appendText(sender + ": " + message + "\n\n"));
                        }
                    }
                }).start();

                // Starting message sender thread (Sends messages to the chat server)

                new Thread(() -> {
                    inboxController.Message.setOnKeyPressed(event2 -> {
                        if (event2.getCode() == KeyCode.ENTER) {
                            Object message = inboxController.getMessage();

                            try {
                                client.getChatOutput().writeObject(message);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                }).start();

                inboxController.InboxView.getChildren().add(inboxController.Chat);
                inboxController.InboxView.getChildren().add(inboxController.Message);
            }
        });

        inboxController.InboxView.getChildren().add(inboxController.Recipient);

        stage.setOnCloseRequest(event ->{
            Platform.exit();
            System.exit(0);
        });

        stage.show();
    }

    public void stopInboxView(){}
}


