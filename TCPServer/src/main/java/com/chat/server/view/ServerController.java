package com.chat.server.view;

import com.chat.server.model.ServerModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.util.*;

/**
 * JavaFX controller for the server UI.
 * Acts as the glue between ServerModel (pure Java) and the FXML view.
 */
public class ServerController {

    // ── FXML bindings ─────────────────────────────────────────────────────────
    @FXML private Button      btnStartStop;
    @FXML private Label       lblStatus;
    @FXML private Circle      statusCircle;
    @FXML private TextArea    logArea;
    @FXML private ListView<String> userListView;

    // ── State ─────────────────────────────────────────────────────────────────
    private final ServerModel model = new ServerModel();
    private boolean running = false;

    // Maps username → random colour for the cell renderer
    private final Map<String, String> userColors = new HashMap<>();
    private final ObservableList<String> userItems = FXCollections.observableArrayList();

    // Random colours pool
    private static final String[] COLORS = {
        "#FFD700", "#90EE90", "#ADD8E6", "#FFB6C1", "#FFDAB9",
        "#E0BBE4", "#B5EAD7", "#C7CEEA", "#FFDFD3", "#D4EDDA"
    };
    private int colorIndex = 0;

    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        userListView.setItems(userItems);
        userListView.setCellFactory(lv -> new ColoredUserCell(userColors));

        // Wire model callbacks — always dispatch to JavaFX thread
        model.setOnLog(msg ->
                Platform.runLater(() -> logArea.appendText(msg + "\n")));

        model.setOnUsersChanged(names -> Platform.runLater(() -> {
            // Assign colours to new users
            for (String name : names) {
                userColors.computeIfAbsent(name, k -> {
                    String c = COLORS[colorIndex % COLORS.length];
                    colorIndex++;
                    return c;
                });
            }
            userItems.setAll(names);
        }));
    }

    @FXML
    private void handleStartStop() {
        if (!running) {
            startServer();
        } else {
            stopServer();
        }
    }

    private void startServer() {
        try {
            model.start();
            running = true;
            btnStartStop.setText("Stop Server");
            lblStatus.setText("Running on port " + model.getPort());
            statusCircle.setFill(Color.LIMEGREEN);
        } catch (IOException e) {
            logArea.appendText("ERROR: Could not start server – " + e.getMessage() + "\n");
        }
    }

    private void stopServer() {
        model.stop();
        running = false;
        btnStartStop.setText("Start Server");
        lblStatus.setText("Stopped");
        statusCircle.setFill(Color.RED);
        userItems.clear();
    }

    // ── Coloured cell renderer ────────────────────────────────────────────────
    private static class ColoredUserCell extends ListCell<String> {
        private final Map<String, String> colorMap;
        ColoredUserCell(Map<String, String> colorMap) { this.colorMap = colorMap; }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setStyle("");
            } else {
                setText(item);
                String hex = colorMap.getOrDefault(item, "#FFFFFF");
                setStyle("-fx-background-color: " + hex + "; -fx-padding: 4 8;");
            }
        }
    }
}