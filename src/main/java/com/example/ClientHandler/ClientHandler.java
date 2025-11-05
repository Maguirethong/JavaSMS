package com.example.ClientHandler;

import com.example.Server.ChatServer;
import com.example.Server.DBHelper;
import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private final DBHelper db;
    private BufferedReader in;
    private PrintWriter out;
    private String username = null;
    private String status = "Đang rảnh";

    private final AuthenticationHandler authHandler;
    private final GroupManagementHandler groupHandler;
    private final MessagingHandler messagingHandler;
    private final UtilityHandler utilityHandler;

    public ClientHandler(Socket socket, ChatServer server, DBHelper db) {
        this.socket = socket;
        this.server = server;
        this.db = db;
        
        this.authHandler = new AuthenticationHandler(this, server, db);
        this.groupHandler = new GroupManagementHandler(this, server, db);
        this.messagingHandler = new MessagingHandler(this, server, db);
        this.utilityHandler = new UtilityHandler(this, server, db);
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                try {
                    String[] parts = line.split(":", 2);
                    String commandKey = parts[0];
                    String payload = (parts.length > 1) ? parts[1] : "";

                    if (username == null) {
                        authHandler.handleCommand(commandKey, payload);
                        continue; 
                    }

                    switch (commandKey) {
                        case "MSG":
                            messagingHandler.handlePrivateMessage(payload);
                            break;
                        case "GROUP_MSG":
                            messagingHandler.handleGroupMessage(payload);
                            break;
                        
                        case "CREATE_GROUP":
                            groupHandler.handleCreateGroup(payload);
                            break;
                        case "JOIN_GROUP":
                            groupHandler.handleJoinGroup(payload);
                            break;
                        case "LEAVE_GROUP":
                            groupHandler.handleLeaveGroup(payload);
                            break;
                        case "INVITE_USER":
                            groupHandler.handleInviteUser(payload);
                            break;
                        case "KICK_USER":
                            groupHandler.handleKickUser(payload);
                            break;
                        case "SET_ROLE":
                            groupHandler.handleSetRole(payload);
                            break;
                        
                        case "LIST_GROUPS":
                            utilityHandler.handleListGroups();
                            break;
                        case "ONLINE":
                            utilityHandler.handleListOnline();
                            break;
                        case "LIST_GROUP_MEMBERS":
                            utilityHandler.handleListGroupMembers(payload);
                            break;
                        case "GET_HISTORY":
                            utilityHandler.handleGetHistory(payload);
                            break;
                        case "TYPING_START":
                            utilityHandler.handleTypingStart(payload);
                            break;
                        case "TYPING_STOP":
                            utilityHandler.handleTypingStop(payload);
                            break;
                        case "SET_STATUS":
                            utilityHandler.handleSetStatus(payload);
                            break;
                        
                        case "LOGOUT":
//                            send("SERVER_MSG:Bạn đã đăng xuất.");
                            return; 

                        default:
                    }

                } catch (SQLException e) {
                    System.err.println("Database error for user " + username + ": " + e.getMessage());
                    send("SERVER_MSG:Đã xảy ra lỗi phía server (Database).");
                } catch (Exception e) {
                    System.err.println("Processing error for user " + username + " on line " + line + ": " + e.getMessage());
                    e.printStackTrace();
                    send("SERVER_MSG:Đã xảy ra lỗi khi xử lý yêu cầu của bạn.");
                }
            }
        } catch (IOException e) {
        } finally {
            try {
                if (username != null) {
                    try {
                        db.setLastSeen(username);
                    } catch (SQLException e_db) {
                        System.err.println("Không thể cập nhật last_seen cho " + username + ": " + e_db.getMessage());
                    }
                    server.removeOnlineUser(username);
                    System.out.println("Client disconnected: " + username);
                }
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    public void send(String msg) {
        out.println(msg);
    }
    
    public void sendUserGroupsList() {
        if (username == null) return;
        try {
            List<String> groups = db.listGroups(username);
            send("USER_GROUPS_UPDATE:" + String.join(",", groups));
        } catch (SQLException e) {
            send("SERVER_MSG:Lỗi khi lấy danh sách nhóm: " + e.getMessage());
        }
    }
    
    public void sendUserGroupsList1() {
        if (username == null) return;
        try {
            List<String> groups = db.listAllGroups();
            send("SERVER_MSG:Các nhóm có sẵn: " + String.join(",", groups));
        } catch (SQLException e) {
            send("SERVER_MSG:Lỗi khi lấy danh sách nhóm: " + e.getMessage());
        }
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return this.username;
    }
    
    public String getStatus() {
        return this.status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}