module com.company.ecoin {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires java.sql;
    requires org.slf4j;

    opens com.company.ecoin to javafx.fxml;
    exports com.company.ecoin;
    opens com.company.ecoin.controller to javafx.fxml;
    opens com.company.ecoin.model to javafx.base;
}