package codes;

import java.io.Serializable;
import java.time.LocalDateTime;

public class CommentPacket implements Serializable {
    private int postId;
    private String commenter;
    private String commentText;
    private LocalDateTime timestamp;

    public CommentPacket(int postId, String commenter, String commentText, LocalDateTime timestamp) {
        this.postId = postId;
        this.commenter = commenter;
        this.commentText = commentText;
        this.timestamp = timestamp;
    }

    public int getPostId() {
        return postId;
    }
    public String getCommenter() {
        return commenter;
    }
    public String getCommentText() {
        return commentText;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
