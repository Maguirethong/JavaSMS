package com.example.Client.ChatController;

import com.example.Client.ChatClientGUI;
import com.example.Client.ChatNetworkService;
import javafx.scene.control.Tab;
import javafx.scene.control.TextInputDialog;

public class UtilityLogic {

    private final ChatClientGUI view;
    private final ChatNetworkService networkService;
    private final ChatController controller;

    public UtilityLogic(ChatController controller, ChatClientGUI view, ChatNetworkService networkService) {
        this.controller = controller;
        this.view = view;
        this.networkService = networkService;
    }

    public void handleSetStatus() {
        TextInputDialog dialog = new TextInputDialog("Đang rảnh");
        dialog.setTitle("Cập Nhật Trạng Thái");
        dialog.setHeaderText("Nhập trạng thái mới của bạn.");
        dialog.setContentText("Trạng thái:");
        dialog.showAndWait().ifPresent(status -> {
            if (status != null && !status.trim().isEmpty()) {
                networkService.sendMessageToServer("SET_STATUS:" + status.trim());
            }
        });
    }

    public void handleTabSelection(Tab newTab) {
        if (newTab != null) {
            newTab.setStyle("");
            view.getMessageField().setPromptText(newTab.getId().equals("SERVER") ? "Kết quả lệnh sẽ hiển thị ở đây..." : "Nhập tin nhắn tới " + newTab.getId() + "...");
        }
    }
}