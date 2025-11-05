package com.example.ClientHandler;

import com.example.Server.ChatServer;

import com.example.Server.DBHelper;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class GroupManagementHandler {

    private final ClientHandler client;
    private final ChatServer server;
    private final DBHelper db;

    public GroupManagementHandler(ClientHandler client, ChatServer server, DBHelper db) {
        this.client = client;
        this.server = server;
        this.db = db;
    }

    public void handleCreateGroup(String payload) throws SQLException {
        String group = payload;
        String creator = client.getUsername();
        
        if (db.createGroup(group, creator)) {
            db.joinGroup(group, creator, "admin");
            db.joinGroup(group, "Hệ thống", "member");
            client.send("NEW_GROUP:" + group);
            client.sendUserGroupsList();
        } else {
            client.send("SERVER_MSG:Tạo nhóm thất bại (tên có thể đã tồn tại).");
        }
    }

    public void handleJoinGroup(String payload) throws SQLException {
        String groupName = payload;
        String username = client.getUsername();

        if (db.joinGroup(groupName, username, "member")) {
            client.send("SERVER_MSG:Bạn đã tham gia nhóm " + groupName);
            client.sendUserGroupsList();
            
            String sender = "Hệ thống";
            String systemMessage = username + " đã tham gia vào nhóm.";
            String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
            
            db.saveGroupMessage(groupName, sender, systemMessage);

            List<String> members = db.getGroupMembers(groupName);
            for (String member : members) {
                server.sendGroupMessage(groupName, member, sender, timestamp, systemMessage);
            }
        } else {
            client.send("SERVER_MSG:Vào nhóm thất bại (nhóm không tồn tại hoặc bạn đã ở trong nhóm).");
        }
    }

    public void handleLeaveGroup(String payload) throws SQLException {
        String groupName = payload;
        String username = client.getUsername();
        String role = db.getRoleInGroup(username, groupName);

        if (!db.leaveGroup(groupName, username)) {
            client.send("SERVER_MSG:Rời nhóm thất bại (bạn không ở trong nhóm).");
            return;
        }
        
        client.send("SERVER_MSG:Bạn đã rời khỏi nhóm " + groupName);
        client.sendUserGroupsList();
        
        String sen = "Hệ thống";
        String systemMes = username + " đã rời khỏi nhóm.";
        String timestam = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
        db.saveGroupMessage(groupName, sen, systemMes);
        
        List<String> members = db.getGroupMembers(groupName);
        for (String member : members) {
            server.sendGroupMessage(groupName, member, sen, timestam, systemMes);
        }

        if ("admin".equals(role)) {
            List<String> remainingAdmins = db.getGroupAdmins(groupName);
            if (remainingAdmins.isEmpty()) {
                List<String> remainingMembers = db.getGroupMembers(groupName);
                if (!remainingMembers.isEmpty() && !remainingMembers.get(0).equals("Hệ thống")) {
                    String newAdmin = remainingMembers.get(0); 
                    db.setGroupRole(newAdmin, groupName, "admin");
                    
                    String systemMessage = newAdmin + " đã được tự động thăng làm admin mới.";
                    String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
                    db.saveGroupMessage(groupName, sen, systemMessage);

                    for (String member : remainingMembers) {
                        server.sendGroupMessage(groupName, member, sen, timestamp, systemMessage);
                    }
                }
            }
        }
    }

    public void handleInviteUser(String payload) throws SQLException {
        String[] parts = payload.split(":", 2);
        if (parts.length < 2) return;
        
        String groupName = parts[0];
        String userToInvite = parts[1];
        String requesterUsername = client.getUsername();

        String role = db.getRoleInGroup(requesterUsername, groupName);
        if (!"admin".equals(role)) {
            client.send("SERVER_MSG:Bạn không có quyền admin để mời thành viên.");
            return;
        }

        if (db.joinGroup(groupName, userToInvite, "member")) {
            ClientHandler invitedHandler = server.getClientHandler(userToInvite);
            if (invitedHandler != null) {
                invitedHandler.send("INVITED_TO_GROUP:" + groupName);
                invitedHandler.sendUserGroupsList();
            }

            String sender = "Hệ thống";
            String systemMessage = requesterUsername + " đã mời " + userToInvite + " vào nhóm.";
            String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
            db.saveGroupMessage(groupName, sender, systemMessage);

            List<String> members = db.getGroupMembers(groupName);
            for (String member : members) {
                server.sendGroupMessage(groupName, member, sender, timestamp, systemMessage);
            }
        } else {
            client.send("SERVER_MSG:Mời thất bại (người dùng đã ở trong nhóm).");
        }
    }

    public void handleKickUser(String payload) throws SQLException {
        String[] parts = payload.split(":", 2);
        if (parts.length < 2) return;

        String groupName = parts[0];
        String userToKick = parts[1];
        String requesterUsername = client.getUsername();

        String role = db.getRoleInGroup(requesterUsername, groupName);
        String role1 = db.getRoleInGroup(userToKick, groupName);
        if (!"admin".equals(role)) {
            client.send("SERVER_MSG:Bạn không có quyền admin để xóa thành viên.");
            return;
        }

        if (userToKick.equals(requesterUsername) || userToKick.equals("Hệ thống") || role1.equals("admin")) {
            client.send("SERVER_MSG:Bạn không thể kick.");
            return;
        }

        List<String> membersToNotify = db.getGroupMembers(groupName);

        if (db.leaveGroup(groupName, userToKick)) {
            ClientHandler kickedHandler = server.getClientHandler(userToKick);
            if (kickedHandler != null) {
                kickedHandler.send("KICKED_FROM_GROUP:" + groupName);
                kickedHandler.sendUserGroupsList();
            }

            String sender = "Hệ thống";
            String systemMessage = requesterUsername + " đã xóa " + userToKick + " khỏi nhóm.";
            String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
            db.saveGroupMessage(groupName, sender, systemMessage);

            for (String member : membersToNotify) {
                if (!member.equals(userToKick)) {
                    server.sendGroupMessage(groupName, member, sender, timestamp, systemMessage);
                }
            }
        } else {
            client.send("SERVER_MSG:Xóa thất bại (người dùng không ở trong nhóm).");
        }
    }

    public void handleSetRole(String payload) throws SQLException {
        String[] parts = payload.split(":", 3);
        if (parts.length != 3) return;
        
        String groupName = parts[0];
        String targetUser = parts[1];
        String newRole = parts[2];
        String requesterUsername = client.getUsername();

        String requesterRole = db.getRoleInGroup(requesterUsername, groupName);
        if (!"admin".equals(requesterRole)) {
            client.send("SERVER_MSG:Chỉ admin mới có quyền thay đổi vai trò.");
            return;
        }
        String Role = db.getRoleInGroup(targetUser, groupName);
        if (Role.equals("admin")) client.send("SERVER_MSG:Không thể thay đổi vai trò admin.");
        else {
            List<String> membersToNotify = db.getGroupMembers(groupName);
            if (db.setGroupRole(targetUser, groupName, newRole)) {
                client.send("SERVER_MSG:Đã cập nhật vai trò của " + targetUser + " thành " + newRole + ".");

                ClientHandler targetHandler = server.getClientHandler(targetUser);
                if (targetHandler != null) {
                    targetHandler.send("SERVER_MSG:Vai trò của bạn trong nhóm " + groupName + " đã bị thay đổi thành " + newRole + ".");
                }

                String sender = "Hệ thống";
                String systemMessage = "Vai trò của " + targetUser + " trong nhóm " + groupName + " đã bị thay đổi thành " + newRole + ".";
                String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
                db.saveGroupMessage(groupName, sender, systemMessage); 

                for (String member : membersToNotify) {
                    server.sendGroupMessage(groupName, member, sender, timestamp, systemMessage);
                }
            } else {
                client.send("SERVER_MSG:Cập nhật vai trò thất bại (người dùng không trong nhóm).");
            }
        }
    }
}