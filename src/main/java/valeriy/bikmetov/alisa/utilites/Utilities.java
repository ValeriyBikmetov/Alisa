package valeriy.bikmetov.alisa.utilites;

import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import valeriy.bikmetov.alisa.model.Author;
import valeriy.bikmetov.alisa.model.Genre;
import valeriy.bikmetov.alisa.model.Series;
import valeriy.bikmetov.alisa.view.PropertiesDlg;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Properties;

/**
 *
 * @author Валерий Бикметов
 */
public final class Utilities {
    private Utilities() {}

    /**
     * Показываем сообщение
     * @param title - Заголовок окна
     * @param header - Заголовок (информация) на панели вывода
     * @param content - Содержимое сообщения
     */
    public static void showMessage(String title, String header, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        alert.showAndWait();
    }

    /**
     * Считываем данные о путях к папке с базами данных и временным файлам
     */
    public static void readProperty() throws IOException {
        Path path = Path.of(".", Constants.APP_PROPERTY);
        if (Files.exists(path)) {
            Properties properties = new Properties();
            try (FileInputStream fin = new FileInputStream(path.toFile())) {
                properties.load(fin);
                Constants.TEMPLATE_DB = properties.getProperty("TEMPLATE_DB");
                Constants.FOLDER_DB = properties.getProperty("FOLDER_DB");
                Constants.TEMP_DIR = properties.getProperty("TEMP_DIR");
                Constants.PASSWORD_DB = properties.getProperty("PASSWORD_DB", "");
                Constants.SUFFIX_DB = properties.getProperty("SUFFIX_DB");
                Constants.ADMIN_DB = properties.getProperty("ADMIN_DB");
                Constants.READ_DJVU = properties.getProperty("READ_DJVU");
                Constants.READ_FB2 = properties.getProperty("READ_FB2");
                Constants.READ_PDF = properties.getProperty("READ_PDF");
                if (!Files.exists(Path.of(Constants.FOLDER_DB)))
                    throw new IOException("Нет доступа к базе данны");
                if (!Files.exists(Path.of(Constants.TEMP_DIR))) {
                    File f = new File(Constants.TEMP_DIR);
                    if (!Files.exists(f.toPath()))
                        throw new IOException("Ошибка создания временного файла");
                }
            } catch(IOException e){
                showMessage(Constants.TITLE_EXCEPTION, "Ошибка файловой системы", e.getMessage());
                correctionProperties();
            }
        } else correctionProperties();
    }

    /**
     * Создаем экземпляр Properties и передаем его в PropertyDlg.
     * Записываем данные о путях к папке с базами данных и временным файлам
     * в Constants и файл app.properties
     */
    private  static void correctionProperties() throws IOException {
//        Properties properties = getProperties();
        PropertiesDlg dialog = new PropertiesDlg();
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent()) {
            if (result.orElseThrow().getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE)
                return;
            else if(result.orElseThrow().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                Properties properties = dialog.getProperties();
                Path path = Path.of(".", Constants.APP_PROPERTY);
                Constants.TEMPLATE_DB = properties.getProperty("TEMPLATE_DB");
                Constants.FOLDER_DB = properties.getProperty("FOLDER_DB");
                Constants.TEMP_DIR = properties.getProperty("TEMP_DIR");
                Constants.ADMIN_DB = properties.getProperty("ADMIN_DB");
                Constants.PASSWORD_DB = properties.getProperty("PASSWORD_DB");
                Constants.SUFFIX_DB = properties.getProperty("SUFFIX_DB");
                Constants.READ_DJVU = properties.getProperty("READ_DJVU");
                Constants.READ_FB2 = properties.getProperty("READ_FB2");
                Constants.READ_PDF = properties.getProperty("READ_PDF");
                try (FileOutputStream fout = new FileOutputStream(path.toString())) {
                    properties.store(fout, "app properties");
                } catch (IOException e) {
                    showMessage(Constants.TITLE_EXCEPTION, "Запись свойств программы", "Ошибка записи в файл");
                }
            }
        }
    }

    /**
     * Запрашиваем разрешение на какое-либо действие
     * @param action что хотим сделать
     * @param object информация о том что хотим сделать
     */
    public static boolean makeIt(String action, String object) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Выполнение действия");
        alert.setHeaderText(action);
        alert.setContentText(object);
        Optional<ButtonType> result = alert.showAndWait();
        return !(result.get() == null || result.get() == ButtonType.CANCEL);
    }

    public static int findItem(Object[] items, String text) {
        int first = 0;
        int last = items.length - 1;
        int mid = (last + first) / 2;
        while(first < last) {
            String name = (String) items[mid];
            if(text.compareTo(name) <= 0) {
                last = mid;
            } else {
                first = mid + 1;
            }
            mid = first + (last - first) / 2;
        }
        return last;
    }

    public static Author getNewAuthor (Connection conn) throws SQLException {
        Author author = new Author();
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Новый автор");
        dialog.setHeaderText("Введите Ф.И.О. нового автора");
        dialog.getEditor().setPrefColumnCount(Constants.LENGTH_TEXT_FIELD);
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            var name = result.get().replace("'", "`");
            author.setName(name);
            int id = H2Operations.newAuthorID(conn, author);
            author.setId(id);
        }
        if(author.getId() > 0)
            return author;
        else
            return null;
    }

    public static Series getNewSeries (Connection conn) throws SQLException {
        Series series = new Series();
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Новая серия");
        dialog.setHeaderText("Введите наименование новой серии");
        dialog.getEditor().setPrefColumnCount(Constants.LENGTH_TEXT_FIELD);
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            var name = result.get().replace("'", "`");
            series.setName(result.get());
            int id = H2Operations.newSeriesID(conn, series);
            series.setId(id);
        }
        if(series.getId() > 0)
            return series;
        else
            return null;
    }

    public static String changeAuthorName(String name) {
        TextInputDialog dialog = new TextInputDialog(name);
        dialog.setTitle("Изменение автора");
        dialog.setHeaderText("Измените Ф.И.О. автора");
        dialog.getEditor().setPrefColumnCount(Constants.LENGTH_TEXT_FIELD);
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }
    public static String changeSeriesName(String name) {
        TextInputDialog dialog = new TextInputDialog(name);
        dialog.setTitle("Изменение серии");
        dialog.setHeaderText("Измените наименование серии");
        dialog.getEditor().setPrefColumnCount(Constants.LENGTH_TEXT_FIELD);
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    /**
     * Получаем список жанров с нулевыми идентификаторами.
     * Соотносим их с встроенными и соответственно корректируем параметры
     */
    public static void correctionGenre(ArrayList<Genre> genres, Connection conn) throws SQLException{

        for (Genre genre : genres) {
            var genre_cod = genre.getGenre();
            var genre_id = H2Operations.getGenreID(conn, genre_cod);
            if (genre_id > 0) {
                Genre tmp = H2Operations.makeGenre(conn, genre_id);
                genre.setId(genre_id);
                genre.setDescription(tmp.getDescription());
                genre.setGroup(tmp.getGroup());
            }
        }
    }

    public static void correctionAuthors(ArrayList<Author> authors, Connection conn) throws SQLException {
        for (Author author : authors) {
            String name = author.getName();
            int author_id = H2Operations.getAuthorID(conn, name);
            if (author_id > 0) {
                Author tmp = H2Operations.makeAuthor(conn, author_id);
                String s = tmp.getName().replaceAll("[/:*?\"<>«»|']", "_");
                author.setName(s);
                author.setId(tmp.getId());
            }
        }
    }

    public static void correctionSeries(ArrayList<Series> series, Connection conn) throws SQLException {
        for (Series series1 : series) {
            String name = series1.getName();
            int series_id = H2Operations.getSeriesID(conn, name);
            if (series_id > 0) {
                Series tmp = H2Operations.makeSeries(conn, series_id, 0);
                String s = tmp.getName().replaceAll("[/:*?\"<>«»|']", " ");
                series1.setName(tmp.getName());
                series1.setId(tmp.getId());
            }
        }
    }
}
