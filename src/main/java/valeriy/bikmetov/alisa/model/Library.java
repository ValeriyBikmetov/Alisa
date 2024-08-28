package valeriy.bikmetov.alisa.model;

import valeriy.bikmetov.alisa.Alisa;
import valeriy.bikmetov.alisa.utilites.Constants;
import valeriy.bikmetov.alisa.utilites.H2Connection;
import valeriy.bikmetov.alisa.utilites.H2Operations;
import valeriy.bikmetov.alisa.utilites.Utilities;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;


/**
 *
 * @author Валерий Бикметов
 */
public final class Library {
    private Connection connection; // Соединение с базой данных
    private String nameLibrary; // Наименование библиотеки
    private Path pathToDb; // Путь к файлам базы
    private Path pathToBooks; // Путь к папке с книгами
    private int indexInDb; // Индекс библиотеки в базе
    private String typeLib; // Тип библиотеки

    /**
     * Класс библиотеки
     * @param nameLib - Наименование библиотеки
     * @param pathToDb - Путь к базе данных
     * @param pathToBooks - Путь к папке с книгами
     */
    public Library(String nameLib, Path pathToDb, Path pathToBooks) {
        this.nameLibrary = nameLib;
        this.pathToDb = pathToDb;
        this.pathToBooks = pathToBooks;
    }

    public Library() {
    }

    /**
     * @return the connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Создаем соединение с базой данных библиотеки
     * @param url - URL - базы данных
     */
    public void createConnection(String url) throws SQLException, ClassNotFoundException {
        H2Connection con = new H2Connection(url);
        connection = con.getConnection();
    }

    /**
     * @param connection the connection to set
     */
    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    /**
     * @return the nameLibrary
     */
    public String getNameLibrary() {
        return nameLibrary;
    }

    /**
     * @param nameLibrary the nameLibrary to set
     */
    public void setNameLibrary(String nameLibrary) {
        this.nameLibrary = nameLibrary;
    }

    /**
     * @return the pathToDb
     */
    public Path getPathToDb() {
        return pathToDb;
    }

    /**
     * @param pathToDb the pathToDb to set
     */
    public void setPathToDb(Path pathToDb) {
        this.pathToDb = pathToDb;
    }

    /**
     * @return the pathToBooks
     */
    public Path getPathToBooks() {
        return pathToBooks;
    }

    /**
     * @param pathToBooks the pathToBooks to set
     */
    public void setPathToBooks(Path pathToBooks) {
        this.pathToBooks = pathToBooks;
    }

    /**
     * @return the indexInDb
     */
    public int getIndexInDb() {
        return indexInDb;
    }

    /**
     * @param indexInDb the indexInDb to set
     */
    public void setIndexInDb(int indexInDb) {
        this.indexInDb = indexInDb;
    }

    @Override
    public String toString() {
        return nameLibrary != null ? nameLibrary : null;
    }

    /**
     * Удаляем физически файл библиотеки
     */
    public void delete() throws SQLException, IOException {
        Path path = Path.of(Constants.FOLDER_DB);
        int id;
        try (Statement stm = connection.createStatement();
             ResultSet rs = stm.executeQuery(H2Operations.getQuery(connection, "listLib"))) {
            rs.next();
            id = rs.getInt(1);
        }
        getConnection().close();
        String prefix = "mylib" + String.format("%03d", id);
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path pth, BasicFileAttributes attrs) throws IOException {
                int index = pth.toString().indexOf(prefix);
                if(index >= 0)
                    Files.delete(pth);
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult visitFileFieled(Path pth, IOException ex) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public String getTypeLib() {
        return typeLib;
    }

    public void setTypeLib(String typeLib) {
        this.typeLib = typeLib;
    }

    /**
     * @return - возвращаем полный путь к файлу базы данных.
     * @throws IOException
     */
    public String createFileDb() throws IOException {
        Path pdb = Path.of(Constants.FOLDER_DB);
        if(pdb == null) {
            return null;
        }
        int index = getOlderstFile(pdb.toString(), "mylib*") + 1;
        setIndexInDb(index);
        String dbName = String.format("mylib%02d", index);
        return pdb.resolve(dbName).toString();
    }

    /**
     * Находим максимальный индекс файла баз данных
     * @param folder - папка для поиска
     * @param mask - какие файлы искать
     * @return - максимальный индекс файла
     * @throws IOException
     */
    public int getOlderstFile(String folder, String mask) throws IOException {
        Path pdb = Path.of(folder);
        DirectoryStream<Path> stream = Files.newDirectoryStream(pdb, mask);
        int result = 0;
        for(Path entry : stream) {
            String name = entry.getFileName().toString();
            int point = mask.indexOf('*');
            name = name.substring(point, point + 3);
            try {
                int index = Integer.parseInt(name);
                if(index > result)
                    result = index;
            } catch(NumberFormatException ex) {
            }
        }
        return result;
    }

    /**
     * Копируем файл библиотеки и записываем данные в базу
     * @param dbName - путь к новому файлу базы данных библиотеки
     * @throws SQLException
     * @throws IOException
     */
    public void writeLibData(String dbName) throws SQLException, IOException {
        try {
            Path source = Path.of(".").resolve(Constants.TEMPLATE_DB);
            Path target = Path.of(dbName + Constants.SUFFIX_DB);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            createConnection(dbName);
            setPathToDb(target);
        } catch (ClassNotFoundException | SQLException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION, "Ошибка создания базы данных библиотеки!", ex.getMessage());
        }
        String sufix = dbName.substring(dbName.length() - 3); // Порядковый номер файла
        setIndexInDb(Integer.parseInt(sufix)); // Его же ставим в качестве индекса в базе
        String libname = getNameLibrary();
        String pathto = getPathToBooks().toString();
        //String typeLib = getTypeLib();
        int result = H2Operations.insertLibToDb(getConnection(), getIndexInDb(), libname, pathto, typeLib);
        if(result == 0)
            throw new SQLException("Ошибка записи данных библиотеки " + libname);
    }

    public void createLib() throws IOException, SQLException {
        String pathDb = createFileDb();
        if(pathDb == null) {
            throw new IOException("Проблемы создания файла базы данных");
        }
        if(!getTypeLib().equals("paper")) {
            Path booksPath = getPathToBooks();
            if(!Files.exists(booksPath)) {
                Files.createDirectories(booksPath);
            }
        }
        writeLibData(pathDb);
    }
}