package com.example.Client;

import javafx.application.Platform;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.Socket;
import java.security.KeyStore;
import java.util.function.Consumer;

public class ChatNetworkService {

    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private SSLSocketFactory sslSocketFactory;
    private Consumer<String> onMessageReceived;
    private Consumer<String> onConnectionError;
    private volatile boolean isLoggingOut = false;

    public ChatNetworkService() throws Exception {
        try {
            if (!new File("client.jks").exists()) {
                 throw new FileNotFoundException("Không tìm thấy tệp 'client.jks'.");
            }
            SSLContext sslContext = createSSLContext();
            this.sslSocketFactory = sslContext.getSocketFactory();
        } catch (Exception e) {
            System.err.println("Không thể tạo SSL Context cho client: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    private SSLContext createSSLContext() throws Exception {
        char[] password = "123456".toCharArray();
        KeyStore ts = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream("client.jks")) {
            ts.load(fis, password);
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);
        
        return sslContext;
    }

    public void connect(Consumer<String> onMessageReceived, Consumer<String> onConnectionError) {
        this.onMessageReceived = onMessageReceived;
        this.onConnectionError = onConnectionError;
        this.isLoggingOut = false;

        try {
            socket = (SSLSocket) sslSocketFactory.createSocket(SERVER_IP, SERVER_PORT);
            ((SSLSocket) socket).startHandshake();

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            startReceiverThread();
            
        } catch (IOException e) {
            Platform.runLater(() -> 
                this.onConnectionError.accept("Không thể kết nối tới server (SSL): " + e.getMessage())
            );
        }
    }

    private void startReceiverThread() {
        Thread receiverThread = new Thread(() -> {
            try {
                String serverResponse;
                while ((serverResponse = in.readLine()) != null) {
                    final String msg = serverResponse;
                    Platform.runLater(() -> onMessageReceived.accept(msg));
                }
            } catch (IOException e) {
                if (!isLoggingOut) {
                    Platform.runLater(() -> onConnectionError.accept("Mất kết nối với server."));
                }
            } finally {
                disconnect();
            }
        });
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    public void sendMessageToServer(String command) {
        new Thread(() -> {
            if (out != null) {
                out.println(command);
            }
        }).start();
    }

    public void disconnect() {
        this.isLoggingOut = true;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {}
        socket = null;
        in = null;
        out = null;
    }
}