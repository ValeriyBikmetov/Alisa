package valeriy.bikmetov.alisa.view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import valeriy.bikmetov.alisa.Alisa;
import valeriy.bikmetov.alisa.model.FlagsActions;
import valeriy.bikmetov.alisa.model.IObservable;
import valeriy.bikmetov.alisa.model.IObserver;
import valeriy.bikmetov.alisa.model.Library;
import valeriy.bikmetov.alisa.utilites.*;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;

/**
 * FXML Controller class
 *
 * @author Валерий
 */

public class RootLayoutController implements IObservable {
    private final ArrayList<IObserver> observers = new ArrayList<>();

    @FXML
    private ComboBox<Library> libraries;
    @FXML
    private TextField findBook;
    @FXML
    private MenuItem mnuAddBook;
    @FXML
    private MenuItem mnuAddBookNoCopy;
    @FXML
    private MenuItem mnuAddBooks;
    @FXML
    private MenuItem mnuAddBooksNoCopy;
    @FXML
    private MenuItem mnuClose;

    /**
     * Заполняем ComboBox созданными объектами библиотек.
     * Считываем из файла свойств информацию о расположении папки с файлами
     * баз данных и папке временных файлов.
     * Запоминаем эти данные в статических полях класса Utilities.
     * Затем проходим по папке с базами данных и подключаем каждую из найденных
     * библиотек
     */
    @FXML
    public void initialize() throws IOException {
        Utilities.readProperty();
        Path pathDb = Path.of(Constants.FOLDER_DB);
        int lenPrefix = Constants.PREFIX_DB.length();
        Files.walkFileTree(pathDb, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String name = file.getFileName().toString();
                if (name.endsWith(Constants.SUFFIX_DB) && name.startsWith(Constants.PREFIX_DB)) {
                    String num = name.substring(lenPrefix, lenPrefix + 2);
                    Library lib = make_alisa(pathDb, num);
                    libraries.getItems().add(lib);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        if(!libraries.getItems().isEmpty()) {
            libraries.getSelectionModel().select(0);
        }
        mnuClose.setOnAction(event -> onClose());
        mnuAddBook.setOnAction(event -> onAddBook(false));
        mnuAddBookNoCopy.setOnAction(event -> onAddBook(true));
        mnuAddBooks.setOnAction(event -> onAddBooks(false));
        mnuAddBooksNoCopy.setOnAction(event -> onAddBooks(true));
        findBook.setOnKeyReleased(keyEvent -> {
            var key = keyEvent.getCode();
            if (key == KeyCode.ENTER) getFindBook(findBook.getText());
        });
    }

    /**
     * Реагируем на изменение выбора текущей библиотеки, посылкой сообщения
     */
    @FXML
    private void changeItem() {
        notifyObservers(libraries.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void onClose() {
        Platform.exit();
    }

    @FXML
    private void onAddBook(boolean flagCopy) {
        var folder = Constants.CURRENT_PATH;
        var fileOfBook = FileUtil.fileChooser(Alisa.getPrimaryStage(), "Выбрать книгу", folder, true);
        if (fileOfBook == null)
            return;
        if (flagCopy && !correctionLayout(fileOfBook))
            return;
        Constants.CURRENT_PATH = fileOfBook.getParent();
        var managerBookInfo = new ManagerBookInfo();
        FlagsActions flagsActions = new FlagsActions();
        flagsActions.setNoCopy(flagCopy);
        try {
            FXMLLoader loader = new FXMLLoader(Alisa.class.getResource("BookInfoDlg.fxml"));
            AnchorPane bookInfo = loader.load();
            Stage dlgInfo = new Stage();
            dlgInfo.setTitle("Свойства книги");
            dlgInfo.initModality(Modality.WINDOW_MODAL);
            dlgInfo.initOwner(Alisa.getPrimaryStage());
            Scene scene = new Scene(bookInfo);
            dlgInfo.setScene(scene);
            BookInfoDlgController controller = loader.getController();
            controller.setDlgStage(dlgInfo);
            controller.setCurrentLibrary(libraries.getValue());
            managerBookInfo.makeInformation(fileOfBook, controller.imageView, controller.textArea);
            controller.setNewBookInfo(managerBookInfo.getCurrentBook());
            controller.setFlagsActions(flagsActions);
            dlgInfo.showAndWait();
        } catch(IOException | XMLStreamException | SQLException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION, this.getClass().getName(), ex.getMessage());
        }
    }

    @FXML
    private void onAddBooks(boolean flagCopy) {
        var folder = Constants.CURRENT_PATH;
        var folderOfBooks = FileUtil.directoryChooser(Alisa.getPrimaryStage(), "Выбор каталога", folder);
        if (folderOfBooks == null)
            return;
        if (flagCopy && !correctionLayout(folderOfBooks))
            return;
        Constants.CURRENT_PATH = Path.of(folderOfBooks.toString());
        // Список файлов в каталоге
        ArrayList<String> list = FileUtil.listFilesOfDir(folderOfBooks.toString());
        if (list.isEmpty())
            return;
        FlagsActions flagsActions = new FlagsActions();
        flagsActions.setNoCopy(flagCopy);
        try {
            FXMLLoader loader = new FXMLLoader(Alisa.class.getResource("BooksOfDir.fxml"));
            GridPane pane = loader.load();
            Stage lstStage = new Stage();
            lstStage.setTitle("Список книг для обработки");
            lstStage.initModality(Modality.WINDOW_MODAL);
            lstStage.initOwner(Alisa.getPrimaryStage());
            Scene scene = new Scene(pane);
            lstStage.setScene(scene);
            BooksOfDirController controller = loader.getController();
            controller.setDlgStage(lstStage);
            controller.setCurrentLibrary(libraries.getValue());
            controller.fillListSource(list);
            controller.setFlagsActions(flagsActions);
            lstStage.showAndWait();
        } catch(IOException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION, this.getClass().getName(), ex.getMessage());
        }
    }

    public void getFindBook(String bookName) {
        ArrayList<Integer> findBooks = null;
        try {
            findBooks = H2Operations.findBooks(libraries.getValue().getConnection(), bookName);
        } catch (SQLException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION,"Ошибка поиска аналогичной книги.",
                    ex.getMessage());
            return;
        }
        if (!findBooks.isEmpty()) {
            try {
                FXMLLoader loader = new FXMLLoader(Alisa.class.getResource("FindBooks.fxml"));
                AnchorPane findInfo = loader.load();
                Stage dlgFind = new Stage();
                dlgFind.setTitle("Найденные книги");
                dlgFind.initModality(Modality.WINDOW_MODAL);
                dlgFind.initOwner(Alisa.getPrimaryStage());
                Scene scene = new Scene(findInfo);
                dlgFind.setScene(scene);
                FindBooksController controller = loader.getController();
                controller.setDlgStage(dlgFind);
                controller.hiddenBtnAdd();
                controller.setCurrentLibrary(libraries.getValue());
                controller.fillListBooks(findBooks);
                dlgFind.showAndWait();
            } catch (IOException ex) {
                Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
                Utilities.showMessage(Constants.TITLE_EXCEPTION, this.getClass().getName(), ex.getMessage());
            }
        }
    }

    /**
     * @return ComboBox с активными библиотеками
     */
    public ComboBox<Library> getLibraries() {
        return this.libraries;
    }

    /**
     * Создаем объект библиотеки
     * @param pathDb - путь к библиотеки
     * @param num - порядковый номер библиотеки
     * @return - созданный объект библиотеки
     */
    private Library make_alisa(Path pathDb, String num) {
        Library lib = new Library();
        String url = Constants.PREFIX_URL_DB + pathDb + File.separator + String.format(Constants.PREFIX_DB + "%s", num);
        try {
            lib.createConnection(url);
            ArrayList<Object> params = H2Operations.getLibParams(lib.getConnection());
            int index = (Integer) params.get(0);
            String name = (String) params.get(1);
            String folder = (String) params.get(2);
            String typeLib = (String) params.get(3);
            while (folder == null || !Files.exists(Path.of(folder))) {
                Path path = FileUtil.directoryChooser(Alisa.getPrimaryStage(), "Выбор пути к книгам", null);
                if (path == null) continue;
                H2Operations.updatePathTo(lib.getConnection(), path);
                folder = path.toString();
            }
            lib.setIndexInDb(index);
            lib.setNameLibrary(name);
            lib.setPathToBooks(Path.of(folder));
            lib.setPathToDb(pathDb);
            lib.setTypeLib(typeLib);
        } catch(SQLException | ClassNotFoundException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION, "Ошибка создания соединения с базой!", ex.getMessage());
        }
        return lib;
    }

    /**
     * Посылаем сообщение о выборе новой активной библиотеки
     * @param obj - объект активной библиотеки
     */
    @Override
    public void notifyObservers(Object obj) {
        observers.forEach(obs -> obs.refreshData(this, obj));
    }

    /**
     * Регистрируем слушателя
     * @param obs - объект слушателя
     */
    @Override
    public void register(IObserver obs) {
        observers.add(obs);
    }

    /**
     * Исключаем наблюдателя из списка
     * @param obs - объект наблюдателя
     */
    @Override
    public void unRegister(IObserver obs) {
        observers.remove(obs);
    }

    /**
     * Активизация работы приложения, путем имитации выбора активной библитоеки
     */
    public void startApp() {
        changeItem();
    }

    private boolean correctionLayout(Path fileName) {
        var root = libraries.getValue().getPathToBooks();
        if (!fileName.startsWith(root)) {
            Utilities.showMessage("Error", null, "Файлы книг уже должны быть в каталоге библиотеки");
            return false;
        }
        return true;
    }
}
