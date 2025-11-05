package com.example.Client.ChatController;

import com.example.Client.ChatClientGUI;
import com.example.Client.ChatNetworkService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatController {

    private final ChatClientGUI view;
    private final ChatNetworkService networkService;

    private final AuthenticationLogic authLogic;
    private final GroupLogic groupLogic;
    private final MessagingLogic messagingLogic;
    private final UtilityLogic utilityLogic;

    private final Map<String, TextArea> chatAreas = new ConcurrentHashMap<>();
    private final Map<String, Tab> chatTabs = new ConcurrentHashMap<>();
    private final Map<String, Label> typingLabels = new ConcurrentHashMap<>();

    private String currentUsername = "";

    public ChatController(ChatClientGUI view, ChatNetworkService networkService) {
        this.view = view;
        this.networkService = networkService;

        this.authLogic = new AuthenticationLogic(this, view, networkService);
        this.groupLogic = new GroupLogic(this, view, networkService);
        this.messagingLogic = new MessagingLogic(this, view, networkService);
        this.utilityLogic = new UtilityLogic(this, view, networkService);
    }

    public void connectToServer() {
        networkService.connect(
            this::processServerResponse,
            this::handleConnectionError
        );
    }

    private void handleConnectionError(String errorMessage) {
        appendMessageToArea("SERVER", errorMessage, true);
        cleanup();
    }

    public void processServerResponse(String response) {
        if (response.startsWith("SERVER_MSG:")) {
            appendMessageToArea("SERVER", response.substring("SERVER_MSG:".length()), true);
            return;
        }

        String[] parts = response.split(":", 2);
        String cmd = parts[0];

        switch (cmd) {
            case "LOGIN_SUCCESS":
                appendMessageToArea("SERVER", "Đăng nhập thành công.", true);
                currentUsername = response.split(":")[1];
                updateLoginUI(true);
                break;
            case "UNREAD_GROUPS": {
                String[] updateParts = response.split(":", 2);
                if (updateParts.length > 1 && !updateParts[1].isEmpty()) {
                    Arrays.stream(updateParts[1].split(","))
                            .filter(g -> !g.isEmpty())
                            .forEach(groupName -> {
                                openChatTab(groupName, false);
                                Tab tab = chatTabs.get(groupName);
                                if (tab != null && !tab.isSelected()) {
                                    tab.setStyle("-fx-background-color: #aaddff;");
                                }
                            });
                }
                break;
            }
            case "UNREAD_SENDERS": {
                String[] updateParts = response.split(":", 2);
                if (updateParts.length > 1 && !updateParts[1].isEmpty()) {
                    Arrays.stream(updateParts[1].split(","))
                            .filter(sender -> !sender.isEmpty())
                            .forEach(senderName -> {
                                openChatTab(senderName, false);
                                Tab tab = chatTabs.get(senderName);
                                if (tab != null && !tab.isSelected()) {
                                    tab.setStyle("-fx-background-color: #aaddff;");
                                }
                            });
                }
                break;
            }
            case "LOGIN_FAIL", "REGISTER_FAIL", "REGISTER_SUCCESS":
                appendMessageToArea("SERVER", response.split(":")[1], true);
                break;
            case "USER_LIST_UPDATE": {
                String[] updateParts = response.split(":", 2);
                view.getOnlineUsersList().getItems().clear();
                if (updateParts.length > 1 && !updateParts[1].isEmpty()) {
                    Arrays.stream(updateParts[1].split(","))
                            .filter(u -> !u.isEmpty() && !u.equals(currentUsername))
                            .sorted().forEach(view.getOnlineUsersList().getItems()::add);
                }
                break;
            }
            case "USER_GROUPS_UPDATE": {
                String[] updateParts = response.split(":", 2);
                view.getMyGroupsList().getItems().clear();
                if (updateParts.length > 1 && !updateParts[1].isEmpty()) {
                    Arrays.stream(updateParts[1].split(","))
                            .filter(g -> !g.isEmpty())
                            .sorted().forEach(view.getMyGroupsList().getItems()::add);
                }
                break;
            }
            case "PRIVATE_MSG": {
                String[] msgParts = response.split(":", 6);
                if (msgParts.length == 6) {
                    String sender = msgParts[1];
                    String timestamp = msgParts[2] + ":" + msgParts[3] + ":" + msgParts[4];
                    String message = msgParts[5];
                    appendMessageToArea(sender, String.format("(%s) [%s]: %s", timestamp, sender, message), true);
                }
                break;
            }
            case "MSG_SENT": {
                String[] msgParts = response.split(":", 6);
                if (msgParts.length == 6) {
                    String recipient = msgParts[1];
                    String timestamp = msgParts[2] + ":" + msgParts[3] + ":" + msgParts[4];
                    String message = msgParts[5];
                    appendMessageToArea(recipient, String.format("(%s) [Bạn]: %s", timestamp, message), false);
                }
                break;
            }
            case "GROUP_MSG": {
                String[] msgParts = response.split(":", 7);
                if (msgParts.length == 7) {
                    String groupName = msgParts[1];
                    String sender = msgParts[2];
                    String timestamp = msgParts[3] + ":" + msgParts[4] + ":" + msgParts[5];
                    String message = msgParts[6];
                    boolean isNotification = !sender.equals(currentUsername);
                    String formattedSender = isNotification ? sender : "Bạn";
                    appendMessageToArea(groupName, String.format("(%s) [%s]: %s", timestamp, formattedSender, message), isNotification);
                }
                break;
            }
            case "CHAT_HISTORY": {
                String[] historyParts = response.split(":", 3);
                TextArea area = chatAreas.get(historyParts[1]);
                if (area != null) {
                    String history = (historyParts.length > 2 && !historyParts[2].isEmpty()) ? historyParts[2].replace("\\n", "\n") + "\n" : "";
                    area.setText(history);
                    area.positionCaret(area.getLength());
                }
                break;
            }
            case "KICKED_FROM_GROUP": {
                String kickedGroup = response.split(":")[1];
                Tab kickedTab = chatTabs.get(kickedGroup);
                if (kickedTab != null) view.getMainTabPane().getTabs().remove(kickedTab);
                appendMessageToArea("SERVER", "Bạn đã bị xóa khỏi nhóm " + kickedGroup, true);
                break;
            }
            case "TYPING_NOTIF": {
                String[] typingParts = response.split(":", 3);
                Label typingLabel = typingLabels.get(typingParts[1]);
                if (typingLabel != null) {
                    typingLabel.setVisible("START".equals(typingParts[2]));
                    typingLabel.setText(typingParts[1] + " đang soạn tin...");
                }
                break;
            }
            case "INVITED_TO_GROUP", "NEW_GROUP":
                openChatTab(response.split(":")[1], false);
                break;
            default:
                appendMessageToArea("SERVER", response, true);
        }
    }

    public void openChatTab(String targetName, boolean selectAfterOpening) {
        if (chatTabs.containsKey(targetName)) {
            if (selectAfterOpening) view.getMainTabPane().getSelectionModel().select(chatTabs.get(targetName));
            return;
        }
        Tab newTab = new Tab(targetName);
        newTab.setId(targetName);
        BorderPane tabContent = new BorderPane();
        TextArea newChatArea = new TextArea();
        newChatArea.setEditable(false);
        newChatArea.setWrapText(true);
        tabContent.setCenter(newChatArea);
        Label typingLabel = new Label();
        typingLabel.setPadding(new Insets(0, 5, 2, 5));
        typingLabel.setVisible(false);
        tabContent.setBottom(typingLabel);
        newTab.setContent(tabContent);
        newTab.setOnCloseRequest(e -> {
            chatAreas.remove(targetName);
            chatTabs.remove(targetName);
            typingLabels.remove(targetName);
        });
        chatAreas.put(targetName, newChatArea);
        chatTabs.put(targetName, newTab);
        typingLabels.put(targetName, typingLabel);
        view.getMainTabPane().getTabs().add(newTab);
        if (selectAfterOpening) {
            view.getMainTabPane().getSelectionModel().select(newTab);
        }
        networkService.sendMessageToServer("GET_HISTORY:" + targetName);
    }

    public void appendMessageToArea(String target, String message, boolean isNotification) {
        Platform.runLater(() -> {
            boolean isNewTab = !chatTabs.containsKey(target);
            if (isNewTab) {
                openChatTab(target, false);
            }
            TextArea area = chatAreas.get(target);
            if (area != null) {
                area.appendText(message + "\n");
                area.positionCaret(area.getLength());
            }
            Tab tab = chatTabs.get(target);
            if (tab != null && !tab.isSelected() && isNotification) {
                tab.setStyle("-fx-background-color: #aaddff;");
            }
        });
    }

    public void sendCommandAndShowServerTab(String command) {
        networkService.sendMessageToServer(command);
        view.getMainTabPane().getSelectionModel().select(chatTabs.get("SERVER"));
    }

    public void updateLoginUI(boolean loggedIn) {
        view.getLoginPanel().setVisible(!loggedIn);
        view.getLoginPanel().setManaged(!loggedIn);
        view.getMessageInputBox().setVisible(loggedIn);
        view.getMessageInputBox().setManaged(loggedIn);
        if (loggedIn) {
            view.getPrimaryStage().setTitle("Chat - " + currentUsername);
            view.getMessageField().setPromptText("Chọn một cuộc trò chuyện để bắt đầu...");
        } else {
            view.getPrimaryStage().setTitle("Ứng Dụng Chat");
            if (view.getMainTabPane().getTabs().size() > 1)
                view.getMainTabPane().getTabs().remove(1, view.getMainTabPane().getTabs().size());
            chatAreas.keySet().removeIf(key -> !key.equals("SERVER"));
            chatTabs.keySet().removeIf(key -> !key.equals("SERVER"));
            view.getOnlineUsersList().getItems().clear();
            view.getMyGroupsList().getItems().clear();
        }
    }
    
    public void cleanup() {
        if (networkService != null) {
            networkService.disconnect();
        }
        Platform.runLater(() -> updateLoginUI(false));
        currentUsername = "";
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public Map<String, TextArea> getChatAreas() {
        return this.chatAreas;
    }

    public Map<String, Tab> getChatTabs() {
        return this.chatTabs;
    }

    public void handleLoginRegister(String command) {
        authLogic.handleLoginRegister(command);
    }

    public void handleLogout() {
        authLogic.handleLogout();
    }

    public void handleListGroupMembers() {
        groupLogic.handleListGroupMembers();
    }

    public void handleLeaveGroup() {
        groupLogic.handleLeaveGroup();
    }

    public void handleJoinGroup() {
        groupLogic.handleJoinGroup();
    }

    public void handleCreateGroup() {
        groupLogic.handleCreateGroup();
    }

    public void handleInviteUser() {
        groupLogic.handleInviteUser();
    }

    public void handleKickUser() {
        groupLogic.handleKickUser();
    }

    public void handleSetRole() {
        groupLogic.handleSetRole();
    }

    public void handleTyping() {
        messagingLogic.handleTyping();
    }

    public void sendMessage() {
        messagingLogic.sendMessage();
    }

    public void showEmojiPicker() {
        messagingLogic.showEmojiPicker();
    }

    public void handleSetStatus() {
        utilityLogic.handleSetStatus();
    }

    public void handleTabSelection(Tab newTab) {
        utilityLogic.handleTabSelection(newTab);
    }
}