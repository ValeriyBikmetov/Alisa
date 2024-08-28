package valeriy.bikmetov.alisa.model;

import valeriy.bikmetov.alisa.utilites.Constants;

import java.util.Comparator;

/**
 *
 * @author Валерий Бикметов
 */
public final class Folder implements Comparator<Folder> {
    private int id;
    private String folder;

    public Folder() {
        this.id = 0;
        this.folder = Constants.EMPTY_STRING;
    }

    public Folder(int id, String path) {
        this.id = id;
        this.folder = path;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    @Override
    public String toString() {
        return this.folder;
    }

    @Override
    public int compare(Folder o1, Folder o2) {
        return o1.folder.toLowerCase().compareTo(o2.folder.toLowerCase());
    }
}
