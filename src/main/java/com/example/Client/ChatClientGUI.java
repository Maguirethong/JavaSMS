package com.example.Client;

import com.example.Client.ChatController.ChatController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.Arrays;

public class ChatClientGUI extends Application {

    private ChatNetworkService networkService;
    private ChatController controller;

    private Stage primaryStage;
    private TabPane mainTabPane;
    private TextField messageField, usernameField;
    private PasswordField passwordField;
    private ListView<String> onlineUsersList, myGroupsList;
    private VBox loginPanel;
    private HBox messageInputBox;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        try {
            this.networkService = new ChatNetworkService();
        } catch (Exception e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Lỗi Bảo Mật");
                alert.setHeaderText("Không thể tải TrustStore (client.jks).");
                alert.setContentText("Hãy chắc chắn tệp client.jks (mật khẩu 123456) nằm ở thư mục gốc của ứng dụng.\nLỗi: " + e.getMessage());
                alert.showAndWait();
                Platform.exit();
            });
            return;
        }

        this.controller = new ChatController(this, networkService);
        this.primaryStage = stage;
        primaryStage.setTitle("Ứng Dụng Chat");
        BorderPane mainLayout = new BorderPane();

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
        Button setRoleButton = new Button("👑 Đặt Vai Trò");
        Button logoutButton = new Button("⬅️ Đăng Xuất");
        Button emojiButton = new Button("😊");
        
        Arrays.asList(onlineButton, listGroupsButton, listMembersButton, joinGroupButton, leaveGroupButton, setStatusButton, createGroupButton, inviteUserButton, kickUserButton, setRoleButton, logoutButton)
                .forEach(button -> button.setMaxWidth(Double.MAX_VALUE));
        
        VBox commandButtons = new VBox(5, onlineButton, listGroupsButton, listMembersButton, new Separator(), joinGroupButton, leaveGroupButton, setStatusButton, new Separator(), createGroupButton, inviteUserButton, kickUserButton, setRoleButton, logoutButton);
        VBox leftPanel = new VBox(10, new Label("Users:"), onlineUsersList, new Label("My Groups:"), myGroupsList, new Separator(), new Label("Chức năng:"), commandButtons);
        leftPanel.setOnMouseClicked(event -> {
            onlineUsersList.getSelectionModel().clearSelection();
            myGroupsList.getSelectionModel().clearSelection();
        });
        leftPanel.setPadding(new Insets(10));
        leftPanel.setPrefWidth(200);
        VBox.setVgrow(onlineUsersList, Priority.ALWAYS);
        VBox.setVgrow(myGroupsList, Priority.ALWAYS);
        mainLayout.setLeft(leftPanel);

        mainTabPane = new TabPane();
        Tab serverTab = new Tab("Server");
        TextArea serverMessagesArea = new TextArea();
        serverMessagesArea.setEditable(false);
        serverTab.setContent(serverMessagesArea);
        serverTab.setClosable(false);
        serverTab.setId("SERVER");
        mainTabPane.getTabs().add(serverTab);
        mainLayout.setCenter(mainTabPane);
        
        controller.getChatAreas().put("SERVER", serverMessagesArea);
        controller.getChatTabs().put("SERVER", serverTab);

        messageField = new TextField();
        messageField.setPromptText("Đăng nhập để bắt đầu...");
        Button sendButton = new Button("Gửi");
        messageInputBox = new HBox(10, messageField, sendButton, emojiButton);
        HBox.setHgrow(messageField, Priority.ALWAYS);
        messageInputBox.setPadding(new Insets(10));

        usernameField = new TextField();
        usernameField.setPromptText("Tên đăng nhập");
        passwordField = new PasswordField();
        passwordField.setPromptText("Mật khẩu");
        Button loginButton = new Button("Đăng nhập");
        Button registerButton = new Button("Đăng ký");
        loginPanel = new VBox(10, new Label("Đăng nhập hoặc Đăng ký"), usernameField, passwordField, new HBox(10, loginButton, registerButton));
        loginPanel.setPadding(new Insets(10));

        VBox bottomContainer = new VBox(loginPanel, messageInputBox);
        mainLayout.setBottom(bottomContainer);

        loginButton.setOnAction(e -> controller.handleLoginRegister("LOGIN"));
        registerButton.setOnAction(e -> controller.handleLoginRegister("REGISTER"));
        sendButton.setOnAction(e -> controller.sendMessage());
        messageField.setOnAction(e -> controller.sendMessage());
        onlineButton.setOnAction(e -> controller.sendCommandAndShowServerTab("ONLINE"));
        listGroupsButton.setOnAction(e -> controller.sendCommandAndShowServerTab("LIST_GROUPS"));
        listMembersButton.setOnAction(e -> controller.handleListGroupMembers());
        joinGroupButton.setOnAction(e -> controller.handleJoinGroup());
        leaveGroupButton.setOnAction(e -> controller.handleLeaveGroup());
        setStatusButton.setOnAction(e -> controller.handleSetStatus());
        createGroupButton.setOnAction(e -> controller.handleCreateGroup());
        inviteUserButton.setOnAction(e -> controller.handleInviteUser());
        kickUserButton.setOnAction(e -> controller.handleKickUser());
        setRoleButton.setOnAction(e -> controller.handleSetRole());
        logoutButton.setOnAction(e -> controller.handleLogout());
        emojiButton.setOnAction(e -> controller.showEmojiPicker());
        
        onlineUsersList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String selectedItem = onlineUsersList.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    String targetUser = selectedItem.split(" \\(")[0].trim();
                    controller.openChatTab(targetUser, true);
                }
            }
        });
        
        myGroupsList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String g = myGroupsList.getSelectionModel().getSelectedItem();
                if (g != null) controller.openChatTab(g, true);
            }
        });
        
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            controller.handleTabSelection(newTab);
        });
        
        messageField.textProperty().addListener((obs, oldVal, newVal) -> {
            controller.handleTyping();
        });

        controller.connectToServer();
        controller.updateLoginUI(false);
        
        Scene scene = new Scene(mainLayout, 900, 650);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> controller.cleanup());
        primaryStage.show();
    }

    public Stage getPrimaryStage() { return primaryStage; }
    public TabPane getMainTabPane() { return mainTabPane; }
    public TextField getMessageField() { return messageField; }
    public ListView<String> getOnlineUsersList() { return onlineUsersList; }
    public ListView<String> getMyGroupsList() { return myGroupsList; }
    public VBox getLoginPanel() { return loginPanel; }
    public HBox getMessageInputBox() { return messageInputBox; }
    public TextField getUsernameField() { return usernameField; }
    public PasswordField getPasswordField() { return passwordField; }
}