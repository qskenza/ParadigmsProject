package com.chat.server.model;

import java.io.*;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Handles communication with a single connected client in its own thread.
 * Fully decoupled from JavaFX — notifies the server model via callbacks.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ServerModel serverModel;

    private PrintWriter out;
    private BufferedReader in;

    private String username;
    private boolean readOnly = false;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    public ClientHandler(Socket socket, ServerModel serverModel) {
        this.socket = socket;
        this.serverModel = serverModel;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // ── 1. Handshake: receive username ───────────────────────────────
            String rawName = in.readLine();
            if (rawName == null || rawName.trim().isEmpty()) {
                username  = "Anonymous_" + socket.getPort();
                readOnly  = true;
            } else {
                username = rawName.trim();
            }

            // Register this handler in the server model
            serverModel.addClient(this);

            if (readOnly) {
                sendMessage("SERVER: You are connected in READ-ONLY MODE.");
                serverModel.log("READ-ONLY connection: " + username);
            } else {
                sendMessage("SERVER: Welcome, " + username + "!");
                serverModel.log("Welcome " + username);
                serverModel.broadcastExcept(
                        "SERVER: " + username + " has joined the chat.", this);
            }

            // ── 2. Main read loop ─────────────────────────────────────────────
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();

                if (line.equalsIgnoreCase("end") || line.equalsIgnoreCase("bye")) {
                    sendMessage("SERVER: Goodbye, " + username + "!");
                    break;
                }

                if (readOnly) {
                    sendMessage("SERVER: You are in READ-ONLY MODE. Messages cannot be sent.");
                    continue;
                }

                if (line.equalsIgnoreCase("allUsers")) {
                    sendMessage("SERVER: Active users → " + serverModel.getActiveUsernames());
                    continue;
                }

                // Regular message — broadcast to everyone else
                String timestamp = LocalTime.now().format(TIME_FMT);
                String formatted = "[" + timestamp + "] " + username + ": " + line;
                serverModel.broadcast(formatted, this);
                serverModel.log("MSG from " + username + ": " + line);
            }

        } catch (IOException e) {
            serverModel.log("Connection lost: " + (username != null ? username : "unknown"));
        } finally {
            disconnect();
        }
    }

    /** Send a message directly to this client. */
    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    /** Gracefully close everything and notify the server model. */
    public void disconnect() {
        serverModel.removeClient(this);
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        if (username != null) {
            serverModel.log(username + " disconnected.");
            if (!readOnly) {
                serverModel.broadcastExcept("SERVER: " + username + " has left the chat.", this);
            }
        }
    }

    public String getUsername()  { return username; }
    public boolean isReadOnly()  { return readOnly; }
}