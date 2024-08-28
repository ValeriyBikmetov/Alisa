package valeriy.bikmetov.alisa.view;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import valeriy.bikmetov.alisa.Alisa;
import valeriy.bikmetov.alisa.model.Library;
import valeriy.bikmetov.alisa.model.Series;
import valeriy.bikmetov.alisa.utilites.BookUtil;
import valeriy.bikmetov.alisa.utilites.Constants;
import valeriy.bikmetov.alisa.utilites.H2Operations;
import valeriy.bikmetov.alisa.utilites.Utilities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

public class ListSeriesController {
    private Stage dlgStage;
    private Library currentLibrary; // Текущая открытая библиотека
    private int currentIndex = 0;   // Текущая выбранная позиция в списке
    private PreparedStatement preparedStatement = null;
    private String currentChar = null; // Начальный символ в сторке поиска
    private Series series; // Ссылка на возвращаемое значение

    @FXML
    private TextField search;
    @FXML
    private ListView<Series> listSeries;
    @FXML
    private Button btnOk;
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
        listSeries.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Series>() {
            @Override
            public void changed(ObservableValue<? extends Series> observable, Series oldValue, Series newValue) {
                int index = listSeries.getSelectionModel().getSelectedIndex();
                if(index >= 0) {
                    currentIndex = index;
                    series = newValue;
                }
            }
        });
        search.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                String text = search.getText() + event.getCharacter();
                if(text.isEmpty()) {
                    currentIndex = 0;
                    series = new Series();
                }
                if(text.length() == 2 && text.charAt(1) == '\b') {
                    text = text.substring(0, 1);
                    if(text.equals(currentChar)) {
                        currentIndex = 0;
                        listSeries.scrollTo(currentIndex);
                        listSeries.getSelectionModel().select(currentIndex);
                    }
                }
                String first = text.substring(0, 1).toUpperCase();
                if (currentChar == null || !currentChar.equals(first)) { // Новый список
                    currentChar = first;
                    currentIndex = 0;
                    fillListItems();
                    listSeries.scrollTo(currentIndex);
                    listSeries.getSelectionModel().select(currentIndex);
                    if (!listSeries.getItems().isEmpty()) {
                        listSeries.getSelectionModel().selectFirst();
                        currentIndex = 0;
                    }
                } else { // Иначе ищем введенное значение в списке
                    findInListItems(text);
                }
            }
        });
        // При нажатии клавиши Enter в поле поиска - устанавливаем фокус на выбранное значение
        search.setOnAction((ActionEvent event) -> {
            if(!listSeries.getItems().isEmpty()) {
                listSeries.getFocusModel().focus(currentIndex);
            }
        });
        btnCancel.setOnAction(((event) -> {
            series = null;
            closePreparedStatement();
            dlgStage.close();
        }));
        btnOk.setOnAction((event) -> {
            closePreparedStatement();
            makeResult();
            dlgStage.close();
        });
        mnuAdd.setOnAction(event -> {
            try {
                onAddSeries();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        mnuDel.setOnAction(event -> {
            onDelSeries();
        });
        mnuChange.setOnAction(event -> {
            try {
                onChangeSeries();
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
    public void processing(Stage stage, Library library, String seriesName) {
        this.dlgStage = stage;
        currentLibrary = library;
        String query;
        Connection conn = currentLibrary.getConnection();
        try {
            search.setText(seriesName);
            if (search.getText().isEmpty())
                currentChar = null;
            else currentChar = search.getText().substring(0,1);
            query = H2Operations.getQuery(conn, "QUERY_LIST_SERIES");
            preparedStatement = currentLibrary.getConnection().prepareStatement(query);
            fillListItems();
            findInListItems(search.getText());
        } catch(SQLException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION, this.getClass().getName(), ex.getMessage());
        }
        if(series != null && series.getId() > 0) {
            currentChar = series.getName().substring(0, 1);
            search.setText(series.getName());
            fillListItems();
            String search = series.getName();
            findInListItems(search);
        }
    }

    /**
     * Заполняем список новыми значениями в зависимости от currentChar.
     */
    private void fillListItems() {
        if(currentChar == null) {
            return;
        }
        ObservableList<Series> items = listSeries.getItems();
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
                    Series item = new Series();
                    item.setId(id);
                    item.setName(name);
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
            listSeries.getSelectionModel().select(0);
            return;
        }
        Object[] items = getNames();
        if(items.length == 0) {
            return;
        }
        int last = Utilities.findItem(items, text);
        listSeries.scrollTo(last);
        listSeries.getSelectionModel().select(last);
    }

    private Object[] getNames() {
        List<String> names = listSeries.getItems().stream().map(Series::getName).toList();
        return names.toArray();
    }

    private void onAddSeries() throws SQLException {
        Series item = Utilities.getNewSeries(currentLibrary.getConnection());
        if(item != null) {
            currentChar = item.getName().substring(0, 1).toUpperCase();
            currentIndex = 0;
            fillListItems();
            search.setText(item.getName());
            findInListItems(search.getText());
        }
    }

    private void onDelSeries() {
        BookUtil.deleteSeriesWithBooks(currentLibrary, series.getId());
        Series selected = listSeries.getSelectionModel().getSelectedItem();
        ObservableList<Series> lst = listSeries.getItems();
        lst.remove(selected);
    }

    private void onChangeSeries() throws SQLException {
        int index = listSeries.getSelectionModel().getSelectedIndex();
        if (index == -1)
            return;
        Series series1 = listSeries.getSelectionModel().getSelectedItem();
        String name = Utilities.changeSeriesName(series1.getName());
        if (name != null) {
            series1.setName(name);
            H2Operations.changeSeries(currentLibrary.getConnection(), series1);
            listSeries.getSelectionModel().select(index);
        }
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
        if(series.getId() == 0) {
            try {
                int id = H2Operations.newSeriesID(currentLibrary.getConnection(), series);
                if(id > 0) {
                    series.setId(id);
                }
            } catch(SQLException ex) {
                series = null;
                Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
                Utilities.showMessage(Constants.TITLE_EXCEPTION,
                        this.getClass().getName(), ex.getMessage());
            }
        }
    }

    public Series getSeries() {
        return series;
    }

    public void setSeries(Series value) {
        this.series = value;
    }
}
