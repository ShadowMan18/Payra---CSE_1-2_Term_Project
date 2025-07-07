module com.example.javafx_project {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires javafx.swing;
    requires webcam.capture;
    requires java.desktop;
    requires com.fasterxml.jackson.annotation;
    requires java.compiler;
    requires java.sql;

    opens codes to javafx.fxml;
    exports codes;
}