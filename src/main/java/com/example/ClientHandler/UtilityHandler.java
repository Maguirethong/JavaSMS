package com.example.ClientHandler;

import com.example.Server.ChatServer;
import com.example.Server.DBHelper;
import java.sql.SQLException;
import java.util.List;

public class UtilityHandler {

    private final ClientHandler client;
    private final ChatServer server;
    private final DBHelper db;

    public UtilityHandler(ClientHandler client, ChatServer server, DBHelper db) {
        this.client = client;
        this.server = server;
        this.db = db;
    }

    public void handleListGroups() throws SQLException {
        client.sendUserGroupsList1();
    }

    public void handleListOnline() throws SQLException {
        server.sendOnlineListToRequester(client.getUsername());
    }

    public void handleListGroupMembers(String payload) throws SQLException {
        String groupName = payload;
        List<String> members = db.getGroupMembers(groupName);
        client.send("SERVER_MSG:Thành viên nhóm " + groupName + ": " + String.join(", ", members));
        client.send("SERVER_MSG:Admin là " + String.join(", ", db.getGroupAdmins(groupName)));
    }

    public void handleGetHistory(String payload) throws SQLException {
        String targetName = payload;
        String username = client.getUsername();
        String history;
        if (db.isGroup(targetName)) {
            history = db.getGroupHistory(targetName, username);
        } else {
            history = db.getPrivateHistory(username, targetName);
        }
        client.send("CHAT_HISTORY:" + targetName + ":" + history);
    }

    public void handleTypingStart(String payload) throws SQLException {
        String recipient = payload;
        server.sendTypingNotification(client.getUsername(), recipient, "START");
    }

    public void handleTypingStop(String payload) throws SQLException {
        String recipient = payload;
        server.sendTypingNotification(client.getUsername(), recipient, "STOP");
    }

    public void handleSetStatus(String payload) throws SQLException {
        String newStatus = payload;
        if (newStatus == null || newStatus.trim().isEmpty()) {
            client.setStatus("Online");
        } else {
            client.setStatus(newStatus.trim());
        }
        client.send("SERVER_MSG:Đã cập nhật trạng thái thành: " + client.getStatus());
        server.broadcastOnlineUsers();
    }
}
