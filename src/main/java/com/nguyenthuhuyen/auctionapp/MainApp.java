package com.nguyenthuhuyen.auctionapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Hãy chắc chắn tên file ProductList.fxml viết đúng từng chữ hoa chữ thường
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("ProductList.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Hệ thống đấu giá - Quản lý sản phẩm");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}