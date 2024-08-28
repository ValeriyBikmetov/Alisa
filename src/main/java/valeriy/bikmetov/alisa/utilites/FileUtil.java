package valeriy.bikmetov.alisa.utilites;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import valeriy.bikmetov.alisa.Alisa;
import valeriy.bikmetov.alisa.model.Book;
import valeriy.bikmetov.alisa.model.Library;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static valeriy.bikmetov.alisa.utilites.ZipFileUtil.isZipped;

public final class FileUtil {

    private FileUtil() {}
    /**
     * Выбираем файл либо для открытия, либо для сохранения
     * @param stage - родительские подмостки
     * @param title - Заголовок диалога
     * @param folder - текщая папка, может быть null
     * @param open - если true - открываем файл, иначе сохраняем
     * @return - путь к выбранному файлу
     */
    public static Path fileChooser(Stage stage, String title, Path folder, boolean open) {
        File result;
        final FileChooser fileChooser = new FileChooser();
        if(title != null) {
            fileChooser.setTitle(title);
        }
        if(folder != null) {
            fileChooser.setInitialDirectory(folder.toFile());
        }
        if(open) {
            result = fileChooser.showOpenDialog(stage);
        } else {
            result = fileChooser.showSaveDialog(stage);
        }
        if (result != null)
            return result.toPath();
        else return null;
    }

    /**
     * Возвращает путь к выбранному каталогу
     * @param stage - родительские подмостки
     * @param title - заголовок окна диалога
     * @param folder - каталог по умолчанию
     * @return path
     */
    public static Path directoryChooser(Stage stage, String title, Path folder) {
        final DirectoryChooser dirChooser = new DirectoryChooser();
        if(title != null) {
            dirChooser.setTitle(title);
        }
        if(folder != null) {
            dirChooser.setInitialDirectory(folder.toFile());
        }
        File result = dirChooser.showDialog(stage);
        if (result != null)
            return result.toPath();
        else return null;
    }

    /**
     * Получаем из имени файла тип книги(pdf, fb2, epub and so on)
     * @param filename - имя файла
     * @return - строка с именим типа книги
     */
    public static String getTypeBook(String filename) {
        int pos;
        String result = null;
        boolean zipped = isZipped(filename);
        if(zipped) {
            int length = filename.length();
            pos = filename.lastIndexOf('.', length - 5);
            if(pos > 0) {
                result = filename.substring(pos + 1, length - 4);
            }
        } else {
            pos = filename.lastIndexOf('.');
            if(pos > 0) {
                result = filename.substring(pos + 1);
            }
        }
        if(result != null) {
            result = result.toLowerCase();
        }
        return result;
    }

    /**
     * Удаляем все файлы из каталога
     * IOException
     */
    public static void deleteFromDir(String dir) throws IOException {
        Files.walkFileTree(Path.of(dir), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Формируем путь к файлу книги
     * @return возвращаем путь к файлу книги
     * IOException
     */
    public static Path makeBookPath(Path source) throws IOException {
        var fileName = source.getFileName().toString();
        boolean fileZipped = isZipped(fileName);
        if (fileZipped) {
            return ZipFileUtil.copyZipFileToDir(source, Constants.TEMP_DIR);
        } else {
            return source;
        }
    }

    /**
     * Удаляем файл в папке. Если папка пустая удаляем и ее
     * @param lib текущая библиотека
     * @param book - текущая удаляемая книга
     */
    public static void deleteFileAndFolder(Library lib, Book book) {
        try {
            String root = lib.getPathToBooks().toString();
            Path path = Path.of(root, book.getFolder().getFolder(), book.getFile());
            Files.deleteIfExists(path);
            path = path.getParent();
            File file = path.toFile();
            String[] files = file.list();
            if(files == null || files.length == 0) {
                if(file.delete())
                    H2Operations.deleteFolder(lib.getConnection(), book.getFolder().getId());
            }
        }catch(IOException | SQLException ex){
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION,"Ошибка удаления файла или каталога", ex.getMessage());
        }
    }

    /**
     * Переместить файл, возможно с переименованием из одного каталога в другой.
     * Если нового каталога не существует, то создать его.
     * Если старй станет пустым удалить его физически и из базы. Использовать при этом предыдущий метод.
     */
    public static Path moveFile(Library lib, Book oldBook, Book newBook) {
        String root = lib.getPathToBooks().toString();
        Path pathTo = Path.of(root, newBook.getFolder().getFolder(), newBook.getFile());
        Path pathSource = Path.of(root, oldBook.getFolder().getFolder(), oldBook.getFile());
        Path result = null;
        boolean isZiped = ZipFileUtil.isZipped(oldBook.getFile());
        try {
            Path folder = Path.of(root, newBook.getFolder().getFolder());
            if (!Files.exists(folder))
                folder = Files.createDirectories(folder);
            if (isZiped) {
                var source = Path.of(root, oldBook.getFolder().getFolder(), oldBook.getFile());
                Path pathFrom = ZipFileUtil.copyZipFileToDir(source, Constants.TEMP_DIR);
                String ext = getExtension(newBook.getFile());
                String name = newBook.getFile().replace(ext, "");
                result = ZipFileUtil.writeZipFile(pathFrom, folder.toString(), name);
            } else {
                result = Files.move(pathSource, pathTo, REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION,"Ошибка перемещения файла", ex.getMessage());
        }
        if (result != null) {
            deleteFileAndFolder(lib, oldBook);
        }
        return result;
    }

    public static String getExtension(String file) {
        int index = file.lastIndexOf('.');
        return file.substring(index);
    }

    public static Path appendNewBook(Library lib, Book book) {
        String root = lib.getPathToBooks().toString(); // Путь к каталогу библиотеки
        var pathTo = Path.of(root, book.getFolder().getFolder(), book.getFile());  // Путь к конечному файлу
        var pathSource = book.getPath();  // Исходный каталог расположения файла книги
        Path result = null;
        boolean isZiped = ZipFileUtil.isZipped(book.getFile());  // Файл источника архивирован
        try {
            Path folder = Path.of(root, book.getFolder().getFolder());  // Конечная папка расположения файла
            if (!Files.exists(folder))
                folder = Files.createDirectories(folder);
            if (isZiped) {
                var ext = getExtension(book.getFile());
                var name = book.getFile().replace(ext, "");
                result = ZipFileUtil.writeZipFile(pathSource, folder.toString(),name);
            } else {
                result = Files.copy(book.getPath(), pathTo, REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION,"Ошибка добавления файла", ex.getMessage());
        }
        return result;
    }

    /**
     * Копировать файл книги в заданный каталог
     */
    public static Path copyFileOfBook(Library lib, Book book, Path target) throws IOException {
        var root = lib.getPathToBooks().toString();
        var pathSource = Path.of(root, book.getFolder().getFolder(), book.getFile());
        return Files.copy(pathSource, target.resolve(book.getFile()), REPLACE_EXISTING);
    }

    /**
     * Список файлов в каталоге и подкаталогах
     */
    public static ArrayList<String> listFilesOfDir(String dir) {
        ArrayList<String> listOfFile = new ArrayList<>();
        try {
            Files.walkFileTree(Path.of(dir), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes bfa) throws IOException {
                    listOfFile.add(path.toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION,"Ошибка обработки каталога", ex.getMessage());
        }
        return listOfFile;
    }
}
