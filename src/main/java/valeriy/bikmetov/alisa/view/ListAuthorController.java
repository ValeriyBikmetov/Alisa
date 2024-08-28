package valeriy.bikmetov.alisa.view;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import valeriy.bikmetov.alisa.Alisa;
import valeriy.bikmetov.alisa.model.Author;
import valeriy.bikmetov.alisa.model.Library;
import valeriy.bikmetov.alisa.utilites.Constants;
import valeriy.bikmetov.alisa.utilites.H2Operations;
import valeriy.bikmetov.alisa.utilites.Utilities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

import static valeriy.bikmetov.alisa.utilites.BookUtil.deleteAuthorWithBooks;

/**
 *
 * @author valer
 */
public class ListAuthorController {
    private Stage dlgStage;
    private Library currentLibrary; // Текущая открытая библиотека
    private int currentIndex = 0;   // Текущая выбранная позиция в списке
    private PreparedStatement preparedStatement = null;
    private String currentChar = null; // Начальный символ в строке поиска
    private Author author; // Ссылка на возвращаемое значение

    @FXML
    public TextField textName; // Поле ввода для строки поиска
    @FXML
    private ListView<Author> listAuthor; // Список
    @FXML
    private Button btnSelected;
    @FXML
    private Button btnCancel;
    @FXML
    private MenuItem mnuAdd;
    @FXML
    private MenuItem mnuDel;
    @FXML
    private MenuItem mnuChange;

    @FXML
    public void initialize() {
        // Назначаем слушателя изменения выбранной позиции в списке
        listAuthor.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            int index = listAuthor.getSelectionModel().getSelectedIndex();
            if(index >= 0) {
                currentIndex = index; // Запоминаем выбранную позицию
                author = newValue;
            }
        });
        // Слушатель изменения строки поиска
        textName.setOnKeyReleased(event -> {
            String text = textName.getText() + event.getCharacter();
            if(text.isEmpty()) {
                currentIndex = 0;
                author = new Author();
            }
            if(text.length() == 2 && text.charAt(1) == '\b') {
                text = text.substring(0, 1);
                if(text.equals(currentChar)) {
                    currentIndex = 0;
                    listAuthor.scrollTo(currentIndex);
                    listAuthor.getSelectionModel().select(currentIndex);
                }
            }
            String first = text.substring(0, 1).toUpperCase();
            if (currentChar == null || !currentChar.equals(first)) { // Новый список
                currentChar = first;
                currentIndex = 0;
                fillListItems();
                listAuthor.scrollTo(currentIndex);
                listAuthor.getSelectionModel().select(currentIndex);
                if (!listAuthor.getItems().isEmpty()) {
                    listAuthor.getSelectionModel().selectFirst();
                    currentIndex = 0;
                }
            } else { // Иначе ищем введенное значение в списке
                findInListItems(text);
            }
        });
        // При нажатии клавиши Enter в поле поиска - устанавливаем фокус на выбранное значение
        textName.setOnAction((ActionEvent event) -> {
            if(!listAuthor.getItems().isEmpty()) {
                listAuthor.getFocusModel().focus(currentIndex);
            }
        });
        //textName.setText(Constants.LIST_AUTHOR_NAME);
        btnCancel.setOnAction(((event) -> {
            author = null;
            closePreparedStatement();
            dlgStage.close();
        }));
        btnSelected.setOnAction((event) -> {
            closePreparedStatement();
            makeResult();
            dlgStage.close();
        });
        mnuAdd.setOnAction(event -> {
            try {
                onAddAuthor();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        mnuDel.setOnAction(event -> {
            onDelAuthor();
        });
        mnuChange.setOnAction(event -> {
            try {
                onChangeAuthor();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Подготавливаем диалог к работе
     * @param stage подмостки диалога
     * @param library текущая библиотека
     */
    public void processing(Stage stage, Library library, String authorName) {
        this.dlgStage = stage;
        currentLibrary = library;
        String query;
        Connection conn = currentLibrary.getConnection();
        try {
            textName.setText(authorName);
            if (textName.getText().isEmpty())
                    currentChar = null;
            else currentChar = textName.getText().substring(0,1);
            query = H2Operations.getQuery(conn, "QUERY_LIST_AUTHOR");
            preparedStatement = currentLibrary.getConnection().prepareStatement(query);
            fillListItems();
            findInListItems(authorName);
        } catch(SQLException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION, this.getClass().getName(), ex.getMessage());
        }
    }

    public Author getAuthor() {
        return this.author;
    }

    public void setAuthor(Author value) {
        this.author = value;
    }

    /**
     * Заполняем список новыми значениями в зависимости от currentChar.
     */
    private void fillListItems() {
        if(currentChar == null) {
            return;
        }
        ObservableList<Author> items = listAuthor.getItems();
        if(!items.isEmpty()) {
            items.clear();
        }
        try{
            String search;
            if (currentChar.equals("_")) {
                search = "\\" + currentChar;
            } else {
                search = currentChar;
            }
            preparedStatement.setString(1, search + "%");
            try(ResultSet rs = preparedStatement.executeQuery()) {
                while(rs.next()) {
                    int id = rs.getInt(1);
                    String name = rs.getString(2);
                    Author item;
                    item = new Author(name, id);
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
    public void findInListItems(String text) {
        if (text == null || text.isEmpty())
            return;
        if(text.length() == 1) {
            listAuthor.getSelectionModel().select(0);
            return;
        }
        Object[] items = getNames();
        if(items.length == 0) {
            return;
        }
        int last = Utilities.findItem(items, text);
        listAuthor.scrollTo(last);
        listAuthor.getSelectionModel().select(last);
    }

    private Object[] getNames() {
        List<String> names = listAuthor.getItems().stream().map(Author::getName).toList();
        return names.toArray();
    }

    /**
     * При закрытии PreparedStatement
     * очищаем список и поле поиска, устанавливаем в 0 текущий индекс
     * и устанавливаем в NULL текущий символ
     */
    private void closePreparedStatement() {
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

    private void makeResult() {
        if(author.getId() == 0) {
            try {
                int id = H2Operations.newAuthorID(currentLibrary.getConnection(), author);
                if(id > 0) {
                    author.setId(id);
                }
            } catch(SQLException ex) {
                author = null;
                Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
                Utilities.showMessage(Constants.TITLE_EXCEPTION,
                        this.getClass().getName(), ex.getMessage());
            }
        }
    }

    /**
     * Создать нового автора.
     * Если фамилия нового автора начинается с той же буквы - просто вставить его в список
     * Иначе: очистить список, установить новый начальный символ, ввести в строку поиска ФИО нового автора,
     * зааполнить список и призвести поиск нового автора
     */
    private void onAddAuthor() throws SQLException {
        Author author = Utilities.getNewAuthor(currentLibrary.getConnection());
        assert author != null;
        if(author.getId() > 0) {
            currentChar = author.getName().substring(0, 1).toUpperCase();
            currentIndex = 0;
            fillListItems();
            textName.setText(author.getName());
            findInListItems(textName.getText());
        }
    }

    private void onDelAuthor() {
        deleteAuthorWithBooks(currentLibrary, author.getId());
        Author selected = listAuthor.getSelectionModel().getSelectedItem();
        ObservableList<Author> items = listAuthor.getItems();
        items.remove(selected);
    }
    private void onChangeAuthor() throws SQLException {
        int index = listAuthor.getSelectionModel().getSelectedIndex();
        Author author = listAuthor.getSelectionModel().getSelectedItem();
        String name = Utilities.changeAuthorName(author.getName());
        if (name != null) {
            author.setName(name);
            H2Operations.changeAuthor(currentLibrary.getConnection(), author);
            listAuthor.getSelectionModel().select(index);
        }
    }
}

