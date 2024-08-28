package valeriy.bikmetov.alisa.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


/**
 *
 * @author Валерий Бикметов
 */
public class RowBookGenre implements IRowBookList {
    private int bookid;
    private ObservableList<String> authors = FXCollections.observableArrayList();
    private String title;
    private ObservableList<String> series = FXCollections.observableArrayList();

    /**
     * @return the bookid
     */
    @Override
    public int getBookid() {
        return bookid;
    }

    /**
     * @param bookid the bookid to set
     */
    public void setBookid(int bookid) {
        this.bookid = bookid;
    }

    /**
     * @return the title
     */
    @Override
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the series
     */
    @Override
    public ObservableList<String> getSeries() {
        return series;
    }

    /**
     * @param series the series to set
     */
    public void setSeries(ObservableList<String> series) {
        this.series = series;
    }

    /**
     * @return the authors
     */
    @Override
    public ObservableList<String> getAuthors() {
        return authors;
    }

    /**
     * @param authors the authors to set
     */
    public void setAuthors(ObservableList<String> authors) {
        this.authors = authors;
    }

    @Override
    public ObservableList<String> getGenres() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
