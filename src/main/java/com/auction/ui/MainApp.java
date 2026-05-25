package com.auction.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Đường dẫn mới trỏ thẳng vào fxml/bidding/
        URL fxmlLocation = getClass().getResource("/fxml/bidding/MainDashboardUI.fxml");

        if (fxmlLocation == null) {
            System.err.println("LỖI: Không tìm thấy file tại /fxml/bidding/MainDashboardUI.fxml");
            return;
        }

        FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Hệ thống đấu giá");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}