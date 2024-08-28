package valeriy.bikmetov.alisa.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 *
 * @author Валерий Бикметов
 */
public class RowBookAuthor implements IRowBookList {
    private int bookid;
    private String title;
    private ObservableList<String> series = FXCollections.observableArrayList();
    private ObservableList<String> genres = FXCollections.observableArrayList();

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
     * @return the genres
     */
    @Override
    public ObservableList<String> getGenres() {
        return genres;
    }

    /**
     * @param genres the genres to set
     */
    public void setGenres(ObservableList<String> genres) {
        this.genres = genres;
    }

    @Override
    public ObservableList<String> getAuthors() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
