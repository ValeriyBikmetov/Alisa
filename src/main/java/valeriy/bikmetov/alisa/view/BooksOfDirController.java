package valeriy.bikmetov.alisa.view;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import valeriy.bikmetov.alisa.Alisa;
import valeriy.bikmetov.alisa.model.FlagsActions;
import valeriy.bikmetov.alisa.model.Library;
import valeriy.bikmetov.alisa.utilites.Constants;
import valeriy.bikmetov.alisa.utilites.ManagerBookInfo;
import valeriy.bikmetov.alisa.utilites.Utilities;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;

public class BooksOfDirController {
    private FlagsActions flagsActions;
    private Stage dlgStage;
    private Library currentLibrary;
    @FXML
    private ListView<String> listSource;
    @FXML
    private ListView<String> listProcess;

    public BooksOfDirController() {}

    @FXML
    public void initialize() {
        listProcess.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listSource.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    @FXML
    private void onMoveAll() {
        listSource.getSelectionModel().selectAll();
        ObservableList<String> selected = listSource.getSelectionModel().getSelectedItems();
        listProcess.getItems().addAll(selected);
        listSource.getItems().clear();
    }

    @FXML
    private void onMoveSelected() {
        ObservableList<String> selected = listSource.getSelectionModel().getSelectedItems();
        listProcess.getItems().addAll(selected);
        listSource.getItems().removeAll(selected);
    }

    @FXML
    private void onBackAll() {
        listProcess.getSelectionModel().selectAll();
        ObservableList<String> selected = listProcess.getSelectionModel().getSelectedItems();
        listSource.getItems().addAll(selected);
        listProcess.getItems().clear();
    }

    @FXML
    private void onBackSelected() {
        ObservableList<String> selected = listProcess.getSelectionModel().getSelectedItems();
        listSource.getItems().addAll(selected);
        listProcess.getItems().removeAll(selected);
    }

    @FXML
    private void onProcessing() {
    // Просматриваем список listProcess. Для каждого файла вызываем BookInfoDlg
        if (listProcess.getItems().isEmpty())
            return;
        String[] items = listProcess.getItems().toArray(new String[0]);
        for (String item : items) {
            if (flagsActions.isBreakAct()) break;
            var path = Path.of(item); // Путь к книге
            var managerBookInfo = new ManagerBookInfo();
            try {
                FXMLLoader loader = new FXMLLoader(Alisa.class.getResource("BookInfoDlg.fxml"));
                AnchorPane bookInfo = loader.load();
                Stage dlgInfo = new Stage();
                dlgInfo.setTitle("Свойства книги");
                dlgInfo.initModality(Modality.WINDOW_MODAL);
                dlgInfo.initOwner(this.dlgStage);
                Scene scene = new Scene(bookInfo);
                dlgInfo.setScene(scene);
                BookInfoDlgController controller = loader.getController();
                controller.setDlgStage(dlgInfo);
                controller.setCurrentLibrary(currentLibrary);
                managerBookInfo.makeInformation(path, controller.imageView, controller.textArea);
                controller.setNewBookInfo(managerBookInfo.getCurrentBook());
                controller.setVisibleBtn();
                controller.setFlagsActions(flagsActions);
                dlgInfo.showAndWait();
                listProcess.getItems().remove(item);
            } catch(IOException | XMLStreamException | SQLException ex) {
                Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
                Utilities.showMessage(Constants.TITLE_EXCEPTION, this.getClass().getName(), ex.getMessage());
            }
        }
    }

    public void setFlagsActions(FlagsActions flags) {flagsActions = flags;}
    @FXML
    public void onCancel() {
        dlgStage.close();
    }

    public void setDlgStage(Stage stage) {
        dlgStage = stage;
    }

    public void setCurrentLibrary(Library library) {
        currentLibrary = library;
    }

    public void fillListSource(ArrayList<String> list) {
        listSource.getItems().addAll(list);
    }
}
