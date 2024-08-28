package valeriy.bikmetov.alisa.utilites;

import valeriy.bikmetov.alisa.Alisa;
import valeriy.bikmetov.alisa.model.Author;
import valeriy.bikmetov.alisa.model.Book;
import valeriy.bikmetov.alisa.model.Library;
import com.lizardtech.djvu.DjVuInfo;
import com.lizardtech.djvu.DjVuPage;
import com.lizardtech.djvu.Document;
import com.lizardtech.djvubean.DjVuFilter;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;

import static valeriy.bikmetov.alisa.utilites.FileUtil.deleteFileAndFolder;
import static valeriy.bikmetov.alisa.utilites.Utilities.makeIt;

public final class BookUtil {
    /**
     * Получаем изображение первой страницы документа
     * @param path - Путь к файлу книги
     * @param page_num - номер отображаемой страницы
     * @param imageView - то где мы будем демонстрировать изображение
     */
    public static void getDjvuPage(Path path, int page_num, ImageView imageView) throws IOException {
        URI uri = path.toUri();
        URL url = uri.toURL();
        Document document = new Document(url);
        DjVuPage page = document.getPage(page_num, DjVuPage.MAX_PRIORITY,false);
        DjVuInfo info = page.getInfo();
        int w = info.width;
        int h = info.height;
        DjVuFilter filter = new DjVuFilter(new Rectangle(5,5, 5 + w, 5 + h), new Dimension(w, h), page, false);
        Canvas canvas = new Canvas();
        Image image = filter.getImage(canvas);
        BufferedImage bi = new BufferedImage(Constants.WIDTH_IMAGE, Constants.HEIGHT_IMAGE, BufferedImage.TYPE_INT_RGB);
        Graphics g = bi.getGraphics();
        g.drawImage(image, 0, 0, Constants.WIDTH_IMAGE, Constants.HEIGHT_IMAGE, null);
        g.dispose();
        WritableImage wim = null;
        wim = SwingFXUtils.toFXImage(bi, wim);
        imageView.setImage(wim);
    }

    /**
     * Получаем изображение первой страницы PDF документа
     * @param path путь к файлу
     * @param pagenum номер страницы
     * @param imageView кудавыводим изображение
     */
    public static void getPdfPage(Path path, int pagenum, ImageView imageView) throws IOException {
        BufferedImage image = null;
        try(PDDocument document = Loader.loadPDF(path.toFile())) {
            if(!document.isEncrypted()) {
                PDPage pdPage = document.getPage(pagenum);
                PDRectangle pdRectangle = pdPage.getBBox();
//                float width = pdRectangle.getWidth();
                float heghit = pdRectangle.getHeight();
                float scale = Constants.HEIGHT_IMAGE / heghit;
                PDFRenderer render = new PDFRenderer(document);
                image = render.renderImage(0, scale);
            }
        }
        if(image != null) {
            Graphics g = image.getGraphics();
            g.drawImage(null, 0, 0, Constants.WIDTH_IMAGE, Constants.HEIGHT_IMAGE, null);
            g.dispose();
            WritableImage wim = null;
            wim = SwingFXUtils.toFXImage(image, wim);
            imageView.setImage(wim);
        }
    }

    /**
     * Проверить книги одного автора на наличие других соавторов
     * @param books массив книг автора
     * @return true - если есть соавторы
     */
    public static boolean isCo_Author(ArrayList<Book> books) {
        boolean result = false;
        for(Book book : books) {
            ArrayList<Author> authors;
            authors = book.getAuthors();
            result = authors.size() > 1;
            if (result)
                break;
        }
        return result;
    }

    /**
     * Delete The Author and his Books
     * @param lib current Library
     * @param authorid Author ID
     */
    public static void deleteAuthorWithBooks(Library lib, int authorid) {
        boolean action = makeIt("Удаление автора", "Вместе с автором будут удалены все его книги.\nВы хотите этого?!");
        if (action) {
            try {
                // Get a list of author's books.
                ArrayList<Book> listBooks = H2Operations.getAuthorBooks(lib.getConnection(), authorid);
                // View list for other authors.
                if (!isCo_Author(listBooks)) {
                    // Delete books from directory.
                    String root = lib.getPathToBooks().toString();
                    for(Book book : listBooks) {
                        deleteFileAndFolder(lib, book);
                    }
                    // Delete books from DB
                    H2Operations.delAuthorWithBooks(lib.getConnection(), listBooks);
                }
            } catch(SQLException ex){
                Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
                Utilities.showMessage(Constants.TITLE_EXCEPTION,"Ошибка удаления автора", ex.getMessage());
            }
        }
    }

    /**
     * Delete Series and Books
     * @param lib current Library
     * @param seriesid Series ID
     */
    public static void deleteSeriesWithBooks(Library lib, int seriesid) {
        boolean action = makeIt("Удаление серии", "Будут удалены все книги в этой серии.\nВы хотите этого?!");
        if(action) {
            try {
                ArrayList<Book> listBooks = H2Operations.getSeriesBooks(lib.getConnection(), seriesid);
                String root = lib.getPathToBooks().toString();
                for (Book book : listBooks) {
                    deleteFileAndFolder(lib, book);
                }
                H2Operations.delSeriesWithBooks(lib.getConnection(), listBooks);
            } catch(SQLException ex){
                Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
                Utilities.showMessage(Constants.TITLE_EXCEPTION,"Ошибка удаления серии", ex.getMessage());
            }
        }
    }

    /**
     * Delete Books of a Genre
     * @param lib current Library
     * @param genreid Genre ID
     */
    public static void deleteBooksOfGenre(Library lib, int genreid) {
        boolean action = makeIt("Удаление книг этого жанра", "Будут удалены все книги в этого жанра.\nВы хотите этого?!");
        if(action) {
            try {
                ArrayList<Book> listBooks = H2Operations.getBooksOfGenre(lib.getConnection(), genreid);
                String root = lib.getPathToBooks().toString();
                for (Book book : listBooks) {
                    deleteFileAndFolder(lib, book);
                }
                H2Operations.delBooksOfGenre(lib.getConnection(),listBooks);
            } catch(SQLException ex){
                Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
                Utilities.showMessage(Constants.TITLE_EXCEPTION,"Ошибка удаления жанра", ex.getMessage());
            }
        }
    }

    public static void readOfBook(Library lib, int book_id) {
        try {
            var book = H2Operations.makeBook(lib.getConnection(), book_id);
            var fileName = book.getFile();
            var typeBook = FileUtil.getTypeBook(fileName);
            String command = switch (typeBook) {
                case "djvu" -> Constants.READ_DJVU;
                case "pdf" -> Constants.READ_PDF;
                default -> Constants.READ_FB2;
            };
            var root = lib.getPathToBooks().toString();
            var folder = book.getFolder().getFolder();
            var path = Path.of(root, folder, fileName);
            var process = new ProcessBuilder(command, path.toString());
            process.start();
        } catch (SQLException | IOException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION,"Error.", ex.getMessage());
        }
    }

    /**
     * Копировать выбранную книгу в заданное место
     */
    public static void copyBook(Library lib, int book_id, Path target) {
        try {
            var book = H2Operations.makeBook(lib.getConnection(), book_id);
            var path = FileUtil.copyFileOfBook(lib, book, target);
            if (path != null) {
                Utilities.showMessage("Информация", Constants.EMPTY_STRING, "Файл скопирован");
            }
        } catch (SQLException | IOException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION,"Error.", ex.getMessage());
        }
    }
}
