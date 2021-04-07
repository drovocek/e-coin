package com.company.NetworkHandlers;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class UI extends Application {


    @Override
    public void start(Stage stage) throws Exception {
        Parent root = null;
        try {
            root = FXMLLoader.load(getClass().getResource("../View/MainWindow.fxml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        stage.setTitle("E-Coin");
        stage.setScene(new Scene(root, 700, 600));
        stage.show();
        System.out.println("dododod");
    }


}
