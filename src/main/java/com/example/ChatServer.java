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

    public void sendPrivateMessage(String sender, String receiver, String timestamp, String msg) {
        ClientHandler ch = onlineClients.get(receiver);
        if (ch != null) {
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

    public void sendGroupMessage(String group, String member, String sender, String timestamp, String msg) {
        ClientHandler ch = onlineClients.get(member);
        if (ch != null) {
            ch.send(String.format("GROUP_MSG:%s:%s:%s:%s", group, sender, timestamp, msg));
        }
    }

    public void broadcastOnlineUsers() {
        String userList = String.join(",", onlineClients.keySet());
        String msg = "USER_LIST_UPDATE:" + userList;
        for (ClientHandler ch : onlineClients.values()) {
            ch.send(msg);
        }
    }

    public void sendTypingNotification(String sender, String recipient, String status) {
        ClientHandler ch = onlineClients.get(recipient);
        if (ch != null) {
            ch.send(String.format("TYPING_NOTIF:%s:%s", sender, status));
        }
    }
    public void sendOnlineListToRequester(String requesterUsername) {
        ClientHandler ch = onlineClients.get(requesterUsername);
        if (ch == null) return;
    
        String userListStr = onlineClients.keySet().stream()
                            .filter(u -> !u.equals(requesterUsername))
                            .collect(Collectors.joining(","));
        String serverMessage;
        if (userListStr.isEmpty()) {
            serverMessage = "SERVER_MSG:Không có người dùng nào khác đang online.";
        } else {
            String userListReadable = onlineClients.keySet().stream()
                                        .filter(u -> !u.equals(requesterUsername))
                                        .collect(Collectors.joining(", "));
            serverMessage = "SERVER_MSG:Các user đang online: " + userListReadable;
        }
        ch.send(serverMessage);
    }
    public ClientHandler getClientHandler(String username) {
        return onlineClients.get(username);
    }

    public static void main(String[] args) throws Exception {
        DBHelper db = new DBHelper("jdbc:mysql://localhost:3306/chat_db", "root", "123456");
        new ChatServer(5000, db).start();
    }
}
