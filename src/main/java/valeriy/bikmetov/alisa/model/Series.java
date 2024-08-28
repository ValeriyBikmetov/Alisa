package valeriy.bikmetov.alisa.model;

import valeriy.bikmetov.alisa.utilites.Constants;

import java.util.Comparator;

/**
 *
 * @author Валерий Бикметов
 */
public final class Series implements Comparator<Series> {
    private String name;
    private int id;
    private int num;

    public Series() {
        this.name = Constants.EMPTY_STRING;
        this.id = 0;
        this.num = 0;
    }

    public Series(String name, int num) {
        this.name = name;
        this.num = num;
    }

    public Series(Series other) {
        this.name = other.getName();
        this.id = other.getId();
        this.num = other.getNum();
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

    public void setNum(int value) {
        this.num = value;
    }

    public int getNum() {
        return this.num;
    }

    @Override
    public String toString() {
        if(num > 0) {
            return String.format("%s №%d", name, num);
        } else
            return this.name;
    }

    @Override
    public int compare(Series o1, Series o2) {
        return o1.toString().toLowerCase().compareTo(o2.toString().toLowerCase());
    }
}

