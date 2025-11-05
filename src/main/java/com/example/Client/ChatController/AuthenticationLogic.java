package com.example.Client.ChatController;

import com.example.Client.ChatClientGUI;
import com.example.Client.ChatNetworkService;

public class AuthenticationLogic {
    
    private final ChatClientGUI view;
    private final ChatNetworkService networkService;
    private final ChatController controller;

    public AuthenticationLogic(ChatController controller, ChatClientGUI view, ChatNetworkService networkService) {
        this.controller = controller;
        this.view = view;
        this.networkService = networkService;
    }
    
    public void handleLoginRegister(String command) {
        String user = view.getUsernameField().getText().trim();
        String pass = view.getPasswordField().getText().trim();
        if (user.isEmpty() || pass.isEmpty()) {
            controller.appendMessageToArea("SERVER", "Tên đăng nhập và mật khẩu không được để trống.", false);
            return;
        }
        networkService.sendMessageToServer(String.format("%s:%s:%s", command, user, pass));
    }

    public void handleLogout() {
        networkService.sendMessageToServer("LOGOUT");
        controller.cleanup();
        view.getPasswordField().clear();
        
        controller.connectToServer();
        controller.appendMessageToArea("SERVER", "Bạn đã đăng xuất.", true);
    }
}