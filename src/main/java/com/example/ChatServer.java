package com.example;

import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private final int port;
    private final DBHelper db;
    private final Map<String, ClientHandler> onlineClients = new ConcurrentHashMap<>();

    public ChatServer(int port, DBHelper db) {
        this.port = port;
        this.db = db;
    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Chat server started on port " + port);

        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(new ClientHandler(socket, this, db)).start();
        }
    }

    // SỬA LỖI: Đổi tên và thay đổi logic để khớp với client
    public void sendPrivateMessage(String sender, String receiver, String timestamp, String msg) {
        ClientHandler ch = onlineClients.get(receiver);
        if (ch != null) {
            // Cú pháp client mong đợi: PRIVATE_MSG:SENDER:TIMESTAMP:MESSAGE
            ch.send(String.format("PRIVATE_MSG:%s:%s:%s", sender, timestamp, msg));
        }
    }

    public void addOnlineUser(String username, ClientHandler handler) {
        onlineClients.put(username, handler);
        broadcastOnlineUsers(); // SỬA LỖI: Cập nhật danh sách cho mọi người
    }

    public void removeOnlineUser(String username) {
        onlineClients.remove(username);
        broadcastOnlineUsers(); // SỬA LỖI: Cập nhật danh sách cho mọi người
    }

    // SỬA LỖI: Cập nhật chữ ký và logic để khớp với client
    public void sendGroupMessage(String group, String member, String sender, String timestamp, String msg) {
        ClientHandler ch = onlineClients.get(member);
        if (ch != null) {
            // Cú pháp client mong đợi: GROUP_MSG:GROUP:SENDER:TIMESTAMP:MESSAGE
            ch.send(String.format("GROUP_MSG:%s:%s:%s:%s", group, sender, timestamp, msg));
        }
    }

    // SỬA LỖI: Thêm phương thức broadcast danh sách user
    public void broadcastOnlineUsers() {
        String userList = String.join(",", onlineClients.keySet());
        String msg = "USER_LIST_UPDATE:" + userList;
        for (ClientHandler ch : onlineClients.values()) {
            ch.send(msg);
        }
    }

    // SỬA LỖI: Thêm phương thức gửi thông báo typing
    public void sendTypingNotification(String sender, String recipient, String status) {
        ClientHandler ch = onlineClients.get(recipient);
        if (ch != null) {
            ch.send(String.format("TYPING_NOTIF:%s:%s", sender, status));
        }
    }

    // SỬA LỖI: Thêm phương thức helper để lấy handler
    public ClientHandler getClientHandler(String username) {
        return onlineClients.get(username);
    }

    public static void main(String[] args) throws Exception {
        // Hãy chắc chắn rằng DB 'chat_db' đã được tạo và tài khoản/mật khẩu là chính xác
        DBHelper db = new DBHelper("jdbc:mysql://localhost:3306/chat_db", "root", "123456");
        new ChatServer(5000, db).start();
    }
}