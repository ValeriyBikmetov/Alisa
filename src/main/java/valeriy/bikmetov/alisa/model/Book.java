package valeriy.bikmetov.alisa.model;

import java.io.Serializable;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import javafx.scene.image.Image;

/**
 *
 * @author Валерий Бикметов
 */
public class  Book implements Comparator<Book>, Serializable {
    private int id;
    private final ArrayList<Author> authors = new ArrayList<>();
    private final ArrayList<Series> series = new ArrayList<>();
    private final ArrayList<Genre> genre = new ArrayList<>();
    private final Folder folder = new Folder();
    private String folderName;
    private String encoding;
    private String title = "";
    private String coverpage;
    private Path path;  // Путь к исходному файлу новой книги
    private String file;
    private int fsize;
    private String annotation;
    private boolean fresh = true;

    public Book() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void addAuthor(Author author) {
        this.authors.add(author);
    }

    public ArrayList<Author> getAuthors() {
        return authors;
    }

    public void addSeries(Series series) {
        this.series.add(series);
    }

    public ArrayList<Series> getSeries() {
        return series;
    }

    public void addGenre(Genre genre){
        this.genre.add(genre);
    }

    public ArrayList<Genre> getGenre() {
        return genre;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public int getFsize() {
        return fsize;
    }

    public void setFsize(int fsize) {
        this.fsize = fsize;
    }

    public void setCoverpage(String coverpage) {
        this.coverpage = coverpage;
    }

    public String getCoverpage() {
        return coverpage;
    }

    public void setEncoding(String encodeing) {
        this.encoding = encodeing;
    }

    public String getEncoding() {
        return encoding;
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder.setId(folder.getId());
        this.folder.setFolder(folder.getFolder());
    }

    public String getFolderName() {return folderName;}

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public boolean isFresh() {
        return fresh;
    }

    public void setFresh(boolean fresh) {
        this.fresh = fresh;
    }

    public boolean isZipped() {
        if (fresh)
            return path.endsWith(".zip");
        else return file.endsWith(".zip");
    }

    public String getTypeBook() {
        String tmp;
        String name;
        if (fresh) {
            name = path.toString();
        } else {
            name = file;
        }
        int length = name.length();
        if(isZipped()) {
            tmp = name.substring(0, length - 4);
        } else {
            tmp = name;
        }
        int pos = tmp.lastIndexOf(".");
        if(pos > 0) {
            return tmp.substring(pos + 1);
        } else {
            return null;
        }
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    @Override
    public String toString() {
        if (authors.isEmpty())
            return title;
        else return authors.getFirst().getName() + " - " + title;
    }

    @Override
    public int compare(Book o1, Book o2) {
        return o1.toString().toLowerCase().compareTo(o2.toString().toLowerCase());
    }
}
