package valeriy.bikmetov.alisa.utilites;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import valeriy.bikmetov.alisa.Alisa;
import valeriy.bikmetov.alisa.model.IRowBookList;
import valeriy.bikmetov.alisa.model.RowBookAuthor;
import valeriy.bikmetov.alisa.model.RowBookGenre;
import valeriy.bikmetov.alisa.model.RowBookSeries;
import valeriy.bikmetov.alisa.utilites.Constants.TABS;
import valeriy.bikmetov.alisa.view.MainWinController;

/**
 *
 * @author Валерий Бикметов
 */
public class ManagerBookList {
    private final TableView tableView;

    private PreparedStatement prepBookAuthor;
    private PreparedStatement prepBookGenre;
    private PreparedStatement prepBookSeries;
    private PreparedStatement prepGenre;
    private PreparedStatement prepSeries;
    private PreparedStatement prepAuthor;

    /**
     * Управляет фоормированием колонок таблицы и ее заполнением. 
     * @param mainView
     * @param tableView - ссылка на TableView из MainView
     */
    public ManagerBookList(MainWinController mainView, TableView tableView) {

        this.tableView = tableView;
        tableView.getSelectionModel().selectedItemProperty().addListener((ChangeListener<IRowBookList>) (observable, oldValue, newValue) -> {
            // TODO Получить свойства книги и ее обложку
            if(mainView != null && newValue != null) {
                mainView.getCurrentBook().setValue(newValue.getBookid());
            }
        });
    }

    /**
     * Подготавливает PreparedStatement для заполнения таблицы при смене
     * библиотеки. В начале закрывает все открытые PreparedStatement исплдьзуя
     * процедуру closePrepStatement
     * @param conn - Connection текущей библиотеки
     * @throws SQLException
     */
    public void prepareStatements(Connection conn) throws SQLException {
        closePrepStatements();
        String query = H2Operations.getQuery(conn, "QUERY_BOOK_AUTHOR");
        prepBookAuthor = conn.prepareStatement(query);
        query = H2Operations.getQuery(conn, "QUERY_BOOK_GENRE");
        prepBookGenre = conn.prepareStatement(query);
        query = H2Operations.getQuery(conn, "QUERY_BOOK_SERIES");
        prepBookSeries = conn.prepareStatement(query);
        query = H2Operations.getQuery(conn, "QUERY_AUTHOR");
        prepAuthor = conn.prepareStatement(query);
        query = H2Operations.getQuery(conn, "QUERY_GENRE");
        prepGenre = conn.prepareStatement(query);
        query = H2Operations.getQuery(conn, "QUERY_SERIES");
        prepSeries = conn.prepareStatement(query);
    }

    /**
     * Закрывает все открытые PreparedStatement
     * @throws SQLException
     */
    public void closePrepStatements() throws SQLException {
        clearTable();
        if(prepBookAuthor != null && !prepBookAuthor.isClosed()) {
            prepBookAuthor.close();
        }
        if(prepBookGenre != null && !prepBookGenre.isClosed()) {
            prepBookGenre.close();
        }
        if(prepBookSeries != null && !prepBookSeries.isClosed()) {
            prepBookSeries.close();
        }
        if(prepAuthor != null && !prepAuthor.isClosed()) {
            prepAuthor.close();
        }
        if(prepGenre != null && !prepGenre.isClosed()) {
            prepGenre.close();
        }
        if(prepSeries != null && !prepSeries.isClosed()) {
            prepSeries.close();
        }
    }

    /**
     * Удаляет данные таблицы
     */
    private void clearTable() {
        if(!tableView.getItems().isEmpty()) {
            tableView.getItems().clear();
        }
    }

    /**
     * Заполняе таблицу данными в соответствии с текущуй открытой вкладкой
     * и идентификаторм владельца книг (автор, серия, жанр)
     * @param tab - идентификатор вкладки
     * @param itemid - идентификатор владельца книг
     */
    public void fillTable(TABS tab, int itemid) {
        clearTable();
        try {
            switch (tab) {
                case TAB_AUTHOR:
                    fillBooksAuthor(itemid);
                    break;
                case TAB_SERIES:
                    fillBookSeries(itemid);
                    break;
                case TAB_GENRE:
                    fillBookGenres(itemid);
                    break;
            }
        } catch (SQLException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION, this.getClass().getName(), ex.getMessage());
        }
    }

    /**
     * Заполняе таблицу списком книг данного автора
     * @param authorid - идентификатор автора
     * @throws SQLException
     */
    private void fillBooksAuthor(int authorid) throws SQLException {
        prepBookAuthor.setInt(1, authorid);
        try(ResultSet rs = prepBookAuthor.executeQuery()) {
            while(rs.next()) {
                RowBookAuthor rowBookAuthor = new RowBookAuthor();
                int bookid = rs.getInt(1);
                String title = rs.getString(2);
                rowBookAuthor.setBookid(bookid);
                rowBookAuthor.setTitle(title);
                getGenres(bookid, rowBookAuthor);
                getSeries(bookid, rowBookAuthor);
                tableView.getItems().add(rowBookAuthor);
            }
        }
    }

    /**
     * Заполняе таблицу списком книг заданной серии
     * @param seriesid - идентификатор серии
     * @throws SQLException
     */
    private void fillBookSeries(int seriesid) throws SQLException {
        prepBookSeries.setInt(1, seriesid);
        try(ResultSet rs = prepBookSeries.executeQuery()) {
            while(rs.next()) {
                RowBookSeries rowBookSeries = new RowBookSeries();
                int bookid = rs.getInt(1);
                String title = rs.getString(2);
                rowBookSeries.setBookid(bookid);
                rowBookSeries.setTitle(title);
                getGenres(bookid, rowBookSeries);
                getAuthors(bookid, rowBookSeries);
                tableView.getItems().add(rowBookSeries);
            }
        }
    }

    /**
     * Заполняе таблицу списком книг данного жанра
     * @param genreid - идентификатор жанра
     * @throws SQLException
     */
    private void fillBookGenres(int genreid) throws SQLException {
        prepBookGenre.setInt(1, genreid);
        try(ResultSet rs = prepBookGenre.executeQuery()) {
            while(rs.next()) {
                RowBookGenre rowBookGenres = new RowBookGenre();
                int bookid = rs.getInt(1);
                String title = rs.getString(2);
                rowBookGenres.setBookid(bookid);
                rowBookGenres.setTitle(title);
                getSeries(bookid, rowBookGenres);
                getAuthors(bookid, rowBookGenres);
                tableView.getItems().add(rowBookGenres);
            }
        }
    }

    /**
     * Формирует списсок жанров относящихся к данной книге
     * @param bookid - идентификатор книги
     * @return - массив строк со списком жанров
     * @throws SQLException
     */
    private void getGenres(int bookid, IRowBookList rowList) throws SQLException {
        prepGenre.setInt(1, bookid);
        try (ResultSet rs = prepGenre.executeQuery()) {
            while(rs.next()) {
                String genre = rs.getString(1);
                rowList.getGenres().add(genre);
            }
        }
    }

    /**
     * Формирует список серий в которые входит данная книга
     * @param bookid- идентификатор книги
     * @return - массив строк со списком серий и номерами книг в этих сериях
     * @throws SQLException
     */
    private void getSeries(int bookid, IRowBookList rowList) throws SQLException {
        prepSeries.setInt(1, bookid);
        try (ResultSet rs = prepSeries.executeQuery()) {
            while(rs.next()) {
                String name = rs.getString(1);
                int num = rs.getInt(2);
                String str = String.format("%s №%d", name, num);
                rowList.getSeries().add(str);
            }
        }
    }

    /**
     * Формирует список авторов данной книги
     * @param bookid - идентификатор книги
     * @return - массив строк автров данной книги
     * @throws SQLException
     */
    private void getAuthors(int bookid, IRowBookList rowList) throws SQLException {
        prepAuthor.setInt(1, bookid);
        try (ResultSet rs = prepAuthor.executeQuery()) {
            while(rs.next()) {
                String result = rs.getString(1);
                rowList.getAuthors().add(result);
            }
        }
    }

    /**
     * Формирует колонки таблицы всоответствии с текущей открытой вкладкой, 
     * запуская соответствующую процедуру
     * @param tab - идентификатор открытой вкладки
     */
    public void prepareColumns(TABS tab) {
        clearTable();
        tableView.getColumns().clear();
        ArrayList<TableColumn> columns = new ArrayList<>();
        switch(tab) {
            case TAB_AUTHOR: columns = makeAuthorColumns(); break;
            case TAB_SERIES: columns = makeSeriesColumns(); break;
            case TAB_GENRE: columns = makeGenreColumns(); break;
        }
        if(!columns.isEmpty()) {
            tableView.getColumns().addAll(columns);
        }
    }

    /**
     * Формирует колонки таблицы для отображения списка книг автора
     * @return - массив TableColumn
     */
    private ArrayList<TableColumn> makeAuthorColumns() {
        ArrayList<TableColumn> columns = new ArrayList<>();
        TableColumn<IRowBookList, Integer> bookidCol = new TableColumn<>("ID");
        bookidCol.setCellValueFactory(new PropertyValueFactory<>("bookid"));
        bookidCol.setVisible(false);
        columns.add(bookidCol);
        TableColumn<IRowBookList, String> titleCol = new TableColumn<>("Заголовок");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        columns.add(titleCol);
        TableColumn<IRowBookList, ObservableList<String>> seriesCol = new TableColumn<>("Серии");
        seriesCol.setCellValueFactory(new PropertyValueFactory<>("series"));
        seriesCol.setCellFactory(col -> {
            TableCell<IRowBookList, ObservableList<String>> cell = new TableCell<IRowBookList, ObservableList<String>>(){
                @Override
                public void updateItem(ObservableList<String> item, boolean empty) {
                    super.updateItem(item, empty);
                    this.setText(Constants.EMPTY_STRING);
                    this.setGraphic(null);
                    if(!empty) {
                        if(this.getTableRow() != null) {
                            int index = this.getTableRow().getIndex();
                            RowBookAuthor row = (RowBookAuthor) this.getTableView().getItems().get(index);
                            ObservableList<String> array = row.getSeries();
                            if(!array.isEmpty()) {
                                if(array.size() == 1) {
                                    this.setText(array.get(0));
                                } else {
                                    ComboBox cb = new ComboBox(array);
                                    cb.getSelectionModel().selectFirst();
                                    this.setGraphic(cb);
                                }
                            }
                        }
                    }
                }
            };
            return cell;
        });
        seriesCol.setMinWidth(250);
        seriesCol.setMaxWidth(300);
        //seriesCol.setResizable(false);
        columns.add(seriesCol);
        TableColumn<IRowBookList, ObservableList<String>> genresCol = new TableColumn<>("Жанры");
        genresCol.setCellValueFactory(new PropertyValueFactory<>("genres"));
        genresCol.setCellFactory(cal -> {
            TableCell<IRowBookList, ObservableList<String>> cell = new TableCell<IRowBookList, ObservableList<String>>(){
                @Override
                public void updateItem(ObservableList<String> item, boolean empty) {
                    super.updateItem(item, empty);
                    this.setText(Constants.EMPTY_STRING);
                    this.setGraphic(null);
                    if(!empty) {
                        if(this.getTableRow() != null) {
                            int index = this.getTableRow().getIndex();
                            RowBookAuthor row = (RowBookAuthor) this.getTableView().getItems().get(index);
                            ObservableList<String> array = row.getGenres();
                            if(!array.isEmpty()) {
                                if(array.size() == 1) {
                                    this.setText(array.get(0));
                                } else {
                                    ComboBox cb = new ComboBox(array);
                                    cb.getSelectionModel().selectFirst();
                                    this.setGraphic(cb);
                                }
                            }
                        }
                    }
                }
            };
            return cell;
        });
        genresCol.setMinWidth(250);
        genresCol.setMaxWidth(300);
        //genresCol.setResizable(false);
        columns.add(genresCol);
        return columns;
    }

    /**
     * Формирует колонки таблицы для отображения списка книг жанра
     * @return - массив TableColumn
     */
    private ArrayList<TableColumn> makeGenreColumns() {
        RowBookGenre rowList = new RowBookGenre();
        ArrayList<TableColumn> columns = new ArrayList<>();
        TableColumn<IRowBookList, Integer> bookidCol = new TableColumn<>("ID");
        bookidCol.setCellValueFactory(new PropertyValueFactory<>("bookid"));
        bookidCol.setVisible(false);
        columns.add(bookidCol);
        TableColumn<IRowBookList, ObservableList<String>> authorCol = new TableColumn<>("Автор");
        authorCol.setCellValueFactory(new PropertyValueFactory<>("authors"));
        authorCol.setCellFactory(cal -> {
            TableCell<IRowBookList, ObservableList<String>> cell = new TableCell<IRowBookList, ObservableList<String>>(){
                @Override
                public void updateItem(ObservableList<String> item, boolean empty) {
                    super.updateItem(item, empty);
                    this.setText(Constants.EMPTY_STRING);
                    this.setGraphic(null);
                    if(!empty) {
                        if(this.getTableRow() != null) {
                            int index = this.getTableRow().getIndex();
                            RowBookGenre row = (RowBookGenre) this.getTableView().getItems().get(index);
                            ObservableList<String> array = row.getAuthors();
                            if(!array.isEmpty()) {
                                if(array.size() == 1) {
                                    this.setText(array.get(0));
                                } else {
                                    ComboBox cb = new ComboBox(array);
                                    cb.getSelectionModel().selectFirst();
                                    this.setGraphic(cb);
                                }
                            }
                        }
                    }
                }
            };
            return cell;
        });
        authorCol.setMinWidth(250);
        authorCol.setMaxWidth(300);
        columns.add(authorCol);
        TableColumn<IRowBookList, String> titleCol = new TableColumn<>("Заголовок");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        columns.add(titleCol);
        TableColumn<IRowBookList, ObservableList<String>> seriesCol = new TableColumn<>("Серии");
        seriesCol.setCellValueFactory(new PropertyValueFactory<>("series"));
        seriesCol.setCellFactory(cal -> {
            TableCell<IRowBookList, ObservableList<String>> cell = new TableCell<IRowBookList, ObservableList<String>>(){
                @Override
                public void updateItem(ObservableList<String> item, boolean empty) {
                    super.updateItem(item, empty);
                    this.setText(Constants.EMPTY_STRING);
                    this.setGraphic(null);
                    if(!empty) {
                        if(this.getTableRow() != null) {
                            int index = this.getTableRow().getIndex();
                            RowBookGenre row = (RowBookGenre) this.getTableView().getItems().get(index);
                            ObservableList<String> array = row.getSeries();
                            if(!array.isEmpty()) {
                                if(array.size() == 1) {
                                    this.setText(array.get(0));
                                } else {
                                    ComboBox cb = new ComboBox(array);
                                    cb.getSelectionModel().selectFirst();
                                    this.setGraphic(cb);
                                }
                            }
                        }
                    }
                }
            };
            return cell;
        });
        seriesCol.setMinWidth(250);
        seriesCol.setMaxWidth(300);
        columns.add(seriesCol);
        return columns;
    }

    /**
     * Формирует колонки таблицы для отображения списка книг серии
     * @return - массив TableColumn
     */
    private ArrayList<TableColumn> makeSeriesColumns() {
        ArrayList<TableColumn> columns = new ArrayList<>();
        TableColumn<IRowBookList, Integer> bookidCol = new TableColumn<>("ID");
        bookidCol.setCellValueFactory(new PropertyValueFactory<>("bookid"));
        bookidCol.setVisible(false);
        columns.add(bookidCol);
        TableColumn<IRowBookList, ObservableList<String>> authorCol = new TableColumn<>("Автор");
        authorCol.setCellValueFactory(new PropertyValueFactory<>("authors"));
        authorCol.setCellFactory(cal -> {
            TableCell<IRowBookList, ObservableList<String>> cell = new TableCell<IRowBookList, ObservableList<String>>(){
                @Override
                public void updateItem(ObservableList<String> item, boolean empty) {
                    super.updateItem(item, empty);
                    this.setText(Constants.EMPTY_STRING);
                    this.setGraphic(null);
                    if(!empty) {
                        if(this.getTableRow() != null) {
                            int index = this.getTableRow().getIndex();
                            RowBookSeries row = (RowBookSeries) this.getTableView().getItems().get(index);
                            ObservableList<String> array = row.getAuthors();
                            if(!array.isEmpty()) {
                                if(array.size() == 1) {
                                    this.setText(array.get(0));
                                } else {
                                    ComboBox cb = new ComboBox(array);
                                    cb.getSelectionModel().selectFirst();
                                    this.setGraphic(cb);
                                }
                            }
                        }
                    }
                }
            };
            return cell;
        });
        authorCol.setMinWidth(250);
        authorCol.setMaxWidth(300);
        columns.add(authorCol);
        TableColumn<IRowBookList, String> titleCol = new TableColumn<>("Заголовок");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        columns.add(titleCol);
        TableColumn<IRowBookList, Integer> numCol = new TableColumn<>("№");
        numCol.setCellValueFactory(new PropertyValueFactory<>("num"));
        numCol.setMinWidth(50);
        numCol.setMaxWidth(60);
        //numCol.setResizable(false);
        columns.add(numCol);
        TableColumn<IRowBookList, ObservableList<String>> genresCol = new TableColumn<>("Жанры");
        genresCol.setCellValueFactory(new PropertyValueFactory<>("genres"));
        genresCol.setCellFactory(cal -> {
            TableCell<IRowBookList, ObservableList<String>> cell = new TableCell<IRowBookList, ObservableList<String>>(){
                @Override
                public void updateItem(ObservableList<String> item, boolean empty) {
                    super.updateItem(item, empty);
                    this.setText(Constants.EMPTY_STRING);
                    this.setGraphic(null);
                    if(!empty) {
                        if(this.getTableRow() != null) {
                            int index = this.getTableRow().getIndex();
                            RowBookSeries row = (RowBookSeries) this.getTableView().getItems().get(index);
                            ObservableList<String> array = row.getGenres();
                            if(!array.isEmpty()) {
                                if(array.size() == 1) {
                                    this.setText(array.get(0));
                                } else {
                                    ComboBox cb = new ComboBox(array);
                                    cb.getSelectionModel().selectFirst();
                                    this.setGraphic(cb);
                                }
                            }
                        }
                    }
                }
            };
            return cell;
        });
        genresCol.setMinWidth(250);
        genresCol.setMaxWidth(300);
        columns.add(genresCol);
        return columns;
    }
}
