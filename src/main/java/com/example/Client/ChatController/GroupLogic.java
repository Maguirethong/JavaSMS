package com.example.Client.ChatController;

import com.example.Client.ChatClientGUI;
import com.example.Client.ChatNetworkService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import java.util.Optional;

public class GroupLogic {

    private final ChatClientGUI view;
    private final ChatNetworkService networkService;
    private final ChatController controller;

    public GroupLogic(ChatController controller, ChatClientGUI view, ChatNetworkService networkService) {
        this.controller = controller;
        this.view = view;
        this.networkService = networkService;
    }

    public void handleListGroupMembers() {
        String selectedGroup = view.getMyGroupsList().getSelectionModel().getSelectedItem();
        TextInputDialog dialog = new TextInputDialog(selectedGroup != null ? selectedGroup : "");
        dialog.setTitle("Xem Thành Viên Nhóm");
        dialog.setHeaderText("Nhập tên nhóm bạn muốn xem thành viên.");
        dialog.setContentText("Tên nhóm:");
        dialog.showAndWait().ifPresent(groupName -> {
            if (groupName != null && !groupName.trim().isEmpty()) {
                controller.sendCommandAndShowServerTab("LIST_GROUP_MEMBERS:" + groupName.trim());
            }
        });
    }

    public void handleLeaveGroup() {
        String selectedGroup = view.getMyGroupsList().getSelectionModel().getSelectedItem();
        TextInputDialog dialog = new TextInputDialog(selectedGroup != null ? selectedGroup : "");
        dialog.setTitle("Rời Nhóm");
        dialog.setHeaderText("Nhập tên nhóm bạn muốn rời.");
        dialog.setContentText("Tên nhóm:");
        dialog.showAndWait().ifPresent(groupName -> {
            if (groupName != null && !groupName.trim().isEmpty()) {
                networkService.sendMessageToServer("LEAVE_GROUP:" + groupName.trim());
            }
        });
    }

    public void handleJoinGroup() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Tham Gia Nhóm");
        dialog.setHeaderText("Nhập tên nhóm bạn muốn tham gia.");
        dialog.setContentText("Tên nhóm:");
        dialog.showAndWait().ifPresent(groupName -> {
            if (groupName != null && !groupName.trim().isEmpty()) {
                networkService.sendMessageToServer("JOIN_GROUP:" + groupName.trim());
            }
        });
    }

    public void handleCreateGroup() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Tạo Nhóm Mới");
        dialog.setHeaderText("Nhập tên cho nhóm bạn muốn tạo.");
        dialog.setContentText("Tên nhóm:");
        dialog.showAndWait().ifPresent(groupName -> {
            if (groupName != null && !groupName.trim().isEmpty()) {
                networkService.sendMessageToServer("CREATE_GROUP:" + groupName.trim());
            }
        });
    }

    public void handleInviteUser() {
        String selectedGroup = view.getMyGroupsList().getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            controller.appendMessageToArea("SERVER", "Vui lòng chọn một nhóm từ danh sách 'My Groups' trước.", false);
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Mời Thành Viên");
        dialog.setHeaderText("Bạn sắp mời một thành viên vào nhóm: " + selectedGroup);
        dialog.setContentText("Nhập tên người dùng (username) muốn mời:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(usernameToInvite -> {
            if (usernameToInvite != null && !usernameToInvite.trim().isEmpty()) {
                networkService.sendMessageToServer(String.format("INVITE_USER:%s:%s", selectedGroup, usernameToInvite.trim()));
            }
        });
    }

    public void handleKickUser() {
        String selectedGroup = view.getMyGroupsList().getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            controller.appendMessageToArea("SERVER", "Vui lòng chọn một nhóm từ danh sách 'My Groups' trước.", false);
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Kick Thành Viên");
        dialog.setHeaderText("Bạn sắp kick một thành viên khỏi nhóm: " + selectedGroup);
        dialog.setContentText("Nhập tên người dùng (username) muốn kick:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(usernameToKick -> {
            if (usernameToKick != null && !usernameToKick.trim().isEmpty()) {
                networkService.sendMessageToServer(String.format("KICK_USER:%s:%s", selectedGroup, usernameToKick.trim()));
            }
        });
    }

    public void handleSetRole() {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Đặt Vai Trò Thành Viên");
        dialog.setHeaderText("Chỉ admin mới có thể thực hiện hành động này.");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        TextField groupField = new TextField();
        TextField userField = new TextField();
        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("admin", "member");
        roleBox.setValue("member");
        String selectedGroup = view.getMyGroupsList().getSelectionModel().getSelectedItem();
        if (selectedGroup != null) {
            groupField.setText(selectedGroup);
        }
        String selectedUser = view.getOnlineUsersList().getSelectionModel().getSelectedItem();
        if (selectedUser != null) {
            userField.setText(selectedUser.split(" \\(")[0].trim());
        }

        grid.add(new Label("Tên nhóm:"), 0, 0);
        grid.add(groupField, 1, 0);
        grid.add(new Label("Người dùng:"), 0, 1);
        grid.add(userField, 1, 1);
        grid.add(new Label("Vai trò mới:"), 0, 2);
        grid.add(roleBox, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new String[]{
                        groupField.getText(),
                        userField.getText(),
                        roleBox.getValue()
                };
            }
            return null;
        });

        Optional<String[]> result = dialog.showAndWait();

        result.ifPresent(details -> {
            String group = details[0];
            String user = details[1];
            String role = details[2];

            if (group.isEmpty() || user.isEmpty() || role.isEmpty()) {
                controller.appendMessageToArea("SERVER", "Vui lòng điền đầy đủ thông tin.", false);
            } else {
                networkService.sendMessageToServer(String.format("SET_ROLE:%s:%s:%s", group, user, role));
            }
        });
    }
}