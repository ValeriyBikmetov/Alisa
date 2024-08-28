package valeriy.bikmetov.alisa.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import valeriy.bikmetov.alisa.Alisa;
import valeriy.bikmetov.alisa.model.Book;
import valeriy.bikmetov.alisa.model.FlagsActions;
import valeriy.bikmetov.alisa.model.Item;
import valeriy.bikmetov.alisa.model.Library;
import valeriy.bikmetov.alisa.utilites.*;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;

import static valeriy.bikmetov.alisa.utilites.FileUtil.directoryChooser;

public class FindBooksController {
    @FXML
    private ImageView imageView;
    @FXML
    private ListView<Item> lstBooks;
    @FXML
    private TextArea txtInfo;
    @FXML
    private Button btnAdd;
    @FXML
    private Button btnCancel;

    private Library currentLibrary;
    private Book currentBook;
    private Stage dlgStage;

    public FindBooksController() {}

    @FXML
    public void initialize() {
        lstBooks.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        lstBooks.getSelectionModel().selectedItemProperty().addListener((obj, oldValue, newValue) -> {
            selectedListener(newValue.getId());
        });
        btnAdd.setOnAction(event -> {
            addBook();
            dlgStage.close();
        });
        btnCancel.setOnAction(event -> dlgStage.close());
    }
    @FXML
    private void onDeleteBook() {
        var action = Utilities.makeIt("Удаление книги", "Вы уверенв?");
        if(!action) return;;
        // Удалить выбранную книгу
        var sel = lstBooks.getSelectionModel();
        var item = sel.getSelectedItem();
        var book_id = item.getId();
        var index = sel.getSelectedIndex();
        try {
            var book = H2Operations.makeBook(currentLibrary.getConnection(), book_id);
            H2Operations.deleteBook(currentLibrary.getConnection(), book);
            FileUtil.deleteFileAndFolder(currentLibrary, book);
            lstBooks.getItems().remove(index);
            if (!sel.isEmpty()) {
                sel.selectFirst();
            }
        } catch (SQLException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION,"Error.", ex.getMessage());
        }
    }
    @FXML
    private void onCopy() {
        // Копировать выбранную книгу куда-либо
        var item = lstBooks.getSelectionModel().getSelectedItem();
        var book_id = item.getId();
        var target = directoryChooser(Alisa.getPrimaryStage(), "Куда копируем?", null);
        if (target != null)
            BookUtil.copyBook(currentLibrary, book_id, target);
    }
    @FXML
    private void onRead() {
        // Читать выбранную книгу
        var item = lstBooks.getSelectionModel().getSelectedItem();
        var book_id = item.getId();
        BookUtil.readOfBook(currentLibrary, book_id);
    }

    @FXML
    private void addBook() {
        var path = FileUtil.appendNewBook(currentLibrary, currentBook);
        if (path != null) {
            try {
                H2Operations.newBookID(currentLibrary.getConnection(), currentBook);
            } catch (SQLException ex) {
                Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
                Utilities.showMessage(Constants.TITLE_EXCEPTION,"Ошибка добавления книги. Файл будет удален.",
                        ex.getMessage());
                FileUtil.deleteFileAndFolder(currentLibrary, currentBook);
            }
        }
    }

    @FXML
    private void onChange() throws SQLException {
        int bookId = lstBooks.getSelectionModel().getSelectedItem().getId();
        var managerBookInfo = new ManagerBookInfo();
        managerBookInfo.setCurrentLibrary(currentLibrary);
        var flagsActions = new FlagsActions();
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
            managerBookInfo.makeInformation(bookId, controller.imageView, controller.textArea);
            controller.setNewBookInfo(managerBookInfo.getCurrentBook());
            controller.setFlagsActions(flagsActions);
            dlgInfo.showAndWait();
        } catch(IOException | XMLStreamException | SQLException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION, this.getClass().getName(), ex.getMessage());
        }
    }

    public void fillListBooks(ArrayList<Integer> lst) {
        // Заполняем список заголовками найденных книг
        try {
            String query = H2Operations.getQuery(currentLibrary.getConnection(), "QUERY_BOOK_TITLE");
            try(var pstm = currentLibrary.getConnection().prepareStatement(query)) {
                for(var item: lst) {
                    pstm.setInt(1, item);
                    try (ResultSet rs = pstm.executeQuery()) {
                        if (rs.next()) {
                            var title = rs.getString(1);
                            var element = new Item(item, title);
                            lstBooks.getItems().add(element);
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION,"Error.", ex.getMessage());
        }
        lstBooks.getSelectionModel().selectFirst();
    }

    private void selectedListener(int book_id) {
        var manager = new ManagerBookInfo();
        try {
            manager.setCurrentLibrary(currentLibrary);
            manager.makeInformation(book_id, imageView, txtInfo);
        } catch (XMLStreamException | SQLException | IOException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION,"Error.", ex.getMessage());
        }
    }

    public void setCurrentLibrary(Library lib) {
        currentLibrary = lib;
    }

    public void setCurrentBook(Book book) {
        currentBook = book;
    }

    public void hiddenBtnAdd() {
        btnAdd.setVisible(false);
    }

    public void setDlgStage(Stage stage) {
        dlgStage = stage;
    }
}
