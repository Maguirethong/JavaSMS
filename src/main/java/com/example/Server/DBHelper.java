package com.example.Server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DBHelper {
    private final String url;
    private final String user;
    private final String pass;

    public DBHelper(String url, String user, String pass) {
        this.url = url;
        this.user = user;
        this.pass = pass;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, pass);
    }

    public boolean registerUser(String username, String password) {
        String sql = "INSERT INTO users(username, password) VALUES (?, ?)";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean checkLogin(String username, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE username=? AND password=?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    public void saveMessage(String sender, String receiver, String content) throws SQLException {
        String sql = "INSERT INTO messages(sender, receiver, content) VALUES (?, ?, ?)";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.setString(3, content);
            ps.executeUpdate();
        }
    }

    public boolean createGroup(String groupName, String creator) throws SQLException {
        String sql = "INSERT INTO chat_groups(group_name, created_by) VALUES (?, ?)";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, groupName);
            ps.setString(2, creator);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean joinGroup(String groupName, String username, String role) throws SQLException {
    // Sửa SQL để thêm 'role'
    String sql = "INSERT IGNORE INTO group_members(group_id, username, role) " +
            "SELECT group_id, ?, ? FROM chat_groups WHERE group_name=?";
    try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setString(1, username);
        ps.setString(2, role); // Gán vai trò (vd: "admin" hoặc "member")
        ps.setString(3, groupName);
        return ps.executeUpdate() > 0;
    }
}

    public boolean leaveGroup(String groupName, String username) throws SQLException {
        String sql = "DELETE gm FROM group_members gm " +
                "JOIN chat_groups g ON g.group_id = gm.group_id " +
                "WHERE g.group_name = ? AND gm.username = ?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, groupName);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        }
    }

    public void saveGroupMessage(String groupName, String sender, String content) throws SQLException {
        String sql = "INSERT INTO group_messages(group_id, sender, content) " +
                "SELECT group_id, ?, ? FROM chat_groups WHERE group_name=?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sender);
            ps.setString(2, content);
            ps.setString(3, groupName);
            ps.executeUpdate();
        }
    }

    public List<String> getGroupMembers(String groupName) throws SQLException {
        String sql = "SELECT username FROM group_members gm " +
                "JOIN chat_groups g ON g.group_id = gm.group_id WHERE g.group_name=?";
        List<String> members = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, groupName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) members.add(rs.getString("username"));
        }
        return members;
    }

    public List<String> listGroups(String username) throws SQLException {
        String sql = "SELECT g.group_name FROM chat_groups g " +
                "JOIN group_members gm ON g.group_id = gm.group_id " +
                "WHERE gm.username=?";
        List<String> groups = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) groups.add(rs.getString("group_name"));
        }
        return groups;
    }
    public List<String> listAllGroups() throws SQLException {
        String sql = "SELECT group_name FROM chat_groups ORDER BY group_name ASC";
        List<String> groups = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) groups.add(rs.getString("group_name"));
        }
        return groups;
    }
    public boolean isGroup(String name) throws SQLException {
        String sql = "SELECT 1 FROM chat_groups WHERE group_name = ?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            return ps.executeQuery().next();
        }
    }

    public String getPrivateHistory(String user1, String user2) throws SQLException {
        String sql = "SELECT sender, content, DATE_FORMAT(sent_at, '%d/%m/%Y %H:%i:%s') as time " +
                "FROM messages " +
                "WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?) " +
                "ORDER BY sent_at ASC LIMIT 100";
        StringBuilder history = new StringBuilder();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user1);
            ps.setString(2, user2);
            ps.setString(3, user2);
            ps.setString(4, user1);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String sender = rs.getString("sender");
                String formattedSender = sender.equals(user1) ? "Bạn" : sender;
                history.append(String.format("(%s) [%s]: %s",
                rs.getString("time"), formattedSender, rs.getString("content")));
                if (!rs.isLast()) {
                    history.append("\n");
                }
            }
        }
        return history.toString().replace("\n", "\\n");
    }

    public String getGroupHistory(String groupName, String currentUsername) throws SQLException {
        String sql = "SELECT gm.sender, gm.content, DATE_FORMAT(gm.sent_at, '%d/%m/%Y %H:%i:%s') as time " +
                "FROM group_messages gm " +
                "JOIN chat_groups g ON g.group_id = gm.group_id " +
                "WHERE g.group_name = ? " +
                "ORDER BY gm.sent_at ASC LIMIT 100";
        StringBuilder history = new StringBuilder();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, groupName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String sender = rs.getString("sender");
                String formattedSender = sender.equals(currentUsername) ? "Bạn" : sender;
                history.append(String.format("(%s) [%s]: %s",
                rs.getString("time"), formattedSender, rs.getString("content")));
                if (!rs.isLast()) {
                    history.append("\n");
                }
            }
        }
        return history.toString().replace("\n", "\\n");
    }
    public String getRoleInGroup(String username, String groupName) throws SQLException {
        String sql = "SELECT gm.role FROM group_members gm " +
                     "JOIN chat_groups g ON g.group_id = gm.group_id " +
                     "WHERE gm.username = ? AND g.group_name = ?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, groupName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }
            return null;
        }
    }
    public List<String> getGroupAdmins(String groupName) throws SQLException {
        String sql = "SELECT username FROM group_members gm " +
                     "JOIN chat_groups g ON g.group_id = gm.group_id " +
                     "WHERE g.group_name=? AND gm.role = 'admin'";
        List<String> admins = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, groupName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                admins.add(rs.getString("username"));
            }
        }
        return admins;
    }
    public boolean setGroupRole(String username, String groupName, String newRole) throws SQLException {
        String sql = "UPDATE group_members gm " +
                     "JOIN chat_groups g ON g.group_id = gm.group_id " +
                     "SET gm.role = ? " +
                     "WHERE gm.username = ? AND g.group_name = ?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newRole); // "admin" hoặc "member"
            ps.setString(2, username);
            ps.setString(3, groupName);
            return ps.executeUpdate() > 0;
        }
    }
    public List<String> getAllUsers() throws SQLException {
        String sql = "SELECT username FROM users WHERE username != 'Hệ thống' ORDER BY username ASC";
        List<String> users = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(rs.getString("username"));
            }
        }
        return users;
    }
    public void setLastSeen(String username) throws SQLException {
        String sql = "UPDATE users SET last_seen = CURRENT_TIMESTAMP WHERE username = ?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
    }
    public List<String> getUnreadGroups(String username) throws SQLException {
        String sql = "SELECT DISTINCT g.group_name " +
                     "FROM chat_groups g " +
                     "JOIN group_members gm ON g.group_id = gm.group_id " +
                     "JOIN group_messages gmsg ON g.group_id = gmsg.group_id " +
                     "JOIN users u ON gm.username = u.username " +
                     "WHERE gm.username = ? " +
                     "  AND gmsg.sent_at > u.last_seen " +
                     "  AND gmsg.sender != ?"; // Không tính tin nhắn của chính mình

        List<String> unreadGroups = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                unreadGroups.add(rs.getString("group_name"));
            }
        }
        return unreadGroups;
    }
    public List<String> getUnreadSenders(String username) throws SQLException {
        String sql = "SELECT DISTINCT m.sender " +
                     "FROM messages m " +
                     "JOIN users u ON m.receiver = u.username " +
                     "WHERE m.receiver = ? " +
                     "  AND m.sent_at > u.last_seen";

        List<String> unreadSenders = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                unreadSenders.add(rs.getString("sender"));
            }
        }
        return unreadSenders;
    }
}