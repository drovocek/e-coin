package com.company.NetworkHandlers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class UI extends Thread {
    private Stage primaryStage;

    public UI(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @Override
    public void run() {
        Parent root = null;
        try {
            root = FXMLLoader.load(getClass().getResource("View/MainWindow.fxml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        primaryStage.setTitle("E-Coin");
        primaryStage.setScene(new Scene(root, 700, 600));
        primaryStage.show();
        System.out.println("dododod");
    }
}
