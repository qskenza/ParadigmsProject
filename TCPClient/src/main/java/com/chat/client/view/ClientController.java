package com.chat.client.view;

import com.chat.client.model.ClientModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.io.IOException;

/**
 * JavaFX controller for the client UI.
 * Acts as the glue between ClientModel (pure Java) and the FXML view.
 * Two screens are managed here:
 *   1. Login pane  – username + server IP/port entry
 *   2. Chat pane   – message area, input field, status bar
 */
public class ClientController {

    // ── Login pane ────────────────────────────────────────────────────────────
    @FXML private javafx.scene.layout.GridPane loginPane;
    @FXML private TextField  fldUsername;
    @FXML private TextField  fldServerIp;
    @FXML private TextField  fldServerPort;
    @FXML private Label      lblLoginError;
    @FXML private Button     btnConnect;

    // ── Chat pane ─────────────────────────────────────────────────────────────
    @FXML private javafx.scene.layout.GridPane chatPane;
    @FXML private TextArea   chatArea;
    @FXML private TextField  msgField;
    @FXML private Button     btnSend;
    @FXML private Button     btnDisconnect;
    @FXML private Label      lblStatusText;
    @FXML private Circle     statusCircle;
    @FXML private Label      lblUserDisplay;

    // ── Model ─────────────────────────────────────────────────────────────────
    private final ClientModel model = new ClientModel();

    // ── Pre-filled from command-line args ─────────────────────────────────────
    private String preIp;
    private String prePort;

    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        // Start on the login screen
        showLoginPane();

        // Allow pressing Enter in the message field to send
        msgField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleSend();
        });

        // Allow pressing Enter in username/port fields to connect
        fldUsername.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) handleConnect(); });
        fldServerIp.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) handleConnect(); });
        fldServerPort.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) handleConnect(); });

        // Wire model callbacks
        model.setOnMessage(msg -> Platform.runLater(() -> appendMessage(msg)));

        model.setOnStatusChange(status -> Platform.runLater(() ->
                appendMessage("⚠ " + status)));

        model.setOnDisconnect(() -> Platform.runLater(this::handleServerDisconnect));
    }

    /** Called by ClientApp when command-line args are provided. */
    public void initParams(String ip, String port) {
        this.preIp   = ip;
        this.prePort = port;
        if (fldServerIp   != null) fldServerIp.setText(ip);
        if (fldServerPort != null) fldServerPort.setText(port);
    }

    // ── Login handler ─────────────────────────────────────────────────────────

    @FXML
    private void handleConnect() {
        String ip   = fldServerIp.getText().trim();
        String portStr = fldServerPort.getText().trim();
        String user = fldUsername.getText().trim(); // may be empty → read-only

        if (ip.isEmpty() || portStr.isEmpty()) {
            lblLoginError.setText("Server IP and Port are required.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            lblLoginError.setText("Port must be a number.");
            return;
        }

        lblLoginError.setText("Connecting…");
        btnConnect.setDisable(true);

        // Connect on a background thread so the UI stays responsive
        Thread t = new Thread(() -> {
            try {
                model.connect(ip, port, user);
                Platform.runLater(() -> transitionToChat(user.isEmpty()));
            } catch (IOException e) {
                Platform.runLater(() -> {
                    lblLoginError.setText("Could not connect: " + e.getMessage());
                    btnConnect.setDisable(false);
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Chat handlers ─────────────────────────────────────────────────────────

    @FXML
    private void handleSend() {
        String text = msgField.getText().trim();
        if (text.isEmpty()) return;

        if (!model.isConnected()) {
            appendMessage("⚠ Not connected.");
            return;
        }

        model.sendMessage(text);
        msgField.clear();

        // If the user typed 'end' or 'bye', the server will close the connection
        if (text.equalsIgnoreCase("end") || text.equalsIgnoreCase("bye")) {
            // The server will disconnect; reader thread will call onDisconnect
        }
    }

    @FXML
    private void handleDisconnect() {
        model.disconnect();
        handleServerDisconnect();
    }

    /** Called when the server drops the connection (or user clicked Disconnect). */
    private void handleServerDisconnect() {
        setOnlineStatus(false);
        appendMessage("── Disconnected ──");
        btnSend.setDisable(true);
        msgField.setDisable(true);
        btnDisconnect.setDisable(true);
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void transitionToChat(boolean readOnly) {
        showChatPane();
        setOnlineStatus(true);

        String displayName = model.getUsername().isEmpty() ? "Anonymous" : model.getUsername();
        lblUserDisplay.setText("Logged in as: " + displayName
                + (readOnly ? "  [READ-ONLY]" : ""));

        if (readOnly) {
            msgField.setDisable(true);
            btnSend.setDisable(true);
            msgField.setPromptText("Read-only mode — you cannot send messages.");
        }
    }

    private void setOnlineStatus(boolean online) {
        if (online) {
            statusCircle.setFill(Color.LIMEGREEN);
            lblStatusText.setText("Online");
        } else {
            statusCircle.setFill(Color.RED);
            lblStatusText.setText("Offline");
        }
    }

    private void appendMessage(String msg) {
        chatArea.appendText(msg + "\n");
    }

    private void showLoginPane() {
        loginPane.setVisible(true);
        loginPane.setManaged(true);
        chatPane.setVisible(false);
        chatPane.setManaged(false);
        // Pre-fill from command-line args if available
        if (preIp   != null) fldServerIp.setText(preIp);
        if (prePort != null) fldServerPort.setText(prePort);
    }

    private void showChatPane() {
        loginPane.setVisible(false);
        loginPane.setManaged(false);
        chatPane.setVisible(true);
        chatPane.setManaged(true);
    }

    /** Called when the window is closed. */
    public void shutdown() {
        model.disconnect();
    }
}
