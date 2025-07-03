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
import javafx.stage.Stage;
import org.controlsfx.control.BreadCrumbBar;

import java.awt.*;
import java.io.*;
import java.nio.file.attribute.UserPrincipal;

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

        inboxController.Recipient = new TextField();
        inboxController.Recipient.setLayoutX(100);
        inboxController.Recipient.setLayoutY(500);
        inboxController.Recipient.setPrefWidth(200);
        inboxController.Recipient.setPrefHeight(50);
        inboxController.Recipient.requestFocus();

        inboxController.Recipient.setOnKeyPressed(event1 -> {
            if (event1.getCode() == KeyCode.ENTER) {
                String recipientId = inboxController.getRecipientId();

                if (!client.isConnected(recipientId)) {
                    try {
                        client.getServerOutput().writeObject("connect_to:" + recipientId);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    try {
                        client.getServerOutput().flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }


                inboxController.Chat = new TextArea();
                inboxController.Chat.setEditable(false);
                inboxController.Chat.setText("Chat with " + recipientId + "\n\n");
                inboxController.Chat.setLayoutX(800);
                inboxController.Chat.setLayoutY(150);
                inboxController.Chat.setPrefWidth(300);
                inboxController.Chat.setPrefHeight(300);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                BufferedReader reader;

                try {
                    reader = new BufferedReader(new FileReader("database/clients/" + client.getId() + "/chats/" + recipientId +"/texts.txt"));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }

                new Thread(() -> {
                    while (true) {
                        String message;

                        try {
                            message = reader.readLine();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        if (message != null) {
                            Platform.runLater(() -> inboxController.Chat.appendText(message + "\n"));
                        }
                    }
                }).start();

                inboxController.Message = new TextArea();
                inboxController.Message.setLayoutX(800);
                inboxController.Message.setLayoutY(500);
                inboxController.Message.setPrefWidth(300);
                inboxController.Message.setPrefHeight(300);

                inboxController.Message.setOnKeyPressed(event2 -> {
                    if (event2.getCode() == KeyCode.ENTER) {
                        String message = inboxController.getMessage();
                        int index = client.getIdIndex(recipientId);

                        try {
                            client.getChatOutput(index).writeObject(message);

                            try (BufferedWriter writer = new BufferedWriter(new FileWriter("database/clients/" + client.getId() + "/chats/" + recipientId + "/texts.txt", true))) {
                                writer.write(client.getId() + ":" + message + "\n");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            try (BufferedWriter writer = new BufferedWriter(new FileWriter("database/clients/" + recipientId + "/chats/" + client.getId() + "/texts.txt", true))) {
                                writer.write(client.getId() + ":" + message + "\n");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

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


