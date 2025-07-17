package codes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;


public class PostPacket implements Serializable {
    private final int postId;
    private final String author;
    private final String content;
    private final String fileName;
    private final byte[] fileData;
    private final LocalDateTime timestamp;

    private final Map<String, Integer> reactionCounts;
    private final String userReactionType;
    public PostPacket(String author, String content, String fileName, byte[] fileData) {
        this(-1, author, content, fileName, fileData, null, null, "none");
    }

    public PostPacket(int postId, String author, String content, String fileName, byte[] fileData,
                      LocalDateTime timestamp, Map<String, Integer> reactionCounts, String userReactionType) {
        this.postId = postId;
        this.author = author;
        this.content = content;
        this.fileName = fileName;
        this.fileData = fileData;
        this.timestamp = timestamp;
        this.reactionCounts = reactionCounts;
        this.userReactionType = userReactionType;
    }

    public String getUserReactedType() {
        return userReactionType;
    }

    public String getFormattedTimestamp() {
        if (timestamp == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
        return timestamp.format(formatter);
    }


    public int getPostId() {
        return postId;
    }

    public String getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, Integer> getReactionCounts() {
        return reactionCounts;
    }

    public String getUserReactionType() {
        return userReactionType;
    }
}
