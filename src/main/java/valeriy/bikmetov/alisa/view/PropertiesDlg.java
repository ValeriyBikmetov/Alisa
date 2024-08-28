package valeriy.bikmetov.alisa.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import valeriy.bikmetov.alisa.Alisa;

import java.io.IOException;
import java.util.Properties;

public class PropertiesDlg extends Dialog<ButtonType> {
    PropertiesController propController;

    public PropertiesDlg() throws IOException {
        this.setTitle("Параметры приложения");
        FXMLLoader loader = new FXMLLoader(Alisa.class.getResource("Properties.fxml"));
        getDialogPane().setContent(loader.load());
        getDialogPane().getStylesheets().add(Alisa.class.getResource("yellowOnBlack.css").toExternalForm());
        getDialogPane().getStylesheets().add("yellowOnBlack");
        propController = loader.getController();

        ButtonType btnOk = new ButtonType("Записать", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Отменить", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(btnOk, btnCancel);
    }

    public Properties getProperties() {
        return propController.getProperties();
    }
}
