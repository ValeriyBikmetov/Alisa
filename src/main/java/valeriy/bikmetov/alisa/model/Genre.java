package valeriy.bikmetov.alisa.model;

import valeriy.bikmetov.alisa.utilites.Constants;

import java.util.Comparator;

/**
 *
 * @author Валерий Бикметов
 */
public final class Genre implements Comparator<Genre> {
    private int id;
    private String genre;
    private String description;
    private String group;

    public Genre(int id, String genre, String desc, String group) {
        this.genre = genre;
        this.group = group;
        this.id = id;
        this.description = desc;
    }

    public Genre() {
        this.genre = Constants.EMPTY_STRING;
        this.group = Constants.EMPTY_STRING;
        this.description = Constants.EMPTY_STRING;
        this.id = 0;
    }

    public Genre(Genre other) {
        this.id = other.getId();
        this.genre = other.getGenre();
        this.description = other.getDescription();
        this.group = other.getGroup();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setDescription(String desc) {
        this.description = desc;
    }

    public String getDescription() {
        return this.description;
    }

    @Override
    public String toString() {
        if (group.isEmpty())
            return genre;
        else return group + ": " + description;
    }

    @Override
    public int compare(Genre o1, Genre o2) {
        String s1 = (o1.genre + o1).toLowerCase();
        String s2 = (o2.genre + o2).toLowerCase();
        return s1.compareTo(s2);
    }
}

