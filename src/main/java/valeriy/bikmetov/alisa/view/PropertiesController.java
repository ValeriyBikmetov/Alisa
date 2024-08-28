package valeriy.bikmetov.alisa.view;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import valeriy.bikmetov.alisa.Alisa;
import valeriy.bikmetov.alisa.utilites.Constants;
import valeriy.bikmetov.alisa.utilites.FileUtil;

import java.nio.file.Path;
import java.util.Properties;

public class PropertiesController {
    @FXML
    TextField txtTemplate_DB;
    @FXML
    TextField txtFolder_DB;
    @FXML
    Button btnDb;
    @FXML
    TextField txtSuffix_DB;
    @FXML
    TextField txtTemp_Dir;
    @FXML
    Button btnTempDir;
    @FXML
    TextField txtPassword_DB;
    @FXML
    TextField txtAdmin_DB;
    @FXML
    TextField txtRead_djvu;
    @FXML
    Button btnDjvu;
    @FXML
    TextField txtRead_pdf;
    @FXML
    Button btnPdf;
    @FXML
    TextField txtRead_fb2;
    @FXML
    Button btnFb2;

    private Properties properties;

    @FXML
    void initialize() {
        properties = new Properties();
        txtTemplate_DB.textProperty().addListener((observable, oldValue, newValue) ->
                properties.setProperty("TEMPLATE_DB", newValue));
        txtFolder_DB.textProperty().addListener((observable, oldValue, newValue) ->
                properties.setProperty("FOLDER_DB", newValue));
        txtAdmin_DB.textProperty().addListener((observable, oldValue, newValue) ->
                properties.setProperty("ADMIN_DB", newValue));
        txtPassword_DB.textProperty().addListener((observable, oldValue, newValue) ->
                properties.setProperty("PASSWORD_DB", newValue));
        txtRead_djvu.textProperty().addListener((observable, oldValue, newValue) ->
                properties.setProperty("READ_DJVU", newValue));
        txtRead_fb2.textProperty().addListener((observable, oldValue, newValue) ->
                properties.setProperty("READ_FB2", newValue));
        txtSuffix_DB.textProperty().addListener((observable, oldValue, newValue) ->
                properties.setProperty("SUFFIX_DB", newValue));
        txtTemp_Dir.textProperty().addListener((observable, oldValue, newValue) ->
                properties.setProperty("TEMP_DIR", newValue));

        txtTemp_Dir.setText(Constants.TEMP_DIR);
        txtTemplate_DB.setText(Constants.TEMPLATE_DB);
        txtFolder_DB.setText(Constants.FOLDER_DB);
        txtSuffix_DB.setText(Constants.SUFFIX_DB);
        txtAdmin_DB.setText(Constants.ADMIN_DB);
        txtPassword_DB.setText(Constants.PASSWORD_DB);
        txtRead_fb2.setText(Constants.READ_FB2);
        txtRead_djvu.setText(Constants.READ_DJVU);
        txtRead_pdf.setText(Constants.READ_PDF);
    }

    @FXML
    private void onChoiceDB() {
        Path pathDB = FileUtil.directoryChooser(Alisa.getPrimaryStage(), "Выбор каталога баз данных", null);
        txtFolder_DB.setText(pathDB.toString());
    }

    @FXML
    private void onChoiceTempDir() {
        Path pathDB = FileUtil.directoryChooser(Alisa.getPrimaryStage(), "Выбор каталога Temp", null);
        txtTemp_Dir.setText(pathDB.toString());
    }

    @FXML
    private void onChoiceDjvu() {
        Path pathDB = FileUtil.directoryChooser(Alisa.getPrimaryStage(), "Программа чтения djvu", null);
        txtRead_djvu.setText(pathDB.toString());
    }

    @FXML
    private void onChoicePdf() {
        Path pathDB = FileUtil.directoryChooser(Alisa.getPrimaryStage(), "Программа чтения pdf", null);
        txtRead_pdf.setText(pathDB.toString());
    }

    @FXML
    private void onChoiceFB2() {
        Path pathDB = FileUtil.directoryChooser(Alisa.getPrimaryStage(), "Программа чтения fb2", null);
        txtRead_fb2.setText(pathDB.toString());
    }

    public Properties getProperties() {
        return properties;
    }
}
