package valeriy.bikmetov.alisa.model;

import javafx.collections.ObservableList;

public interface IRowBookList {
    int getBookid();
    String getTitle();

    ObservableList<String> getGenres();
    ObservableList<String> getSeries();
    ObservableList<String> getAuthors();}
