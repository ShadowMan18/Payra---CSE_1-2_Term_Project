package codes.Server;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FriendManager {


    public static boolean sendFriendRequest(Connection conn, String sender, String receiver) {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO FriendRequests (sender, receiver) VALUES (?, ?)")) {
            stmt.setString(1, sender);
            stmt.setString(2, receiver);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean acceptFriendRequest(Connection conn, String sender, String receiver) {
        try {
            conn.setAutoCommit(false);

            // Delete request
            try (PreparedStatement delete = conn.prepareStatement(
                    "DELETE FROM FriendRequests WHERE sender = ? AND receiver = ?")) {
                delete.setString(1, sender);
                delete.setString(2, sender + ":" + receiver);
                delete.executeUpdate();
            }

            // Add to friends (both directions)
            try (PreparedStatement insert1 = conn.prepareStatement("INSERT INTO Friends (user1, user2) VALUES (?, ?)")) {
                insert1.setString(1, sender);
                insert1.setString(2, receiver);
                insert1.executeUpdate();
            }

            try (PreparedStatement insert2 = conn.prepareStatement("INSERT INTO Friends (user1, user2) VALUES (?, ?)")) {
                insert2.setString(1, receiver);
                insert2.setString(2, sender);
                insert2.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) {}
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ex) {}
        }


    }

    public static boolean rejectFriendRequest(Connection conn, String sender, String receiver) {
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE FriendRequests SET status = 'rejected' WHERE sender = ? AND receiver = ?")) {
            stmt.setString(1, sender);
            stmt.setString(2, sender + ":" + receiver);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }



    public static List<String> getFriendList(Connection conn, String userId) {
        List<String> friends = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT user2 FROM Friends WHERE user1 = ?")) {
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                friends.add(rs.getString("user2"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Hello I am "+userId+" and I have "+friends.size()+" friends");
//        for (String s : friends) {
//            System.out.println("Myyyy friend, yayyy : " + s);
//        }

        return friends;
    }

    public static List<String> getPendingRequests(Connection conn, String userId) {
        System.out.println("Fetching pending friend requests for: " + userId);
        List<String> pending = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT sender FROM FriendRequests WHERE receiver LIKE ? AND status = 'pending'"
        )) {
            stmt.setString(1, "%:" + userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String sender = rs.getString("sender");
                    pending.add(sender);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("Pending requests count: " + pending.size());
//        for (String s : pending) {
//            System.out.println("Request from: " + s);
//        }

        return pending;
    }


    public static boolean unfriend(Connection conn, String user1, String user2) {
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM Friends WHERE (user1 = ? AND user2 = ?) OR (user1 = ? AND user2 = ?)")) {
                stmt.setString(1, user1);
                stmt.setString(2, user2);
                stmt.setString(3, user2);
                stmt.setString(4, user1);
                stmt.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) {}
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ex) {}
        }
    }



}
