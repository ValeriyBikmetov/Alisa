package valeriy.bikmetov.alisa.utilites;

import valeriy.bikmetov.alisa.Alisa;
import valeriy.bikmetov.alisa.model.Author;
import valeriy.bikmetov.alisa.model.Book;
import valeriy.bikmetov.alisa.model.Genre;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.logging.Level;

/**
 *
 * @author Валерий Бикметов
 */
public class PDFBook {
    private Book book;

    private void getMetadata(Path path) throws IOException {
        book = new Book();
        book.setPath(path);
        book.setFsize((int)Files.size(path));
        try(PDDocument document = Loader.loadPDF(path.toFile())) {
            if(!document.isEncrypted()) {
                PDDocumentInformation information = document.getDocumentInformation();
                if (information != null){
                    String metadata = information.getTitle();
                    if(metadata != null && !metadata.isEmpty()) {
                        book.setTitle(metadata);
                    }
                    metadata = information.getSubject();
                    if(metadata != null && !metadata.isEmpty()) {
                        Genre genre = new Genre(0, metadata, Constants.EMPTY_STRING, Constants.EMPTY_STRING);
                        book.addGenre(genre);
                    }
                    metadata = information.getAuthor();
                    getAuthors(metadata, book);
                }
            }
        }
    }

    private void getAuthors(String aString, Book book) {
        String[] aStr = aString.split(",");
        for(String s : aStr) {
            int index = s.lastIndexOf(' ');
            Author author;
            if(index > 0) {
                String fName = s.substring(0, index).trim();
                String lName = s.substring(index + 1).trim();
                author = new Author(lName + " " + fName, 0);
            } else {
                author = new Author(s, 0);
            }
            book.addAuthor(author);
        }
    }

    public Book getBook() {
        return book;
    }

    public void parse(Path path) {
        try {
            getMetadata(path);
        } catch (IOException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION, this.getClass().getName(), ex.getMessage());
        }
    }
}
