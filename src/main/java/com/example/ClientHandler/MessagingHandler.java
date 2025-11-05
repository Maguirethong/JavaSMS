package com.example.ClientHandler;

import com.example.Server.ChatServer;
import com.example.Server.DBHelper;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MessagingHandler {

    private final ClientHandler client;
    private final ChatServer server;
    private final DBHelper db;

    public MessagingHandler(ClientHandler client, ChatServer server, DBHelper db) {
        this.client = client;
        this.server = server;
        this.db = db;
    }

    public void handlePrivateMessage(String payload) throws SQLException {
        String[] p = payload.split(":", 2);
        if (p.length < 2) return;

        String recipient = p[0];
        String content = p[1];
        String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
        String username = client.getUsername();

        db.saveMessage(username, recipient, content);
        server.sendPrivateMessage(username, recipient, timestamp, content);
        client.send(String.format("MSG_SENT:%s:%s:%s", recipient, timestamp, content));
    }

    public void handleGroupMessage(String payload) throws SQLException {
        String[] parts = payload.split(":", 2);
        if (parts.length < 2) return;

        String group = parts[0];
        String msg = parts[1];
        String username = client.getUsername();
        String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());

        db.saveGroupMessage(group, username, msg);
        for (String member : db.getGroupMembers(group)) {
            server.sendGroupMessage(group, member, username, timestamp, msg);
        }
    }
}
