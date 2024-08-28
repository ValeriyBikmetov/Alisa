package valeriy.bikmetov.alisa.view;

import javafx.collections.ObservableList;
import valeriy.bikmetov.alisa.Alisa;
import valeriy.bikmetov.alisa.model.*;
import valeriy.bikmetov.alisa.utilites.*;
import valeriy.bikmetov.alisa.utilites.Constants.TABS;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.logging.Level;

import static valeriy.bikmetov.alisa.utilites.FileUtil.directoryChooser;

/**
 *
 * @author Валерий Бикметов
 */
public class MainWinController implements IObserver {
    // Текущая открытая вкладка (автор, серия или жанр)
    private final ObjectProperty<TABS> currentTab = new SimpleObjectProperty<>(TABS.TAB_AUTHOR);
    // Выбранный элемент списка
    private final ObjectProperty<Item> currentItem = new SimpleObjectProperty<>(new Item(0, null));
    // Идентификатор выбранной книги
    private final IntegerProperty currentBook = new SimpleIntegerProperty(0);

    private Library currentLibrary;
    private ListItemsController listAuthor;
    private ListItemsController listSeries;
    private TreeGenresController treeGenres;
    // Создание списка книг для выбранного автора, серии или жанра
    private ManagerBookList managerBookList;
    // Создание и представление информации по выбранной книге
    private ManagerBookInfo managerBookInfo;
    private Stage primaryStage;

    @FXML
    private TabPane tabPane; // Панель вкладок со списками
    @FXML
    private Tab tabAuthor; // Вкладка авторов
    @FXML
    private Tab tabSeries; // Вкладка серий
    @FXML
    private Tab tabGenre; // Вкладка жанров
    @FXML
    private TableView<IRowBookList> tableView; // Список книг
    @FXML
    private MenuItem mnuDelBook;
    @FXML
    private MenuItem mnuChangeBookProp;
    @FXML
    private MenuItem mnuCopyBook;
    @FXML
    private MenuItem mnuReadBook;
    @FXML
    private MenuItem mnuAddBook;
    @FXML
    private ImageView imageView; // Изображение обложки
    @FXML
    private TextArea annotation; // Текстовая информация о книге

    /**
     * Определяем обработку событий выбора вкладки (автора, серии или жанра),
     * при выборе какой-либо вкладки приваиваем идентификатор открываемой
     * вкладки свойству currentTab.
     * Назначаем слушателя событию изменения свойства currentTab. По этому
     * событию: - очищаем список на закрывающейся вкладке и заполняем список
     * на открываемой вкладке, формируем соостветствующие колонки в таблице.
     * Назначаем слушателя свойству currentItem (владелец книг), котрое
     * изменяется из открытого списка. Заполняем таблицу в соответствии с новым
     * значением currentItem.
     * Создаем объект класса ManagerBookList и создаем в ней колонки для вывода
     * списка книг по автору.
     * Назначаем слушателя событию изменения выбранной кники(currentBook) в таблице. По
     * этому событию формируем информацию о книге и выводим ее в соответствующие
     * окна.
     */
    @FXML
    public void initalize() {
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tabAuthor.setOnSelectionChanged((Event event) -> {
            currentTab.setValue(TABS.TAB_AUTHOR);
        });

        tabSeries.setOnSelectionChanged((Event event) -> {
            currentTab.setValue(TABS.TAB_SERIES);
        });

        tabGenre.setOnSelectionChanged((Event event) -> {
            currentTab.setValue(TABS.TAB_GENRE);
        });

        managerBookList = new ManagerBookList(MainWinController.this, tableView);
        managerBookList.prepareColumns(TABS.TAB_AUTHOR);
        managerBookInfo = new ManagerBookInfo();
        currentTab.addListener((ObservableValue<? extends TABS> observable, TABS oldValue, TABS newValue) -> {
            try {
                managerBookInfo.makeInformation(0, imageView, annotation);
            } catch (SQLException | IOException | XMLStreamException ex) {
                Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            }
            switch (oldValue) {
                case TAB_AUTHOR:
                    listAuthor.clearList();
                    break;
                case TAB_SERIES:
                    listSeries.clearList();
                    break;
            }
            managerBookList.prepareColumns(newValue);
            switch (newValue) {
                case TAB_AUTHOR:
                    listAuthor.fillList();
                    break;
                case TAB_SERIES:
                    listSeries.fillList();
                    break;
                case TAB_GENRE:
                    treeGenres.getSelectedItem();
                    break;
            }
        });
        // Обработка события изменения выбранной книги
        currentBook.addListener((ObservableValue<? extends Number> prop, Number oldValue, Number newValue) -> {
            try {
                managerBookInfo.makeInformation(newValue.intValue(), imageView, annotation);
            } catch (SQLException | IOException | XMLStreamException ex) {
                Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            }
        });
        // Обработка события изменения выбранного автора, серии или жанра
        currentItem.addListener((ObservableValue<? extends Item> observable, Item oldValue, Item newValue) -> {
            managerBookList.fillTable(currentTab.get(), newValue.getId());
            imageView.setImage(null);
            annotation.clear();
            tableView.getSelectionModel().selectFirst();
            try {
                managerBookInfo.makeInformation(0, imageView, annotation);
            } catch (SQLException | IOException | XMLStreamException ex) {
                Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            }
        });
        mnuDelBook.setOnAction(event -> onDeleteBook());
        mnuChangeBookProp.setOnAction(event -> onChangeBookProperty());
        mnuCopyBook.setOnAction(event -> onCopyBook());
        mnuReadBook.setOnAction(event -> onReadBook());
        mnuAddBook.setOnAction(event -> onAddBook());
    }

    private void onChangeBookProperty(){
        if (tableView.getSelectionModel().getSelectedIndices().size() > 1) {
            Utilities.showMessage("Error", "Error", "Выберите одну книгу");
            return;
        }
        int index = tableView.getSelectionModel().getSelectedIndex();
        int bookid = managerBookInfo.getBook_id();
        if(bookid != 0) {
            try {
                FXMLLoader loader = new FXMLLoader(Alisa.class.getResource("BookInfoDlg.fxml"));
                AnchorPane bookInfo = loader.load();
                Stage dlgInfo = new Stage();
                dlgInfo.setTitle("Свойства книги");
                dlgInfo.initModality(Modality.WINDOW_MODAL);
                dlgInfo.initOwner(primaryStage);
                Scene scene = new Scene(bookInfo);
                dlgInfo.setScene(scene);
                BookInfoDlgController controller = loader.getController();
                controller.setDlgStage(dlgInfo);
                controller.setCurrentLibrary(currentLibrary);
                controller.setImage(managerBookInfo.getImage());
                controller.textArea.setText(managerBookInfo.getCurrentBook().getAnnotation());
                controller.setCurrentBookInfo(bookid);
                dlgInfo.showAndWait();
            } catch(IOException ex) {
                Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
                Utilities.showMessage(Constants.TITLE_EXCEPTION, this.getClass().getName(), ex.getMessage());
            }
        }
        int currentId = currentItem.get().getId();
        managerBookList.fillTable(currentTab.get(), currentId);
        tableView.getSelectionModel().select(index);
    }

    private void onDeleteBook(){
        var action = Utilities.makeIt("Удаление книги", "Вы уверены?");
        if (!action) return;;
        var sel = tableView.getSelectionModel();
        ObservableList<IRowBookList> items = sel.getSelectedItems();
        try {
            for (IRowBookList item : items) {
                var book_id = item.getBookid();
                var book = H2Operations.makeBook(currentLibrary.getConnection(), book_id);
                H2Operations.deleteBook(currentLibrary.getConnection(), book);
                FileUtil.deleteFileAndFolder(currentLibrary, book);
            }
            tableView.getItems().removeAll(items);
            if (!sel.isEmpty()) {
                sel.selectFirst();
            }
        } catch (SQLException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION,"Error.", ex.getMessage());
        }
    }

    private void onCopyBook() {
        var sel = tableView.getSelectionModel();
        ObservableList<IRowBookList> items = sel.getSelectedItems();
        var target = directoryChooser(Alisa.getPrimaryStage(), "Куда копируем?", null);
        if (target == null) return;
        for (IRowBookList item : items) {
            var book_id = item.getBookid();
            BookUtil.copyBook(currentLibrary, book_id, target);
        }
    }

    private void onReadBook() {
        if (tableView.getSelectionModel().getSelectedIndices().size() > 1) {
            Utilities.showMessage("Error", "Error", "Выберите одну книгу");
            return;
        }
        int book_id = tableView.getSelectionModel().getSelectedItem().getBookid();
        if (book_id < 1) return;
        BookUtil.readOfBook(currentLibrary, book_id);
    }

    private void onAddBook() {
        var folder = Constants.CURRENT_PATH;
        var fileOfBook = FileUtil.fileChooser(Alisa.getPrimaryStage(), "Выбрать книгу", folder, true);
        if (fileOfBook == null)
            return;
        Constants.CURRENT_PATH = fileOfBook.getParent();
        var managerBookInfo = new ManagerBookInfo();
        FlagsActions flagsActions = new FlagsActions();
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
            controller.setCurrentLibrary(currentLibrary);
            managerBookInfo.makeInformation(fileOfBook, controller.imageView, controller.textArea);
            controller.setNewBookInfo(managerBookInfo.getCurrentBook());
            controller.setFlagsActions(flagsActions);
            dlgInfo.showAndWait();
            managerBookList.fillTable(currentTab.get(), currentItem.get().getId());
        } catch(IOException | XMLStreamException | SQLException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION, this.getClass().getName(), ex.getMessage());
        }
    }
    
    /**
     * Возвращаем в главный класс ссылку TabPane для заполнения вкладок списками
     * @return - TabPane
     */
    public TabPane getTabPane() {
        return tabPane;
    }

    /**
     * Получаем сообщения от RootLayOut о смене библиотек и закрытии приложения.
     * Если объект сообщения null - значит закрываем приложение. Закрываем все
     * открытые PreparedStatement. Иначе переподгатавливаем все необходимые
     * PreparedStatement.
     * @param subject - источник сообщения
     * @param object - тело сообщения
     */
    @Override
    public void refreshData(IObservable subject, Object object) {
        if (object == null) { // Выключаемся
            try {
                managerBookList.closePrepStatements();
//                managerBookInfo.closePreparedStatement();
            } catch(SQLException ex) {
                Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
                Utilities.showMessage(Constants.TITLE_EXCEPTION, this.getClass().getName(), ex.getMessage());
            }
        } else if (!object.equals(currentLibrary)) { // Смена библиотек
            try {
                // Меняем библиотеку
                currentLibrary = (Library) object;
                managerBookList.prepareStatements(currentLibrary.getConnection());
                managerBookInfo.setCurrentLibrary(currentLibrary);
            } catch (SQLException ex) {
                Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
                Utilities.showMessage(Constants.TITLE_EXCEPTION, this.getClass().getName(), ex.getMessage());
            }
        }
    }

    /**
     * Запоминаем ссылку на список авторов
     */
    public void setListAuthor(ListItemsController listAuthor) {
        this.listAuthor = listAuthor;
    }

    /**
     * Запоминаем ссылку на список серий
     */
    public void setListSeries(ListItemsController listSeries) {
        this.listSeries = listSeries;
    }

    /**
     * Запоминаем ссылку на дерево жанров
     */
    public void setTreeGenres(TreeGenresController treeGenres) {
        this.treeGenres = treeGenres;
    }

    /**
     * Посылаем ссылку на совйство currentItem (текущий владелец книг (автор,
     * серия, жанр))
     * @return - наблюдаемое свойство
     */
    public ObjectProperty<Item> getCurrentItem() {
        return this.currentItem;
    }

    /**
     * Возвращаем ссылку на свойство currentBook - выбранная книга
     * @return - наблюдаемое свойство
     */
    public SimpleIntegerProperty getCurrentBook() {
        return (SimpleIntegerProperty) this.currentBook;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
}
