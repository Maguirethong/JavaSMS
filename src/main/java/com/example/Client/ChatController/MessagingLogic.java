package com.example.Client.ChatController;

import com.example.Client.ChatClientGUI;
import com.example.Client.ChatNetworkService;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import java.util.Timer;
import java.util.TimerTask;

public class MessagingLogic {

    private final ChatClientGUI view;
    private final ChatNetworkService networkService;
    private final ChatController controller;

    private boolean isTyping = false;
    private Timer typingTimer = new Timer(true);

    public MessagingLogic(ChatController controller, ChatClientGUI view, ChatNetworkService networkService) {
        this.controller = controller;
        this.view = view;
        this.networkService = networkService;
    }

    public void sendTypingStatus(boolean isTyping) {
        Tab selectedTab = view.getMainTabPane().getSelectionModel().getSelectedItem();
        if (selectedTab != null && selectedTab.getId() != null && !selectedTab.getId().equals("SERVER") && !view.getMyGroupsList().getItems().contains(selectedTab.getId())) {
            String recipient = selectedTab.getId();
            String command = isTyping ? "TYPING_START" : "TYPING_STOP";
            networkService.sendMessageToServer(command + ":" + recipient);
        }
    }

    public void handleTyping() {
        if (controller.getCurrentUsername().isEmpty()) return;
        if (!isTyping) {
            isTyping = true;
            sendTypingStatus(true);
        }
        typingTimer.cancel();
        typingTimer = new Timer(true);
        typingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                isTyping = false;
                sendTypingStatus(false);
            }
        }, 1500);
    }

    public void sendMessage() {
        String message = view.getMessageField().getText().trim();
        if (message.isEmpty()) return;
        Tab selectedTab = view.getMainTabPane().getSelectionModel().getSelectedItem();
        if (selectedTab == null || selectedTab.getId() == null || selectedTab.getId().equals("SERVER")) {
            controller.appendMessageToArea("SERVER", "Vui lòng chọn một tab người dùng hoặc nhóm để gửi tin.", false);
            view.getMessageField().clear();
            return;
        }
        String recipient = selectedTab.getId();
        boolean isGroup = view.getMyGroupsList().getItems().contains(recipient);
        String command = isGroup ? "GROUP_MSG" : "MSG";
        networkService.sendMessageToServer(String.format("%s:%s:%s", command, recipient, message));
        view.getMessageField().clear();
    }

    public void showEmojiPicker() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Chọn Emoji");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        FlowPane flowPane = new FlowPane(Orientation.HORIZONTAL, 5, 5);
        flowPane.setPadding(new Insets(10));
        flowPane.setPrefWrapLength(200);

        String[] emojis = {"😊", "😂", "❤️", "👍", "😢", "🙏", "🤔", "🔥", "😭", "🎉", "😮", "😠", "👀", "👋"};

        for (String emoji : emojis) {
            Button b = new Button(emoji);
            b.setStyle("-fx-font-size: 18;");
            b.setOnAction(e -> {
                view.getMessageField().appendText(emoji);
            });
            flowPane.getChildren().add(b);
        }

        dialog.getDialogPane().setContent(flowPane);
        dialog.showAndWait();
    }
}