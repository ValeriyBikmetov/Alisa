package valeriy.bikmetov.alisa.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


/**
 *
 * @author Валерий Бикметов
 */
public class RowBookSeries implements IRowBookList {
    private int bookid;
    private ObservableList<String> authors = FXCollections.observableArrayList();
    private String title;
    private int num;
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
     * @return the num
     */
    public int getNum() {
        return num;
    }

    /**
     * @param num the num to set
     */
    public void setNum(int num) {
        this.num = num;
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
    public ObservableList<String> getSeries() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}