package valeriy.bikmetov.alisa.model;

import valeriy.bikmetov.alisa.utilites.Constants;

import java.util.Comparator;
/**
 *
 * @author Валерий Бикметов
 */
public final class Author implements Comparator<Author> {
    private int id;
    private String name;

    public Author () {
        this.name = Constants.EMPTY_STRING;
        this.id = 0;
    }

    public Author(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public Author(Author other) {
        this.name = other.name;
        this.id = other.id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compare(Author o1, Author o2) {
        return o1.name.toLowerCase().compareTo(o2.name.toLowerCase());
    }
}