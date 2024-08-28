package valeriy.bikmetov.alisa.utilites;

import valeriy.bikmetov.alisa.Alisa;
import valeriy.bikmetov.alisa.model.Book;
import valeriy.bikmetov.alisa.model.Genre;
import valeriy.bikmetov.alisa.model.Library;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import valeriy.bikmetov.alisa.model.Series;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.logging.Level;

/**
 *
 * @author Валерий Бикметов
 */
public class ManagerBookInfo {
    //private PreparedStatement pstm;
    private String root; // Путь к папке с книгами
    private Library library;
    private int book_id;
    private Image image;
    private Book currentBook;

    public ManagerBookInfo() {}

    public void setCurrentLibrary(Library library) throws SQLException {
        this.library = library;
        root = library.getPathToBooks().toString();
    }

    public void makeInformation(int bookid, ImageView imageView, TextArea textArea)
            throws SQLException, IOException, XMLStreamException {
        if(bookid == 0) {
            return;
        } else currentBook = H2Operations.makeBook(library.getConnection(), bookid);
        textArea.setText(Constants.EMPTY_STRING);
        this.book_id = bookid;
        imageView.setImage(null);
        String typeBook = currentBook.getTypeBook();
        var source = Path.of(root, currentBook.getFolderName(), currentBook.getFile());
        Path path = FileUtil.makeBookPath(source);
        String info = makeCommonInfo();
        textArea.setText(info);
        processingOfBook(typeBook, path, imageView, textArea);
    }

    public void makeInformation(Path sourceFile, ImageView imageView, TextArea textArea)
            throws SQLException, IOException, XMLStreamException {
        var path = FileUtil.makeBookPath(sourceFile);
        var typeBook = FileUtil.getTypeBook(path.getFileName().toString());
        switch (typeBook) {
            case Constants.EPUB_BOOK:
                var parserEpub = new ParserEpub();
                parserEpub.parseEpubBook(path);
                currentBook = parserEpub.getBook();
                break;
            case Constants.FB2_BOOK:
                var parserFB2 = new ParserFB2();
                parserFB2.parse(path);
                currentBook = parserFB2.getBook();
                break;
            default:
                currentBook = new Book();
                currentBook.setTitle(path.toString());
                break;
        }
        currentBook.setPath(path);
        processingOfBook(typeBook, path, imageView, textArea);
    }

    private void processingOfBook(String typeBook, Path path, ImageView imageView,
                                  TextArea textArea) throws XMLStreamException, IOException {
        switch(typeBook) {
            case Constants.DJVU_BOOK:
                BookUtil.getDjvuPage(path, 0, imageView);
                break;
            case Constants.EPUB_BOOK:
                processingEpub(path, imageView, textArea);
                break;
            case Constants.FB2_BOOK:
                makeFb2Info(path, imageView, textArea);
                break;
            case Constants.PDF_BOOK:
                BookUtil.getPdfPage(path, 0, imageView);
                break;
        }
        image = imageView.getImage();
    }

    private String makeCommonInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(currentBook.getTitle());
        sb.append("\nКорневая папка: ");
        sb.append(root);
        sb.append("\nПапка: ");
        sb.append(currentBook.getFolderName());
        sb.append("\nФайл: ");
        sb.append(currentBook.getFile());
        String size = String.format("\nРазмер: %d кБ.", currentBook.getFsize()/1024);
        sb.append(size);
        String encode = currentBook.getEncoding();
        if(encode != null) {
            sb.append(" Кодировка: ");
            sb.append(encode);
        }
        if (!currentBook.getSeries().isEmpty()) {
            sb.append("\nСерии:");
            for (Series series : currentBook.getSeries()) {
                sb.append("\n").append(series.toString());
            }
        }
        if (!currentBook.getGenre().isEmpty()) {
            sb.append("\nЖанры:");
            for (Genre genre : currentBook.getGenre()) {
                sb.append("\n").append(genre.toString());
            }
        }
        return sb.toString();
    }

    /**
     * Извлекаем из файла изображение обложки и аннотацию.
     * Помещаем изображение в ImageView, а аннотацию добавляем в TextArea.
     * @param path - путь к файлу
     * @param imageView - то куда выводим изображение
     * @param textArea - то куда выводим текстовую инфорацию
     */
    private void processingEpub(Path path, ImageView imageView, TextArea textArea) throws IOException, XMLStreamException {
        String cover = currentBook.getCoverpage();
        if(cover == null || cover.isEmpty()){
            correctCoverAndEncode(path, Constants.EPUB_BOOK);
            cover = currentBook.getCoverpage();
        }
        if(cover != null) {
            CoverEPUB coverEPUB = new CoverEPUB();
            coverEPUB.extractCoverAndAnnotation(path, cover);
            currentBook.setAnnotation(coverEPUB.getAnnotation());
            Image img = coverEPUB.getImage();
            addImageAndAnnotation(img, coverEPUB.getAnnotation(), imageView, textArea);
        }
    }

    /**
     * Извлекаем из файла изображение обложки и аннотацию.
     * Помещаем изображение в ImageView, а аннотацию добавляем в TextArea.
     * @param path - путь к файлу
     * @param imageView - то куда выводим изображение
     * @param textArea - то куда выводим текстовую инфорацию
     */
    private void makeFb2Info(Path  path, ImageView imageView, TextArea textArea) throws IOException, XMLStreamException {
        String cover = currentBook.getCoverpage();
        String encoding = currentBook.getEncoding();
        if(Objects.equals(encoding, "null") | Objects.equals(cover, "null") | cover == null | encoding == null) {
            correctCoverAndEncode(path, Constants.FB2_BOOK);
            cover = currentBook.getCoverpage();
            encoding = currentBook.getEncoding();
        }
        CoverFB2 coverFB2 = new CoverFB2();
        coverFB2.extractCoverAndAnnotation(path, encoding, cover);
        Image img = coverFB2.getImage();
        currentBook.setAnnotation(coverFB2.getAnnotation());
        addImageAndAnnotation(img, currentBook.getAnnotation(), imageView, textArea);
    }

    private void addImageAndAnnotation(javafx.scene.image.Image img, String annotation,
                                       ImageView imageView, javafx.scene.control.TextArea textArea) {
        if(img != null) {
            imageView.setImage(img);
            imageView.setFitWidth(Constants.WIDTH_IMAGE);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageView.setCache(true);
        }
        if(textArea != null && annotation != null && !annotation.isEmpty()) {
            String sb = textArea.getText() + '\n' + annotation;
            textArea.setText(sb);
        }
    }

    private void correctCoverAndEncode(Path path, String typeBook) throws IOException, XMLStreamException {
        Book book = null;
        switch (typeBook) {
            case Constants.EPUB_BOOK: ParserEpub parserEpub = new ParserEpub();
                parserEpub.parseEpubBook(path);
                book = parserEpub.getBook();
                break;
            case Constants.FB2_BOOK: ParserFB2 parserFB2 = new ParserFB2();
                parserFB2.parse(path);
                book = parserFB2.getBook();
        }
        if(book == null)
            return;
        String cover = book.getCoverpage();
        String encoding = book.getEncoding();
        currentBook.setCoverpage(cover);
        currentBook.setEncoding(encoding);
        Thread update = new Thread(() -> {
            Connection conn = library.getConnection();
            try(Statement stm = conn.createStatement()) {
                String query = H2Operations.getQuery(conn, "UPDATE_PARAMS");
                StringBuilder sb = new StringBuilder();
                sb.append("'");
                sb.append(cover);
                sb.append("'");
                String cvr = sb.toString();
                sb.delete(0, sb.length());
                sb.append("'");
                sb.append(encoding);
                sb.append("'");
                String encd = sb.toString();
                query = String.format(query, cvr, encd, currentBook.getId());
                stm.executeUpdate(query);
            } catch (SQLException ex) {
                Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            }
        });
        update.start();
    }

    public int getBook_id() {
        return book_id;
    }

    public Image getImage() {return image;}

    public Book getCurrentBook() {return currentBook;}
}
