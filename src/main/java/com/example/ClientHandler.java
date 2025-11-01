package com.example;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private final DBHelper db;
    private BufferedReader in;
    private PrintWriter out;
    private String username = null;

    public ClientHandler(Socket socket, ChatServer server, DBHelper db) {
        this.socket = socket;
        this.server = server;
        this.db = db;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                try {
                    if (username == null) {
                        // Chỉ cho phép LOGIN hoặc REGISTER nếu chưa đăng nhập
                        if (line.startsWith("REGISTER:")) {
                            String[] p = line.split(":", 3);
                            if (p.length == 3 && db.registerUser(p[1], p[2])) {
                                out.println("REGISTER_SUCCESS:Đăng ký thành công. Vui lòng đăng nhập.");
                            } else {
                                out.println("REGISTER_FAIL:Đăng ký thất bại (Tên đăng nhập có thể đã tồn tại).");
                            }
                        } else if (line.startsWith("LOGIN:")) {
                            String[] p = line.split(":", 3);
                            if (p.length == 3 && db.checkLogin(p[1], p[2])) {
                                username = p[1];
                                server.addOnlineUser(username, this);
                                // SỬA LỖI: Client mong đợi "LOGIN_SUCCESS:username"
                                out.println("LOGIN_SUCCESS:" + username);
                                // SỬA LỖI: Gửi danh sách nhóm ngay khi đăng nhập
                                sendUserGroupsList();
                            } else {
                                out.println("LOGIN_FAIL:Tên đăng nhập hoặc mật khẩu không chính xác.");
                            }
                        }
                        continue; // Bỏ qua các lệnh khác nếu chưa đăng nhập
                    }

                    // Các lệnh yêu cầu đã đăng nhập
                    if (line.startsWith("MSG:")) { // SỬA LỖI: Client gửi "MSG:", không phải "SEND:"
                        String[] p = line.split(":", 3);
                        String recipient = p[1];
                        String content = p[2];
                        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());

                        db.saveMessage(username, recipient, content);
                        // Gửi cho người nhận
                        server.sendPrivateMessage(username, recipient, timestamp, content);
                        // Gửi xác nhận cho người gửi (Client cần lệnh này)
                        out.println(String.format("MSG_SENT:%s:%s:%s", recipient, timestamp, content));

                    } else if (line.startsWith("GROUP_MSG:")) { // SỬA LỖI: Client gửi "GROUP_MSG:", không phải "SEND_GROUP:"
                        String[] parts = line.split(":", 3);
                        String group = parts[1];
                        String msg = parts[2];
                        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());

                        db.saveGroupMessage(group, username, msg);
                        for (String member : db.getGroupMembers(group)) {
                            server.sendGroupMessage(group, member, username, timestamp, msg);
                        }
                    } else if (line.startsWith("CREATE_GROUP:")) {
                        String group = line.split(":", 2)[1];
                        if (db.createGroup(group, username)) {
                            db.joinGroup(group, username); // Tự động join nhóm vừa tạo
                            // SỬA LỖI: Client mong đợi "NEW_GROUP:"
                            out.println("NEW_GROUP:" + group);
                            sendUserGroupsList(); // Cập nhật danh sách nhóm
                        } else {
                            out.println("SERVER_MSG:Tạo nhóm thất bại (tên có thể đã tồn tại).");
                        }
                    } else if (line.startsWith("JOIN_GROUP:")) {
                        String group = line.split(":", 2)[1];
                        if (db.joinGroup(group, username)) {
                            // SỬA LỖI: Client mong đợi "INVITED_TO_GROUP:"
                            out.println("INVITED_TO_GROUP:" + group);
                            sendUserGroupsList(); // Cập nhật danh sách nhóm
                        } else {
                            out.println("SERVER_MSG:Vào nhóm thất bại (nhóm không tồn tại).");
                        }
                    } else if (line.equals("LIST_GROUPS")) {
                        // SỬA LỖI: Gửi theo định dạng client mong muốn
                        sendUserGroupsList();

                        // --- SỬA LỖI: Thêm các handler còn thiếu ---

                    } else if (line.equals("ONLINE")) {
                         server.sendOnlineListToRequester(username);

                    } else if (line.startsWith("LIST_GROUP_MEMBERS:")) {
                        String groupName = line.split(":", 2)[1];
                        List<String> members = db.getGroupMembers(groupName);
                        out.println("SERVER_MSG:Thành viên nhóm " + groupName + ": " + String.join(", ", members));

                    } else if (line.startsWith("LEAVE_GROUP:")) {
                        String groupName = line.split(":", 2)[1];
                        if (db.leaveGroup(groupName, username)) {
                            out.println("SERVER_MSG:Bạn đã rời nhóm " + groupName);
                            sendUserGroupsList(); // Cập nhật danh sách
                        } else {
                            out.println("SERVER_MSG:Rời nhóm thất bại (bạn không ở trong nhóm).");
                        }
                    } else if (line.startsWith("GET_HISTORY:")) {
                        String targetName = line.split(":", 2)[1];
                        String history;
                        if (db.isGroup(targetName)) {
                            history = db.getGroupHistory(targetName, username);
                        } else {
                            history = db.getPrivateHistory(username, targetName);
                        }
                        out.println("CHAT_HISTORY:" + targetName + ":" + history);

                    } else if (line.startsWith("TYPING_START:")) {
                        String recipient = line.split(":", 2)[1];
                        server.sendTypingNotification(username, recipient, "START");

                    } else if (line.startsWith("TYPING_STOP:")) {
                        String recipient = line.split(":", 2)[1];
                        server.sendTypingNotification(username, recipient, "STOP");

                    } else if (line.startsWith("INVITE_USER:")) {
                        String[] parts = line.split(":", 3);
                        String groupName = parts[1];
                        String userToInvite = parts[2];

                        // (Nên kiểm tra quyền admin, nhưng tạm thời bỏ qua)
                        if (!db.getGroupMembers(groupName).contains(username)) {
                            out.println("SERVER_MSG:Bạn không phải là thành viên của nhóm này.");
                        } else if (db.joinGroup(groupName, userToInvite)) {
                            out.println("SERVER_MSG:Đã mời " + userToInvite + " vào nhóm " + groupName);
                            // Thông báo cho người được mời
                            ClientHandler invitedHandler = server.getClientHandler(userToInvite);
                            if (invitedHandler != null) {
                                invitedHandler.send("INVITED_TO_GROUP:" + groupName);
                                invitedHandler.sendUserGroupsList(); // Cập nhật DS nhóm của họ
                            }
                        } else {
                            out.println("SERVER_MSG:Mời thất bại (người dùng đã ở trong nhóm).");
                        }
                    } else if (line.startsWith("KICK_USER:")) {
                        String[] parts = line.split(":", 3);
                        String groupName = parts[1];
                        String userToKick = parts[2];

                        // (Cần kiểm tra quyền admin)
                        if (db.leaveGroup(groupName, userToKick)) {
                            out.println("SERVER_MSG:Đã xóa " + userToKick + " khỏi nhóm " + groupName);
                            // Thông báo cho người bị kick
                            ClientHandler kickedHandler = server.getClientHandler(userToKick);
                            if (kickedHandler != null) {
                                kickedHandler.send("KICKED_FROM_GROUP:" + groupName);
                                kickedHandler.sendUserGroupsList(); // Cập nhật DS nhóm của họ
                            }
                        } else {
                            out.println("SERVER_MSG:Xóa thất bại (người dùng không ở trong nhóm).");
                        }
                    } else if (line.startsWith("SET_STATUS:")) {
                        out.println("SERVER_MSG:Đã cập nhật trạng thái (tính năng đang phát triển).");
                    }

                } catch (SQLException e) {
                    System.err.println("Database error for user " + username + ": " + e.getMessage());
                    out.println("SERVER_MSG:Đã xảy ra lỗi phía server (Database).");
                } catch (Exception e) {
                    System.err.println("Processing error for user " + username + " on line " + line + ": " + e.getMessage());
                    out.println("SERVER_MSG:Đã xảy ra lỗi khi xử lý yêu cầu của bạn.");
                }
            }
        } catch (IOException e) {
            // Client ngắt kết nối
        } finally {
            try {
                if (username != null) {
                    server.removeOnlineUser(username);
                    System.out.println("Client disconnected: " + username);
                }
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private void sendUserGroupsList() {
        if (username == null) return;
        try {
            List<String> groups = db.listGroups(username);
              out.println("SERVER_MSG:Các nhóm của bạn: " + String.join(",", groups));

        } catch (SQLException e) {
            out.println("SERVER_MSG:Lỗi khi lấy danh sách nhóm: " + e.getMessage());
        }
    }

    public void send(String msg) {
        out.println(msg);
    }
}
