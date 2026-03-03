package com.chat.server.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX application entry point for the TCP Chat Server.
 * Launch with: java -jar TCPServer.jar
 */
public class ServerApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/chat/server/ServerView.fxml"));
        Scene scene = new Scene(loader.load(), 700, 500);
        scene.getStylesheets().add(
                getClass().getResource("/com/chat/server/server.css").toExternalForm());

        primaryStage.setTitle("TCP Chat Server");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> System.exit(0));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}