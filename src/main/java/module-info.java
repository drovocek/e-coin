module com.company.ecoin {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires java.sql;
    requires org.slf4j;

    opens ru.soft.ecoin to javafx.fxml;
    exports ru.soft.ecoin;
    opens ru.soft.ecoin.controller to javafx.fxml;
    opens ru.soft.ecoin.model to javafx.base;
}