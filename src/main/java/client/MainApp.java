package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.File;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Dùng đường dẫn tuyệt đối
        File fxmlFile = new File("src/main/java/client/login.fxml");
        System.out.println("Duong dan: " + fxmlFile.getAbsolutePath());
        System.out.println("File ton tai: " + fxmlFile.exists());

        FXMLLoader loader = new FXMLLoader(fxmlFile.toURI().toURL());
        Parent root = loader.load();

        primaryStage.setTitle("He thong dau gia");
        primaryStage.setScene(new Scene(root, 400, 500));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}