module valeriy.bikmetov.alisa {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires javadjvu;
    requires pdfbox.app;
    requires com.h2database;
    requires javafx.swing;
    requires java.sql;

    opens valeriy.bikmetov.alisa to javafx.fxml;
    exports valeriy.bikmetov.alisa;
    exports valeriy.bikmetov.alisa.view;
    exports valeriy.bikmetov.alisa.model;
    exports valeriy.bikmetov.alisa.utilites;
    opens valeriy.bikmetov.alisa.view to javafx.fxml;
}