package com.chat.server.model;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Core server model.
 * Manages the ServerSocket, all ClientHandlers, and event notifications.
 * Has ZERO dependency on JavaFX — the UI registers callbacks to receive updates.
 */
public class ServerModel {

    // ── Configuration ───────────────────────────────────────────────────────
    private int port;

    // ── State ────────────────────────────────────────────────────────────────
    private ServerSocket serverSocket;
    private volatile boolean running = false;

    private final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    // ── Callbacks (set by the View) ──────────────────────────────────────────
    private Consumer<String>       onLog;          // new log line
    private Consumer<List<String>> onUsersChanged; // updated user list

    // ─────────────────────────────────────────────────────────────────────────

    public ServerModel() {
        loadConfig();
    }

    /** Load server.port from the properties file bundled in resources. */
    private void loadConfig() {
        Properties props = new Properties();
        try (InputStream is = getClass()
                .getResourceAsStream("/com/chat/server/server.properties")) {
            if (is != null) {
                props.load(is);
                port = Integer.parseInt(props.getProperty("server.port", "3000"));
            } else {
                port = 3000;
            }
        } catch (Exception e) {
            port = 3000;
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Register a callback for log messages. */
    public void setOnLog(Consumer<String> onLog) {
        this.onLog = onLog;
    }

    /** Register a callback invoked whenever the active user list changes. */
    public void setOnUsersChanged(Consumer<List<String>> onUsersChanged) {
        this.onUsersChanged = onUsersChanged;
    }

    /** Start accepting connections in a background thread. */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        log("Server Started on port " + port);
        log("Waiting for clients...");

        Thread acceptThread = new Thread(() -> {
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    log("New connection from " + clientSocket.getInetAddress().getHostAddress());
                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    Thread t = new Thread(handler);
                    t.setDaemon(true);
                    t.start();
                } catch (IOException e) {
                    if (running) log("Error accepting connection: " + e.getMessage());
                }
            }
        }, "accept-thread");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    /** Stop the server and disconnect all clients. */
    public void stop() {
        running = false;
        for (ClientHandler c : clients) c.sendMessage("SERVER: Server is shutting down.");
        clients.clear();
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        log("Server stopped.");
        notifyUsersChanged();
    }

    // ── Called by ClientHandler ───────────────────────────────────────────────

    public void addClient(ClientHandler handler) {
        clients.add(handler);
        log("Welcome " + handler.getUsername());
        notifyUsersChanged();
    }

    public void removeClient(ClientHandler handler) {
        clients.remove(handler);
        notifyUsersChanged();
    }

    /** Broadcast to ALL connected clients including the sender. */
    public void broadcast(String message, ClientHandler sender) {
        for (ClientHandler c : clients) {
            c.sendMessage(message);
        }
    }

    /** Broadcast to every client EXCEPT the sender. */
    public void broadcastExcept(String message, ClientHandler exclude) {
        for (ClientHandler c : clients) {
            if (c != exclude) c.sendMessage(message);
        }
    }

    /** Returns a comma-separated list of active usernames. */
    public String getActiveUsernames() {
        StringBuilder sb = new StringBuilder();
        for (ClientHandler c : clients) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(c.getUsername());
            if (c.isReadOnly()) sb.append(" (read-only)");
        }
        return sb.isEmpty() ? "(none)" : sb.toString();
    }

    public void log(String message) {
        if (onLog != null) onLog.accept(message);
    }

    private void notifyUsersChanged() {
        if (onUsersChanged == null) return;
        List<String> names = new ArrayList<>();
        for (ClientHandler c : clients) names.add(c.getUsername());
        onUsersChanged.accept(Collections.unmodifiableList(names));
    }

    public int getPort() { return port; }
}