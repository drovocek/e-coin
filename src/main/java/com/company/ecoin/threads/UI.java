package com.company.ecoin.threads;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class UI extends Application {

    @Override
    public void start(Stage stage) {
        Parent root = null;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(UI.class.getResource("MainWindow.fxml"));
            root = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        stage.setTitle("E-Coin");
        stage.setScene(new Scene(root, 900, 700));
        stage.show();
    }
}