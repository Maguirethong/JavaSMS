package com.example.ClientHandler;
import com.example.Server.ChatServer;

import com.example.Server.DBHelper;

import java.sql.SQLException;
import java.util.List;

public class AuthenticationHandler {
    
    private final ClientHandler client;
    private final ChatServer server;
    private final DBHelper db;

    public AuthenticationHandler(ClientHandler client, ChatServer server, DBHelper db) {
        this.client = client;
        this.server = server;
        this.db = db;
    }

    public void handleCommand(String commandKey, String payload) throws SQLException {
        
        if ("REGISTER".equals(commandKey)) {
            String[] p = payload.split(":", 2);
            if (p.length == 2 && db.registerUser(p[0], p[1])) {
                client.send("REGISTER_SUCCESS:Đăng ký thành công. Vui lòng đăng nhập.");
            } else {
                client.send("REGISTER_FAIL:Đăng ký thất bại (Tên đăng nhập có thể đã tồn tại).");
            }
            
        } else if ("LOGIN".equals(commandKey)) {
            String[] p = payload.split(":", 2);
            if (p.length == 2 && db.checkLogin(p[0], p[1])) {
                String username = p[0];
                
                client.setUsername(username);
                server.addOnlineUser(username, client);
                
                client.send("LOGIN_SUCCESS:" + username);
                client.sendUserGroupsList(); 
                
                List<String> unreadGroups = db.getUnreadGroups(username);
                if (!unreadGroups.isEmpty()) {
                    client.send("UNREAD_GROUPS:" + String.join(",", unreadGroups));
                }
                List<String> unreadSenders = db.getUnreadSenders(username);
                if (!unreadSenders.isEmpty()) {
                    client.send("UNREAD_SENDERS:" + String.join(",", unreadSenders));
                }
            } else {
                client.send("LOGIN_FAIL:Tên đăng nhập hoặc mật khẩu không chính xác.");
            }
        }
    }
}