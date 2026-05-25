package com.auction.ui.admin;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AdminTestUI extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin_test.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 600);
        scene.getStylesheets().add(getClass().getResource("/css/admin_style.css").toExternalForm());

        primaryStage.setTitle("Admin Dashboard - Auction System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}