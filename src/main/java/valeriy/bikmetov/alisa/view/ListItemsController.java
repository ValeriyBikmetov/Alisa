package valeriy.bikmetov.alisa.view;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import valeriy.bikmetov.alisa.Alisa;
import valeriy.bikmetov.alisa.model.*;
import valeriy.bikmetov.alisa.utilites.BookUtil;
import valeriy.bikmetov.alisa.utilites.Constants;
import valeriy.bikmetov.alisa.utilites.H2Operations;
import valeriy.bikmetov.alisa.utilites.Utilities;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

public class ListItemsController implements IObserver {
    private Library currentLibrary; // Текущая открытая библиотека
    private int currentIndex = 0;   // Текущая выбранная позиция в списке
    private Constants.TABS tabIndex; // Индекс вкладки на которой расположен список
    private PreparedStatement preparedStatement = null;
    private String currentChar = null; // Начальный символ в строке поиска
    private MainWinController mainView; // Ссылка на главное окно программы

    @FXML
    private TextField textSearch; // Поле ввода для строки поиска
    @FXML
    private ListView<Item> listItems; // Список
    @FXML
    private Button refreshButton; // Кнопка поиска

    @FXML
    public void initialize() {
        // Назначаем слушателя изменения выбранной позиции в списке
        listItems.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Item>(){
            @Override
            public void changed(ObservableValue<? extends Item> observable, Item oldValue, Item newValue) {
                int index = listItems.getSelectionModel().getSelectedIndex();
                if(index >= 0) {
                    Item item = listItems.getSelectionModel().getSelectedItem();
                    currentIndex = index; // Запоминаем выбранную позицию
                    mainView.getCurrentItem().setValue(newValue); // Передаем выбранное значение в главное окно программы
                }
            }
        });
        // Слушатель изменения строки поиска
        textSearch.setOnKeyReleased((KeyEvent event) -> {
            String text = textSearch.getText() + event.getCharacter();;
            if(text.isEmpty()) {
                currentIndex = 0;
                mainView.getCurrentItem().setValue(new Item(0,null));
            }
            if(text.length() == 2 && text.charAt(1) == '\b') {
                text = text.substring(0, 1);
                if(text.equals(currentChar)) {
                    currentIndex = 0;
                    listItems.scrollTo(currentIndex);
                    listItems.getSelectionModel().select(currentIndex);
                }
            }
            String first = text.substring(0, 1).toUpperCase();
            if (currentChar == null || !currentChar.equals(first)) { // Новый список
                currentChar = first;
                currentIndex = 0;
                fillListItems();
                listItems.scrollTo(currentIndex);
                listItems.getSelectionModel().select(currentIndex);
                if (!listItems.getItems().isEmpty()) {
                    listItems.getSelectionModel().selectFirst();
                    currentIndex = 0;
                }
            } else { // Иначе ищем введенное значение в списке
                findInListItems(text);
            }
        });
        // При нажатии клавиши Enter в поле поиска - устанавливаем фокус на выбранное значение
        textSearch.setOnAction((ActionEvent event) -> {
            if(!listItems.getItems().isEmpty()) {
                listItems.getFocusModel().focus(currentIndex);
            }
        });
        // Перезагружаем список и если он не пуст выбираем позицию согласно строки поиска
        refreshButton.setOnAction((ActionEvent event) -> {
            fillListItems();

            if(!listItems.getItems().isEmpty()) {
                String text = textSearch.getText();
                if(!text.isEmpty()) {
                    findInListItems(text);
                }
            }
        });
    }

    @FXML
    private void onAddItem() throws SQLException {
        if(tabIndex == Constants.TABS.TAB_AUTHOR) {
            Author author = Utilities.getNewAuthor(currentLibrary.getConnection());
            assert author != null;
            if(author.getId() > 0) {
                currentChar = author.getName().substring(0, 1).toUpperCase();
                currentIndex = 0;
                fillListItems();
                textSearch.setText(author.getName());
                findInListItems(textSearch.getText());
            }
        } else {
            Series series = Utilities.getNewSeries(currentLibrary.getConnection());
            assert series != null;
            if(series.getId() > 0) {
                currentChar = series.getName().substring(0, 1).toUpperCase();
                currentIndex = 0;
                fillListItems();
                textSearch.setText(series.getName());
                findInListItems(textSearch.getText());
            }
        }
    }

    @FXML
    private void onDeleteItem(){
        Item item = (Item) listItems.getSelectionModel().getSelectedItem();
        int id = item.getId();
        if(tabIndex == Constants.TABS.TAB_AUTHOR) {
            BookUtil.deleteAuthorWithBooks(currentLibrary, id);
        } else if(tabIndex == Constants.TABS.TAB_SERIES) {
            BookUtil.deleteSeriesWithBooks(currentLibrary, id);
        }
        listItems.getItems().remove(currentIndex);
        fillListItems();
        --currentIndex;
        if (currentIndex > 0) {
            listItems.scrollTo(currentIndex);
            listItems.getSelectionModel().select(currentIndex);
        }
        else currentIndex = 0;
    }

    @FXML
    private void onChangeName() throws SQLException {
        var index = listItems.getSelectionModel().getSelectedIndex();
        if (index == -1)
            return;
        if (tabIndex == Constants.TABS.TAB_AUTHOR) {
            var item = listItems.getSelectionModel().getSelectedItem();
            Author author = H2Operations.makeAuthor(currentLibrary.getConnection(), item.getId());
            String name = Utilities.changeAuthorName(item.getName());
            if (name != null) {
                author.setName(name.replace("'", "`"));
                H2Operations.changeAuthor(currentLibrary.getConnection(), author);
                item.setName(name);
                fillListItems();
                textSearch.setText(author.getName());
                findInListItems(textSearch.getText());
            }
        } else {
            var item = listItems.getSelectionModel().getSelectedItem();
            Series series = new Series(item.getName(), item.getId());
            String name = Utilities.changeSeriesName(item.getName());
            if (name != null) {
                series.setName(name.replace("'", "`"));
                H2Operations.changeSeries(currentLibrary.getConnection(), series);
                item.setName(name);
                fillListItems();
                textSearch.setText(series.getName());
                findInListItems(textSearch.getText());
            }
        }
    }

    @FXML
    private void onReplacementItem() throws IOException {
        Item item = listItems.getSelectionModel().getSelectedItem();
        int id = item.getId();
        String name = item.getName();
        Stage dialogStage = new Stage();
        dialogStage.setTitle("Выбрать для замены");
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(Alisa.getPrimaryStage());
        FXMLLoader loader;
        if (tabIndex == Constants.TABS.TAB_AUTHOR)
            loader = new FXMLLoader(Alisa.class.getResource("ListAuthor.fxml"));
        else
            loader = new FXMLLoader(Alisa.class.getResource("ListSeries.fxml"));
        AnchorPane choiceAuthor = loader.load();
        Scene scene = new Scene(choiceAuthor);
        dialogStage.setScene(scene);
        try {
            if(tabIndex == Constants.TABS.TAB_AUTHOR) {
                ListAuthorController controller = loader.getController();
                controller.processing(dialogStage, currentLibrary, name);
                dialogStage.showAndWait();

                Author author = controller.getAuthor();
                if (author != null) {
                    Author oldAuthor = new Author(name, id);
                    H2Operations.replacmentAuthor(currentLibrary.getConnection(), oldAuthor, author);
                    H2Operations.deleteAuthor(currentLibrary.getConnection(), oldAuthor.getId());
                    currentChar = author.getName().substring(0, 1).toUpperCase();
                    currentIndex = 0;
                    fillListItems();
                    textSearch.setText(author.getName());
                    findInListItems(textSearch.getText());
                }
            } else {
                ListSeriesController controller = loader.getController();
                controller.processing(dialogStage, currentLibrary, name);
                dialogStage.showAndWait();

                Series series = controller.getSeries();
                if (series != null) {
                    Series oldSeries = new Series(name, id);
                    H2Operations.replacmentSeries(currentLibrary.getConnection(), oldSeries, series);
                    H2Operations.delSeries(currentLibrary.getConnection(), oldSeries.getId());
                    currentChar = series.getName().substring(0, 1).toUpperCase();
                    currentIndex = 0;
                    fillListItems();
                    textSearch.setText(series.getName());
                    findInListItems(textSearch.getText());
                }
            }
        } catch (SQLException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
        }
    }

    public void setTabIndex(Constants.TABS index) {
        this.tabIndex = index;
    }

    public Constants.TABS getTabIndex() {
        return this.tabIndex;
    }

    /**
     * Получаем сообщение от RootLayout. Если тело сообщения NULL -
     * значит закрываем приложение. Зарываем PreparedStatment
     * Если тело сообщения содержит новую библиотеку - закрываем
     * PreparedStatement (при этом оячищаем список, поле поиска и текущий симврол),
     * меняем библиотеку и открываем PreparedStatement в новой библиотеке.
     * @param subject - источник сообщения
     * @param object - тело сообщения
     */
    @Override
    public void refreshData(IObservable subject, Object object) {
        if(object == null) {
            // Закрываем все открытее PreparedStatement
            closePreparedStatement();
        } else if(object instanceof Library && !object.equals(currentLibrary)){
            // Закрываем все открытее PreparedStatement
            closePreparedStatement();
            // Меняем текущую библиотеку
            currentLibrary = (Library) object;
            prepStatement();
        }
    }

    /**
     * Открываем PreparedStatment со строкой запроса в зависимости от индекса панели
     */
    private void prepStatement() {
        String query;
        Connection conn = currentLibrary.getConnection();
        try {
            if(tabIndex == Constants.TABS.TAB_AUTHOR) {
                query = H2Operations.getQuery(conn, "QUERY_LIST_AUTHOR");
            } else {
                query = H2Operations.getQuery(conn, "QUERY_LIST_SERIES");
            }
            preparedStatement = currentLibrary.getConnection().prepareStatement(query);
        } catch(SQLException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION, this.getClass().getName(), ex.getMessage());
        }
    }

    /**
     * Заполняем список новыми значениями в зависимости от currentChar.
     */
    private void fillListItems() {
        if(currentChar == null) {
            return;
        }
        ObservableList<Item> items = listItems.getItems();
        if(!items.isEmpty()) {
            items.clear();
        }
        try{
            String search;
            if(currentChar.equals("_")) {
                search = "\\" + currentChar;
            } else {
                search = currentChar;
            }
            preparedStatement.setString(1, search + "%");
            try(ResultSet rs = preparedStatement.executeQuery()) {
                while(rs.next()) {
                    int id = rs.getInt(1);
                    String name = rs.getString(2);
                    Item item = new Item(id, name);
                    items.add(item);
                }
            }
        } catch(SQLException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION, this.getClass().getName(), ex.getMessage());
        }
    }

    /**
     * Поиск заданного значения в списке
     * Т.к. список получает отсортированные данные,
     * то здесь используется алгоритм двоичного поиска
     * @param text - искомое значение
     */
    private void findInListItems(String text) {
        if(text.length() == 1) {
            listItems.getSelectionModel().select(0);
            return;
        }
        Object[] items = getNames();
        if(items.length == 0) {
            return;
        }
        int last = Utilities.findItem(items, text);
        listItems.scrollTo(last);
        listItems.getSelectionModel().select(last);
    }

    private Object[] getNames() {
        List<String> names = listItems.getItems().stream().map(Item::getName).toList();
        return names.toArray();
    }
    /**
     * При закрытии PreparedStatement
     * очищаем список и поле поиска, устанавливаем в 0 текущий индекс
     * и устанавливаем в NULL текущий символ
     */
    private void closePreparedStatement() {
        listItems.getItems().clear();
        textSearch.setText(Constants.EMPTY_STRING);
        currentIndex = 0;
        currentChar = null;
        try {
            if(preparedStatement != null && !preparedStatement.isClosed()) {
                preparedStatement.close();
            }
        } catch(SQLException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION,
                    this.getClass().getName(), ex.getMessage());
        }
    }

    /**
     * Очищаем список
     */
    public void clearList() {
        listItems.getItems().clear();
    }

    /**
     * Заполняем список и возвращаемся к запомненной прежде повзиции в списке
     */
    public void fillList() {
        fillListItems();
        listItems.getSelectionModel().select(currentIndex);
        listItems.getFocusModel().focus(currentIndex);
        listItems.scrollTo(currentIndex);
    }

    /**
     * Сохраняем ссылку на главное окно программы
     */
    public void setMainView(MainWinController mainView) {
        this.mainView = mainView;
    }
}
