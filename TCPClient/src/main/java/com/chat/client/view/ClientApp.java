package com.chat.client.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX application entry point for the TCP Chat Client.
 * Launch with: java TCPClient <ServerIP> <Port>
 *   e.g.:       java TCPClient localhost 3000
 */
public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/chat/client/ClientView.fxml"));
        Scene scene = new Scene(loader.load(), 750, 560);
        scene.getStylesheets().add(
                getClass().getResource("/com/chat/client/client.css").toExternalForm());

        // Pass command-line parameters to the controller
        ClientController controller = loader.getController();
        Parameters params = getParameters();
        if (params.getRaw().size() >= 2) {
            controller.initParams(params.getRaw().get(0), params.getRaw().get(1));
        }

        primaryStage.setTitle("TCP Chat Client");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            controller.shutdown();
            System.exit(0);
        });
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
