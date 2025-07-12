package codes;

import java.io.Serializable;

public class MessagePacket implements Serializable {
    private String sender;
    private String message;
    private String filename;
    private byte[] filedata;

    MessagePacket(String sender, String message, String filename, byte[] filedata) {
        this.sender = sender;
        this.message = message;
        this.filename = filename;
        this.filedata = filedata;
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

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setFiledata(byte[] filedata) {
        this.filedata = filedata;
    }

    public void reset() {
        sender = null;
        message = null;
        filename = null;
        filedata = null;
    }
}
