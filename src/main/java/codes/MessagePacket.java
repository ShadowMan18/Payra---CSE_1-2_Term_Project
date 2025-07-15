package codes;

import java.io.Serializable;
import java.time.LocalDateTime;

public class MessagePacket implements Serializable {
    private String sender;
    private String receiver;
    private String message;
    private String filename;
    private byte[] filedata;
    private LocalDateTime datetime;

    MessagePacket(String sender, String receiver, String message, String filename, byte[] filedata) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.filename = filename;
        this.filedata = filedata;
    }

    MessagePacket(String sender, String receiver, String message, String filename, byte[] filedata, LocalDateTime datetime) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.filename = filename;
        this.filedata = filedata;
        this.datetime = datetime;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public String getFilename() {
        return filename;
    }

    public byte[] getFiledata() {
        return filedata;
    }

    public LocalDateTime getDatatime() { return datetime; }
}
