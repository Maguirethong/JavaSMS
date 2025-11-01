package com.example;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.*;
import java.security.KeyStore;
import java.io.FileInputStream;

public class ChatServer {
    private final int port;
    private final DBHelper db;
    private final Map<String, ClientHandler> onlineClients = new ConcurrentHashMap<>();

    public ChatServer(int port, DBHelper db) {
        this.port = port;
        this.db = db;
    }
    private SSLContext createSSLContext() throws Exception {
        // 1. Tải KeyStore của Server (chứa khóa riêng)
        char[] password = "123456".toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        // Đảm bảo tệp "server.jks" nằm ở thư mục gốc của dự án
        try (FileInputStream fis = new FileInputStream("server.jks")) {
            ks.load(fis, password);
        }

        // 2. Thiết lập KeyManagerFactory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, password);

        // 3. Khởi tạo SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS"); // Dùng TLS
        sslContext.init(kmf.getKeyManagers(), null, null);
        
        return sslContext;
    }
    public void start() throws Exception { // Sửa: throws Exception
        
        // 1. Khởi tạo SSL Context
        SSLContext sslContext;
        try {
            sslContext = createSSLContext();
        } catch (Exception e) {
            System.err.println("Không thể tạo SSL Context: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // 2. Lấy SSL Server Socket Factory
        SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
        
        // 3. Tạo SSL Server Socket thay vì ServerSocket thường
        ServerSocket serverSocket = ssf.createServerSocket(port);
        System.out.println("Chat server (SSL) started on port " + port);

        while (true) {
            Socket socket = serverSocket.accept(); // Sẽ tự động trả về một SSLSocket
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
        List<String> usersWithStatus = new ArrayList<>();
        for (Map.Entry<String, ClientHandler> entry : onlineClients.entrySet()) {
            String username = entry.getKey();
            String status = entry.getValue().getStatus(); 
            usersWithStatus.add(String.format("%s (%s)", username, status)); // VD: "hieu (Đang bận)"
        }
        String userList = String.join(",", usersWithStatus);
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
        List<String> usersWithStatus = new ArrayList<>();
        for (Map.Entry<String, ClientHandler> entry : onlineClients.entrySet()) {
            String username = entry.getKey();
            String status = entry.getValue().getStatus(); 
            usersWithStatus.add(String.format("%s (%s)", username, status)); // VD: "hieu (Đang bận)"
        }
        String userListStr = String.join(",", usersWithStatus);
        ch.send("USER_LIST_UPDATE:" + userListStr);
        String serverMessage;
        if (usersWithStatus.isEmpty()) {
            serverMessage = "SERVER_MSG:Không có người dùng nào khác đang online.";
        } else {
            // Dùng ", " để danh sách in ra tab Server đẹp hơn
            String userListReadable = String.join(", ", usersWithStatus);
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
