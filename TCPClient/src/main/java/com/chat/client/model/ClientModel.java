package com.chat.client.model;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Core client model.
 * Manages the TCP socket, send/receive logic.
 * Has ZERO dependency on JavaFX — the UI registers callbacks to receive updates.
 */
public class ClientModel {

    // ── State ─────────────────────────────────────────────────────────────────
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private String serverIp;
    private int    serverPort;
    private String username;
    private volatile boolean connected = false;

    // ── Callbacks (set by the View) ──────────────────────────────────────────
    private Consumer<String>  onMessage;      // incoming message from server
    private Consumer<String>  onStatusChange; // connection status updates
    private Runnable          onDisconnect;   // called when connection closes

    // ─────────────────────────────────────────────────────────────────────────

    public void setOnMessage(Consumer<String> onMessage)           { this.onMessage = onMessage; }
    public void setOnStatusChange(Consumer<String> onStatusChange) { this.onStatusChange = onStatusChange; }
    public void setOnDisconnect(Runnable onDisconnect)             { this.onDisconnect = onDisconnect; }

    /**
     * Connect to the server using the given IP, port, and username.
     * The username may be null/empty → read-only mode.
     *
     * @throws IOException if the connection cannot be established
     */
    public void connect(String serverIp, int serverPort, String username) throws IOException {
        this.serverIp   = serverIp;
        this.serverPort = serverPort;
        this.username   = (username == null || username.trim().isEmpty()) ? "" : username.trim();

        socket = new Socket(serverIp, serverPort);
        out    = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        connected = true;

        // Send username as the handshake line (empty string = read-only)
        out.println(this.username);

        // Spin up a background reader thread
        Thread reader = new Thread(() -> {
            try {
                String line;
                while (connected && (line = in.readLine()) != null) {
                    final String msg = line;
                    if (onMessage != null) onMessage.accept(msg);
                }
            } catch (IOException e) {
                if (connected) {
                    notifyStatus("Connection lost: " + e.getMessage());
                }
            } finally {
                cleanup();
                if (onDisconnect != null) onDisconnect.run();
            }
        }, "client-reader");
        reader.setDaemon(true);
        reader.start();
    }

    /**
     * Send a message to the server.
     * Returns false if not connected.
     */
    public boolean sendMessage(String message) {
        if (!connected || out == null) return false;
        out.println(message);
        return true;
    }

    /** Gracefully disconnect. */
    public void disconnect() {
        if (!connected) return;
        connected = false;
        if (out != null) out.println("bye");
        cleanup();
    }

    private void cleanup() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    private void notifyStatus(String msg) {
        if (onStatusChange != null) onStatusChange.accept(msg);
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public boolean isConnected() { return connected; }
    public String  getUsername() { return username; }
    public String  getServerIp()   { return serverIp; }
    public int     getServerPort() { return serverPort; }
}
