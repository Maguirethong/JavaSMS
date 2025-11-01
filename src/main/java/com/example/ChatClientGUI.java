package com.example;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatClientGUI extends Application {

    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    private Stage primaryStage;
    private TabPane mainTabPane;
    private final Map<String, TextArea> chatAreas = new ConcurrentHashMap<>();
    private final Map<String, Tab> chatTabs = new ConcurrentHashMap<>();
    private final Map<String, Label> typingLabels = new ConcurrentHashMap<>();

    private TextField messageField, usernameField;
    private PasswordField passwordField;
    private ListView<String> onlineUsersList, myGroupsList;
    private VBox loginPanel;
    private HBox messageInputBox;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String currentUsername = "";

    private boolean isTyping = false;
    private Timer typingTimer = new Timer(true);
    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("Ứng Dụng Chat");
        BorderPane mainLayout = new BorderPane();

        // --- Khu vực bên trái ---
        onlineUsersList = new ListView<>();
        myGroupsList = new ListView<>();
        Button onlineButton = new Button("👥 Xem Online");
        Button listGroupsButton = new Button("📂 Xem Các Nhóm");
        Button listMembersButton = new Button("👨‍👩‍👧‍👦 Xem Thành Viên");
        Button joinGroupButton = new Button("➡️ Vào Nhóm");
        Button leaveGroupButton = new Button("🚪 Rời Nhóm");
        Button setStatusButton = new Button("💬 Đặt Trạng Thái");
        Button createGroupButton = new Button("➕ Tạo Nhóm Mới");
        Button inviteUserButton = new Button("📧 Mời vào Nhóm");
        Button kickUserButton = new Button("🚫 Kick User");
        Arrays.asList(onlineButton, listGroupsButton, listMembersButton, joinGroupButton, leaveGroupButton, setStatusButton, createGroupButton, inviteUserButton, kickUserButton)
                .forEach(button -> button.setMaxWidth(Double.MAX_VALUE));
        VBox commandButtons = new VBox(5, onlineButton, listGroupsButton, listMembersButton, new Separator(), joinGroupButton, leaveGroupButton, setStatusButton, new Separator(), createGroupButton, inviteUserButton, kickUserButton);
        VBox leftPanel = new VBox(10, new Label("Online Users:"), onlineUsersList, new Label("My Groups:"), myGroupsList, new Separator(), new Label("Chức năng:"), commandButtons);
        leftPanel.setOnMouseClicked(event -> {
            onlineUsersList.getSelectionModel().clearSelection();
            myGroupsList.getSelectionModel().clearSelection();
        });
        leftPanel.setPadding(new Insets(10));
        leftPanel.setPrefWidth(200);
        VBox.setVgrow(onlineUsersList, Priority.ALWAYS);
        VBox.setVgrow(myGroupsList, Priority.ALWAYS);
        mainLayout.setLeft(leftPanel);

        // --- Khu vực trung tâm (Tab) ---
        mainTabPane = new TabPane();
        Tab serverTab = new Tab("Server");
        TextArea serverMessagesArea = new TextArea();
        serverMessagesArea.setEditable(false);
        serverTab.setContent(serverMessagesArea);
        serverTab.setClosable(false);
        serverTab.setId("SERVER");
        mainTabPane.getTabs().add(serverTab);
        chatAreas.put("SERVER", serverMessagesArea);
        chatTabs.put("SERVER", serverTab);
        mainLayout.setCenter(mainTabPane);

        // --- Khu vực dưới cùng ---
        messageField = new TextField();
        messageField.setPromptText("Đăng nhập để bắt đầu...");
        Button sendButton = new Button("Gửi");
        messageInputBox = new HBox(10, messageField, sendButton);
        HBox.setHgrow(messageField, Priority.ALWAYS);
        messageInputBox.setPadding(new Insets(10));

        // --- Khu vực đăng nhập ---
        usernameField = new TextField(); usernameField.setPromptText("Tên đăng nhập");
        passwordField = new PasswordField(); passwordField.setPromptText("Mật khẩu");
        Button loginButton = new Button("Đăng nhập");
        Button registerButton = new Button("Đăng ký");
        loginPanel = new VBox(10, new Label("Đăng nhập hoặc Đăng ký"), usernameField, passwordField, new HBox(10, loginButton, registerButton));
        loginPanel.setPadding(new Insets(10));

        VBox bottomContainer = new VBox(loginPanel, messageInputBox);
        mainLayout.setBottom(bottomContainer);

        // --- Gán sự kiện ---
        loginButton.setOnAction(e -> handleLoginRegister("LOGIN"));
        registerButton.setOnAction(e -> handleLoginRegister("REGISTER"));
        sendButton.setOnAction(e -> sendMessage());
        messageField.setOnAction(e -> sendMessage());
        onlineButton.setOnAction(e -> sendCommandAndShowServerTab("ONLINE"));
        listGroupsButton.setOnAction(e -> sendCommandAndShowServerTab("LIST_GROUPS"));
        listMembersButton.setOnAction(e -> handleListGroupMembers());
        joinGroupButton.setOnAction(e -> handleJoinGroup());
        leaveGroupButton.setOnAction(e -> handleLeaveGroup());
        setStatusButton.setOnAction(e -> handleSetStatus());
        createGroupButton.setOnAction(e -> handleCreateGroup());
        inviteUserButton.setOnAction(e -> handleInviteUser());
        kickUserButton.setOnAction(e -> handleKickUser());
        onlineUsersList.setOnMouseClicked(e -> { 
            if (e.getClickCount() == 2) { 
                String selectedItem = onlineUsersList.getSelectionModel().getSelectedItem(); 
                if (selectedItem != null) {
                    // Tách chuỗi "username (status)" để lấy "username"
                    String targetUser = selectedItem.split(" \\(")[0].trim();
                    openChatTab(targetUser, true); 
                }
            }
        });
        myGroupsList.setOnMouseClicked(e -> { if (e.getClickCount() == 2) { String g = myGroupsList.getSelectionModel().getSelectedItem(); if (g != null) openChatTab(g, true); }});
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                newTab.setStyle("");
                messageField.setPromptText(newTab.getId().equals("SERVER") ? "Kết quả lệnh sẽ hiển thị ở đây..." : "Nhập tin nhắn tới " + newTab.getId() + "...");
            }
        });
        messageField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (currentUsername.isEmpty()) return;
            if (!isTyping) {
                isTyping = true;
                sendTypingStatus(true);
            }
            typingTimer.cancel();
            typingTimer = new Timer(true);
            typingTimer.schedule(new TimerTask() { @Override public void run() { isTyping = false; sendTypingStatus(false); }}, 1500);
        });

        connectToServer();
        updateLoginUI(false);
        Scene scene = new Scene(mainLayout, 900, 650);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> cleanup());
        primaryStage.show();
    }

// --- Các phương thức xử lý giao diện và logic ---

    private void openChatTab(String targetName, boolean selectAfterOpening) {
        if (chatTabs.containsKey(targetName)) {
            if (selectAfterOpening) mainTabPane.getSelectionModel().select(chatTabs.get(targetName));
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
        mainTabPane.getTabs().add(newTab);
        if (selectAfterOpening) {
            mainTabPane.getSelectionModel().select(newTab);
        }
        sendMessageToServer("GET_HISTORY:" + targetName);
    }

    private void appendMessageToArea(String target, String message, boolean isNotification) {
        Platform.runLater(() -> {
            boolean isNewTab = !chatTabs.containsKey(target);

            if (isNewTab) {
                openChatTab(target, false);
            }

            // Chỉ append tin nhắn nếu tab đã tồn tại.
            // Nếu là tab mới, lịch sử trò chuyện (bao gồm cả tin nhắn này) sẽ được tải và hiển thị.
            if (!isNewTab) {
                TextArea area = chatAreas.get(target);
                if (area != null) {
                    area.appendText(message + "\n");
                    area.positionCaret(area.getLength());
                }
            }

            Tab tab = chatTabs.get(target);
            if (tab != null && !tab.isSelected() && isNotification) {
                tab.setStyle("-fx-background-color: #aaddff;");
            }
        });
    }

// Trong file ChatClientGUI.java

    private void processServerResponse(String response) {
        Platform.runLater(() -> {
            if (response.startsWith("SERVER_MSG:")) {
                appendMessageToArea("SERVER", response.substring("SERVER_MSG:".length()), true);
                return;
            }

            String[] parts = response.split(":", 2);
            String cmd = parts[0];

            switch (cmd) {
                case "LOGIN_SUCCESS":
                    currentUsername = response.split(":")[1];
                    updateLoginUI(true);
                    break;
                case "LOGIN_FAIL", "REGISTER_FAIL", "REGISTER_SUCCESS":
                    appendMessageToArea("SERVER", response.split(":")[1], true);
                    break;
                case "USER_LIST_UPDATE": {
                    String[] updateParts = response.split(":", 2);
                    onlineUsersList.getItems().clear();
                    if (updateParts.length > 1 && !updateParts[1].isEmpty()) {
                        Arrays.stream(updateParts[1].split(","))
                                .filter(u -> !u.isEmpty() && !u.equals(currentUsername))
                                .sorted().forEach(onlineUsersList.getItems()::add);
                    }
                    break;
                }
                case "USER_GROUPS_UPDATE": {
                    String[] updateParts = response.split(":", 2);
                    myGroupsList.getItems().clear();
                    if (updateParts.length > 1 && !updateParts[1].isEmpty()) {
                        Arrays.stream(updateParts[1].split(","))
                                .filter(g -> !g.isEmpty())
                                .sorted().forEach(myGroupsList.getItems()::add);
                    }
                    break;
                }
            case "PRIVATE_MSG": {
                String[] msgParts = response.split(":", 5);
                if (msgParts.length == 5) {
                    String sender = msgParts[1];
                    String timestamp = msgParts[2] + ":" + msgParts[3];
                    String message = msgParts[4];
                    appendMessageToArea(sender, String.format("(%s) [%s]: %s", timestamp, sender, message), true);
                }
                break;
            }
            case "MSG_SENT": {
                String[] msgParts = response.split(":", 5);
                if (msgParts.length == 5) {
                    String recipient = msgParts[1];
                    String timestamp = msgParts[2] + ":" + msgParts[3];
                    String message = msgParts[4];
                    appendMessageToArea(recipient, String.format("(%s) [Bạn]: %s", timestamp, message), false);
                }
                break;
            }
            case "GROUP_MSG": {
                String[] msgParts = response.split(":", 6);
                if (msgParts.length == 6) {
                    String groupName = msgParts[1];
                    String sender = msgParts[2];
                    String timestamp = msgParts[3] + ":" + msgParts[4];
                    String message = msgParts[5];
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
                    if (kickedTab != null) mainTabPane.getTabs().remove(kickedTab);
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
        });
    }

    private void sendCommandAndShowServerTab(String command) { sendMessageToServer(command); mainTabPane.getSelectionModel().select(chatTabs.get("SERVER")); }

    private void handleListGroupMembers() {
        String selectedGroup = myGroupsList.getSelectionModel().getSelectedItem();
        TextInputDialog dialog = new TextInputDialog(selectedGroup != null ? selectedGroup : "");
        dialog.setTitle("Xem Thành Viên Nhóm");
        dialog.setHeaderText("Nhập tên nhóm bạn muốn xem thành viên.");
        dialog.setContentText("Tên nhóm:");
        dialog.showAndWait().ifPresent(groupName -> {
            if (groupName != null && !groupName.trim().isEmpty()) sendCommandAndShowServerTab("LIST_GROUP_MEMBERS:" + groupName.trim());
        });
    }

    private void handleLeaveGroup() {
        String selectedGroup = myGroupsList.getSelectionModel().getSelectedItem();
        TextInputDialog dialog = new TextInputDialog(selectedGroup != null ? selectedGroup : "");
        dialog.setTitle("Rời Nhóm");
        dialog.setHeaderText("Nhập tên nhóm bạn muốn rời.");
        dialog.setContentText("Tên nhóm:");
        dialog.showAndWait().ifPresent(groupName -> {
            if (groupName != null && !groupName.trim().isEmpty()) sendMessageToServer("LEAVE_GROUP:" + groupName.trim());
        });
    }

    private void handleJoinGroup() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Tham Gia Nhóm");
        dialog.setHeaderText("Nhập tên nhóm bạn muốn tham gia.");
        dialog.setContentText("Tên nhóm:");
        dialog.showAndWait().ifPresent(groupName -> {
            if (groupName != null && !groupName.trim().isEmpty()) sendMessageToServer("JOIN_GROUP:" + groupName.trim());
        });
    }

    private void handleSetStatus() {
        TextInputDialog dialog = new TextInputDialog("Đang rảnh");
        dialog.setTitle("Cập Nhật Trạng Thái");
        dialog.setHeaderText("Nhập trạng thái mới của bạn.");
        dialog.setContentText("Trạng thái:");
        dialog.showAndWait().ifPresent(status -> {
            if (status != null && !status.trim().isEmpty()) sendMessageToServer("SET_STATUS:" + status.trim());
        });
    }

    private void handleCreateGroup() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Tạo Nhóm Mới");
        dialog.setHeaderText("Nhập tên cho nhóm bạn muốn tạo.");
        dialog.setContentText("Tên nhóm:");
        dialog.showAndWait().ifPresent(groupName -> {
            if (groupName != null && !groupName.trim().isEmpty()) sendMessageToServer("CREATE_GROUP:" + groupName.trim());
        });
    }

    private void handleInviteUser() {
        String selectedItem = onlineUsersList.getSelectionModel().getSelectedItem();
        String selectedGroup = myGroupsList.getSelectionModel().getSelectedItem();
        if (selectedItem == null || selectedGroup == null) {
            appendMessageToArea("SERVER", "Mời thất bại: Vui lòng chọn một người dùng và một nhóm.", false);
            return;
        }
        String selectedUser = selectedItem.split(" \\(")[0].trim();
        sendMessageToServer(String.format("INVITE_USER:%s:%s", selectedGroup, selectedUser));
    }

    private void handleKickUser() {
        String selectedItem = onlineUsersList.getSelectionModel().getSelectedItem();
        String selectedGroup = myGroupsList.getSelectionModel().getSelectedItem();
        if (selectedItem == null || selectedGroup == null) {
            appendMessageToArea("SERVER", "Kick User: Vui lòng chọn một nhóm và một người dùng để xóa.", false);
            return;
        }
        String selectedUser = selectedItem.split(" \\(")[0].trim();
        sendMessageToServer(String.format("KICK_USER:%s:%s", selectedGroup, selectedUser));
    }

    private void sendTypingStatus(boolean isTyping) {
        Tab selectedTab = mainTabPane.getSelectionModel().getSelectedItem();
        // SỬA LỖI: Chỉ gửi typing status cho chat 1-1 (không phải server, không phải group)
        if (selectedTab != null && selectedTab.getId() != null && !selectedTab.getId().equals("SERVER") && !myGroupsList.getItems().contains(selectedTab.getId())) {
            String recipient = selectedTab.getId();
            String command = isTyping ? "TYPING_START" : "TYPING_STOP";
            sendMessageToServer(command + ":" + recipient);
        }
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty()) return;
        Tab selectedTab = mainTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null || selectedTab.getId() == null || selectedTab.getId().equals("SERVER")) {
            appendMessageToArea("SERVER", "Vui lòng chọn một tab người dùng hoặc nhóm để gửi tin.", false);
            messageField.clear();
            return;
        }
        String recipient = selectedTab.getId();
        boolean isGroup = myGroupsList.getItems().contains(recipient);
        String command = isGroup ? "GROUP_MSG" : "MSG"; // Sửa: Khớp với server (MSG và GROUP_MSG)
        sendMessageToServer(String.format("%s:%s:%s", command, recipient, message));
        messageField.clear();
    }

    private void updateLoginUI(boolean loggedIn) {
        loginPanel.setVisible(!loggedIn);
        loginPanel.setManaged(!loggedIn);
        messageInputBox.setVisible(loggedIn);
        messageInputBox.setManaged(loggedIn);
        if (loggedIn) {
            primaryStage.setTitle("Chat - " + currentUsername);
            messageField.setPromptText("Chọn một cuộc trò chuyện để bắt đầu...");
        } else {
            primaryStage.setTitle("Ứng Dụng Chat");
            if (mainTabPane.getTabs().size() > 1) mainTabPane.getTabs().remove(1, mainTabPane.getTabs().size());
            chatAreas.keySet().removeIf(key -> !key.equals("SERVER"));
            chatTabs.keySet().removeIf(key -> !key.equals("SERVER"));
            onlineUsersList.getItems().clear();
            myGroupsList.getItems().clear();
        }
    }

    private void handleLoginRegister(String command) {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText().trim();
        if (user.isEmpty() || pass.isEmpty()) {
            appendMessageToArea("SERVER", "Tên đăng nhập và mật khẩu không được để trống.", false);
            return;
        }
        sendMessageToServer(String.format("%s:%s:%s", command, user, pass));
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Thread receiverThread = new Thread(() -> {
                try {
                    String serverResponse;
                    while ((serverResponse = in.readLine()) != null) {
                        processServerResponse(serverResponse);
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> appendMessageToArea("SERVER", "Mất kết nối với server.", true));
                } finally {
                    cleanup();
                }
            });
            receiverThread.setDaemon(true);
            receiverThread.start();
        } catch (IOException e) {
            Platform.runLater(() -> appendMessageToArea("SERVER", "Không thể kết nối tới server: " + e.getMessage(), false));
        }
    }

    private void sendMessageToServer(String command) {
        new Thread(() -> {
            if (out != null) {
                out.println(command);
            }
        }).start();
    }

    private void cleanup() {
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException e) {}
        Platform.runLater(() -> updateLoginUI(false));
    }
}
