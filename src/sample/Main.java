package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.FileInputStream;

public class Main extends Application {

    public static Stage primaryStage;

    @Override
    public void start(Stage startStage) throws Exception {
        primaryStage = startStage;
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Video Editor");
        primaryStage.getIcons().add(new Image(new FileInputStream("@..\\..\\app-icon.png")));
        primaryStage.setScene(new Scene(root, 1100, 600));
        // primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
