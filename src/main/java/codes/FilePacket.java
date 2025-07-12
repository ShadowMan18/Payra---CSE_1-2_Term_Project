package codes;

import java.io.Serializable;

public class FilePacket implements Serializable {
    private String filename;
    private byte[] fileData;

    public FilePacket(String filename, byte[] fileData) {
        this.filename = filename;
        this.fileData = fileData;
    }

    public String getFilename() {
        return filename;
    }

    public byte[] getFileData() {
        return fileData;
    }
}
