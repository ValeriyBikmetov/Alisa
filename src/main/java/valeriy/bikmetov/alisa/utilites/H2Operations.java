package valeriy.bikmetov.alisa.utilites;

import valeriy.bikmetov.alisa.model.*;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 *
 * @author Валерий Бикметов
 */
public final class H2Operations {
    private H2Operations() {
        throw new AssertionError();
    }
//******************************** Новые операции ***************************

//******************************** Служебные запросы ***************************
    /**
     * Заключить строку в одинарные кавычки
     * @param str - входная строка
     * @return - входная строка заключенная в одинарные кавычки
     */
    private static String strInQuote(String str) {
        return '\'' + str + '\'';
    }

    /**
     * Получить текст запроса из таблицы QUERIES
     * @param stm Current Statment
     * @param name - имя запроса
     * @return - строка запроса
     */
    public static String getQuery(Statement stm, String name) throws SQLException {
        String result = null;
        String sb = "SELECT SQL_QUERY FROM QUERIES WHERE NAMEQUERY = '" + name + '\'';
        try (ResultSet rs = stm.executeQuery(sb)) {
            if (rs.next()) {
                result = rs.getString(1);
            }
        }
        return result;
    }

    public static String getQuery(Connection conn, String name) throws SQLException {
        String result;
        try(Statement stm = conn.createStatement()) {
            result = getQuery(stm, name);
        }
        return result;
    }

    /**
     * Получит максимальное значение первичного ключа
     * @param stm - current Statment
     * @param table - current table
     * @param field - column
     * @return - max value
     */
    public static int getMaxValue(Statement stm, String table, String field) throws SQLException {
        int result = 0;
        String query = String.format("SELECT MAX(%s) FROM %s", field, table);
        try(ResultSet rs = stm.executeQuery(query)) {
            if(rs.next()) {
                result = rs.getInt(1);
            }
        }
        return result;
    }
//*****************************************************************************************
//************************* Работа с таблицой Library ***************************************
//************************* Получит параметры библиотеки *************************************
    /**
     * Получить параметры библиотеки (Используется в новой программе)
     * @param conn соединение с базой
     * @return массив параметров библиотеки (id, libname, pathToBooks, notmove)
     */
    public static ArrayList<Object> getLibParams(Connection conn) throws SQLException {
        ArrayList<Object> params = new ArrayList<>();
        try(Statement stm = conn.createStatement()) {
            String query = getQuery(stm, "GET_LIB_PARAMS");
            try(ResultSet rs = stm.executeQuery(query)) {
                rs.next();
                params.add(rs.getInt(1)); // ID
                params.add(rs.getString(2)); // libname
                params.add(rs.getString(3)); // pathToBooks
                params.add(rs.getString(4)); // notmove
            }
        }
        return params;
    }

    /**
     * Добавить данные библиотеки в базу
     * @param conn - соединение с базой данных
     * @param libid - идентификаор библиотеки
     * @param libname - наименование библиотеки
     * @param pathto - путь к базе данных
     * @param typeLib - тип библиотеки
     * @return - возвращается идентификатор библиотеки
     */
    public static int insertLibToDb(Connection conn, int libid, String libname, String pathto, String typeLib) throws SQLException {
        int result;
        try (Statement stm = conn.createStatement()) {
            String query = getQuery(stm, "INSERT_INTO_LIBRARY");
            String name = strInQuote(libname);
            String folder = strInQuote(pathto);
            String type = strInQuote(typeLib);
            query = String.format(query, libid, name, folder, type);
            result = stm.executeUpdate(query);
        }
        return result;
    }

    public static int updatePathTo(Connection conn, Path path) throws SQLException {
        int result;
        try (Statement stm = conn.createStatement()) {
            String sPath = strInQuote(path.toString());
            String query = String.format("UPDATE LIBRARY SET PATHTO = %s", sPath);
            result = stm.executeUpdate(query);
        }
        return result;
    }
//********************************************************************************************
//************************* Работа с таблицой Book *********************************************
//*************** Создать класс Book по ее идентификатору ***********************************
    public static Book makeBook(Connection conn, int bookid) throws SQLException {
        Book book;
        try (Statement stm = conn.createStatement()) {
            book = makeBook(stm, bookid);
        }
        return book;
    }

    public static Book makeBook(Statement stm, int bookid) throws SQLException {
        Book book = new Book();
        book.setId(bookid);
        book.setFresh(false);
        ArrayList<Author> authors = makeListAuthor(stm, bookid);
        for(Author author : authors) {
            book.addAuthor(author);
        }
        ArrayList<Series> series = makeListSeries(stm, bookid);
        if (series != null) {
            for(Series ser : series) {
                book.addSeries(ser);
            }
        }
        ArrayList<Genre> genries = makeListGenre(stm, bookid);
        for(Genre genre : genries) {
            book.addGenre(genre);
        }
        String query = String.format(getQuery(stm, "GET_BOOK_INFO"), bookid);
        try (ResultSet rs = stm.executeQuery(query)) {
            if (rs.next()) {
                book.setTitle(rs.getString(2));
                book.setFile(rs.getString(4));
                book.setFsize(rs.getInt(5));
                book.setCoverpage(rs.getString(6));
                book.setEncoding(rs.getString(7));
                int folderid = rs.getInt(3);
                Folder folder = makeFolder(stm, folderid);
                book.setFolder(folder);
                book.setFolderName(folder.getFolder());
            }
        }
        return  book;
    }
    //********************* Добавление новой книги **********************************************
    public static void newBookID(Connection conn, Book book) throws SQLException {
        String query;
        correctNewBookItems(conn, book);
        try(Statement stm = conn.createStatement()) {
            var bookid = addBook(stm, book);
            if(bookid > 1) {
                query = getQuery(stm, "INS_AUTHORBOOK");
                ArrayList<Author> authors = book.getAuthors();
                for(Author author : authors) {
                    String str = String.format(query, author.getId(), bookid);
                    stm.executeUpdate(str);
                }
                query = getQuery(stm, "INS_SERIESBOOK");
                ArrayList<Series> series = book.getSeries();
                for(Series serie : series) {
                    String str = String.format(query, serie.getId(), bookid, serie.getNum());
                    stm.executeUpdate(str);
                }
                query = getQuery(stm, "INS_GENREBOOK");
                ArrayList<Genre> genres = book.getGenre();
                for(Genre genre : genres) {
                    String str = String.format(query, genre.getId(), bookid);
                    stm.executeUpdate(str);
                }
            }
        }
    }
    // ********************* Добавить книгу ******************************************************
    public static int addBook(Statement stm, Book book) throws SQLException {
        var bookid = getMaxValue(stm, "BOOK", "BOOKID") + 1;
        String booktitle = strInQuote(book.getTitle());
        int folder = book.getFolder().getId();
        String filename = strInQuote(book.getFile());
        int fsize = book.getFsize();
        String cover = strInQuote(book.getCoverpage());
        String encode = strInQuote(book.getEncoding());
        String query = String.format(getQuery(stm, "INSERT_BOOK"), bookid, booktitle, folder, filename, fsize, cover, encode);
        int result = stm.executeUpdate(query);
        if(result > 0) {
            return bookid;
        }
        return result;
    }
    // ****************** Коррекция идентификаторов параметров новой книги ***********************
    private static void correctNewBookItems(Connection conn, Book book) throws SQLException {
        ArrayList<Author> authors = book.getAuthors();
        for(Author author : authors) {
            if(author.getId() == 0) {
                int id = newAuthorID(conn, author);
                if(id > 1) {
                    author.setId(id);
                }
            }
        }
        ArrayList<Series> series = book.getSeries();
        for(Series serie : series) {
            if(serie.getId() == 0) {
                int id = newSeriesID(conn, serie);
                if(id > 1) {
                    serie.setId(id);
                }
            }
        }
        ArrayList<Genre> genres = book.getGenre();
        for(Genre genre : genres) {
            if(genre.getId() == 0) {
                int id = newGenreID(conn, genre);
                if(id > 1) {
                    genre.setId(id);
                }
            }
        }
        if(book.getFolder().getId() == 0) {
            int id = newFolderID(conn, book.getFolder());
            if(id > 1) {
                book.getFolder().setId(id);
            }
        }
    }
    // ************************ Изменение свойств книги ******************************************
    public static void changeBookProperty(Connection conn, Book book, Book oldBook) throws SQLException {
        correctNewBookItems(conn, book);
        try (Statement stm = conn.createStatement()) {
            if (!book.getTitle().equals(oldBook.getTitle())) {
                String title = strInQuote(book.getTitle());
                String query = String.format(getQuery(stm, "UPDATE_TITLE"), title, oldBook.getId());
                stm.executeUpdate(query);
            }
            ArrayList<Author> authors = book.getAuthors();
            ArrayList<Author> oldAuthors = oldBook.getAuthors();
            for (Author author : oldAuthors) {
                if (!authors.contains(author)) {
                    delAuthorBook(stm, author.getId(), oldBook.getId());
                    deleteAuthor(stm, author.getId());
                }
            }
            for (Author author : authors) {
                if (!oldAuthors.contains(author)) {
                    addAuthorBook(stm, author.getId(), oldBook.getId());
                }
            }
            ArrayList<Series> series = book.getSeries();
            ArrayList<Series> oldSeries = oldBook.getSeries();
            for(Series serie : oldSeries) {
                if(!series.contains(serie)) {
                    delSeriesBook(stm, serie, oldBook.getId());
                    delSeries(stm, serie.getId());
                }
            }
            for(Series serie : series) {
                if(!oldSeries.contains(serie)) {
                    addSeriesBook(stm, serie, oldBook.getId());
                }
            }
            ArrayList<Genre> genres = book.getGenre();
            ArrayList<Genre> oldGenre = oldBook.getGenre();
            for(Genre genre : oldGenre) {
                if(!genres.contains(genre)) {
                    delGenreBook(stm, genre.getId(), oldBook.getId());
                }
            }
            for(Genre genre : genres) {
                if(!oldGenre.contains(genre)) {
                    addGenreBook(stm, genre.getId(), oldBook.getId());
                }
            }
            if(!book.getFile().equals(oldBook.getFile())) {
                String file = strInQuote(book.getFile());
                String query = String.format(getQuery(stm, "UPDATE_FILE"), book.getFolder().getId(), file, book.getId());
                stm.executeUpdate(query);
            }
        }
    }
    // ******************************* Удаление книги *******************************************
    public static void deleteBook(Connection conn, Book book) throws SQLException {
        try(Statement stm = conn.createStatement()) {
            deleteBook(stm, book);
        }
    }
    public static void deleteBook(Statement stm, Book book) throws SQLException {
        ArrayList<Genre> genres = book.getGenre();
        for(Genre genre : genres) {
            delGenreBook(stm, genre.getId(), book.getId());
        }
        ArrayList<Series> series = book.getSeries();
        for(Series serie : series) {
            delSeriesBook(stm, serie, book.getId());
            delSeries(stm, serie.getId());
        }
        ArrayList<Author> authors = book.getAuthors();
        for(Author author : authors) {
            delAuthorBook(stm, author.getId(), book.getId());
            deleteAuthor(stm,author.getId());
        }
        String query = String.format(getQuery(stm, "DEL_BOOK"), book.getId());
        stm.executeUpdate(query);
    }
    // ****************** Поиск книг того же автора и с тем же названием *************************
    public static ArrayList<Integer> findBooks(Connection conn, Book book) throws SQLException {
        var result = new ArrayList<Integer>();
        Author author = book.getAuthors().getFirst();
        String name = "'" + author.getName().split(" ")[0] + "%'";
        var title = "'%" + book.getTitle() + "%'";
        var query = String.format(getQuery(conn, "QUERY_BOOK_FILE"), title, name);
        try (Statement stm = conn.createStatement()) {
            try (var rs = stm.executeQuery(query)) {
                while ((rs.next())) {
                    var item = rs.getInt(1);
                    result.add(item);
                }
            }
        }
        return result;
    }
    // **************************** Поиск книги по ее названию ***************************************
    public static ArrayList<Integer> findBooks(Connection conn, String title) throws SQLException {
        var result = new ArrayList<Integer>();
        var name = "'%" + title + "%'";
        var query = String.format(getQuery(conn, "QUERY_BOOKID_TITLE"), name);
        try (Statement stm = conn.createStatement()) {
            try (ResultSet rs = stm.executeQuery(query)) {
                while (rs.next()) {
                    var item = rs.getInt(1);
                    result.add(item);
                }
            }
        }
        return result;
    }
//*****************************************************************************************
//************************* Работа с таблицой Author ****************************************
//*************** Создать класс Author по его идентификатору **********************************
    public static Author makeAuthor(Connection conn, int authorid) throws SQLException {
        Author author;
        try (Statement stm = conn.createStatement()) {
            author = makeAuthor(stm,authorid);
        }
        return author;
    }
    public static Author makeAuthor(Statement stm, int authorid) throws SQLException {
        Author author = new Author();
        String query = String.format(getQuery(stm, "AUTHOR_PARAMS"), authorid);
        try (ResultSet rs = stm.executeQuery(query)) {
            if (rs.next()) {
                String name = rs.getString(2);
                author.setId(authorid);
                author.setName(name);
            }
        }
        return author;
    }
    // ********************* Получить список Author для данной книги **********************
    public static ArrayList<Author> makeListAuthor(Connection conn, int bookid) throws SQLException {
        ArrayList<Author> list = new ArrayList<>();
        ArrayList<Integer>  authors = getListAuthor(conn, bookid);
        for(int id : authors) {
            Author author = makeAuthor(conn, id);
            list.add(author);
        }
        return list;
    }
    public static ArrayList<Author> makeListAuthor(Statement stm,int bookid) throws SQLException {
        ArrayList<Author> list = new ArrayList<>();
        ArrayList<Integer>  authors = getListAuthor(stm, bookid);
        for(int id : authors) {
            Author author = makeAuthor(stm, id);
            list.add(author);
        }
        return list;
    }
    // ******************** Создать нового автора и получить его идентификатор *******************
    public static int newAuthorID(Connection conn, Author author) throws SQLException {
        int authorID = getAuthorID(conn, author.getName());
        if (authorID > 0)
            return authorID;
        try(Statement stm = conn.createStatement()) {
            authorID = getMaxValue(stm, "AUTHOR", "AUTHORID") + 1;
            String name = strInQuote(author.getName());
            String query = String.format(getQuery(stm, "NEW_AUTHOR"), authorID, name);
            int res = stm.executeUpdate(query);
            if(res > 0) {
                return  authorID;
            } return res;
        }
    }
    // **************************** Получить AUTHORID по фамилии и имени *************************
    public static int getAuthorID(Connection conn, String name) throws SQLException {
        int result = 0;
        try(Statement stm = conn.createStatement()) {
            String sName = strInQuote(name);
            // TODO Откорректировать запрос
            String query = String.format(getQuery(stm, "GET_AUTHORID"), sName);
            try (ResultSet rs = stm.executeQuery(query)){
                if(rs.next()) {
                    result = rs.getInt(1);
                }
            }
        }
        return result;
    }
    // *********************** Удалить автора *****************************************************
    public static void deleteAuthor(Connection conn, int authorid) throws SQLException {
        try(Statement stm = conn.createStatement()) {
            deleteAuthor(stm, authorid);
        }
    }
    public static void deleteAuthor(Statement stm, int authorid) throws SQLException {
        // Определяем количество книг данного автора
        int count = 0;
        String query = String.format(getQuery(stm, "COUNT_IN_AUTHOR"), authorid);
        try (ResultSet rs = stm.executeQuery(query)) {
            if (rs.next()) {
                count = rs.getInt(1);
            }
        }
        if (count == 0) {
            query = String.format(getQuery(stm, "DEL_AUTHOR"), authorid);
            stm.executeUpdate(query);
        }
    }
    // ************************** Удалить автора вместе с книгами ***********************************
    public static void delAuthorWithBooks(Connection conn, ArrayList<Book> books) throws SQLException {
        try(Statement stm = conn.createStatement()) {
            delAuthorWithBooks(stm, books);
        }
    }
    public static void delAuthorWithBooks(Statement stm, ArrayList<Book> books) throws SQLException {
        if(books != null && !books.isEmpty()) {
            for(Book book : books) {
                deleteBook(stm, book);
            }
        }
    }
    // *************************** Change Author settings *************************************
    public static void changeAuthor(Connection conn, Author author) throws SQLException {
        try(Statement stm = conn.createStatement()) {
            changeAuthor(stm, author);
        }
    }
    public static void changeAuthor(Statement stm, Author author) throws SQLException {
        String name = strInQuote(author.getName());
        int id = author.getId();
        String query = String.format(getQuery(stm, "CHANGE_AUTHOR"), name, id);
        stm.executeUpdate(query);
    }
//********************************************************************************************
//************************* Работа с таблицой Authorbook****************************************
//*************** Получить список идентификаторов авторов книги ***********************************
    public static ArrayList<Integer> getListAuthor(Connection conn, int bookid) throws SQLException {
        ArrayList<Integer> list;
        try (Statement stm = conn.createStatement()) {
            list = getListAuthor(stm,bookid);
        }
        return list;
    }
    public static ArrayList<Integer> getListAuthor(Statement stm, int bookid) throws SQLException {
        ArrayList<Integer> list = new ArrayList<>();
        String query = String.format(getQuery(stm, "GET_LIST_AUTHOR"), bookid);
        try(ResultSet rs = stm.executeQuery(query)) {
            while(rs.next()) {
                int i = rs.getInt(1);
                list.add(i);
            }
        }
        return list;
    }
    // *********************** Добавить книгу в AUTHORBOOK ***************************************
    public static void addAuthorBook(Connection conn, int authorid, int bookid) throws SQLException {
        try(Statement stm = conn.createStatement()) {
            addAuthorBook(stm, authorid, bookid);
        }
    }
    public static void addAuthorBook(Statement stm, int authorid, int bookid) throws SQLException {
        String query = String.format(getQuery(stm, "INS_AUTHORBOOK"), authorid, bookid);
        stm.executeUpdate(query);
    }
    // **************************** Удалит книгу из AUTHORBOOK ***********************************
    public static void delAuthorBook(Connection conn, int authorid, int bookid) throws SQLException {
        try(Statement stm = conn.createStatement()) {
            delAuthorBook(stm, authorid, bookid);
        }
    }
    public static void delAuthorBook(Statement stm, int authorid, int bookid) throws SQLException {
        String query = String.format(getQuery(stm, "DEL_AUTHORBOOK"), authorid, bookid);
        stm.executeUpdate(query);
    }
    //********************* Прлучить массив книг данного автора *************************************
    public static ArrayList<Book> getAuthorBooks(Connection conn, int authorid) throws SQLException {
        try(Statement stm = conn.createStatement()) {
            return getAuthorBooks(stm, authorid);
        }
    }
    public static ArrayList<Book> getAuthorBooks(Statement stm, int authorid) throws SQLException {
        String query = String.format(getQuery(stm, "COUNT_IN_AUTHOR"), authorid);
        int count = 0;
        ArrayList<Book> listBooks = new ArrayList<>();
        ArrayList<Integer> listId = new ArrayList<>();
        try(ResultSet rs = stm.executeQuery(query)) {
            if(rs.next()) {
                count = rs.getInt(1);
            }
        }
        if(count > 0) {
            query = String.format(getQuery(stm, "AUTHOR_BOOKS"), authorid);
            try(ResultSet rs = stm.executeQuery(query)) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    listId.add(id);
                }
            }
            for(int id : listId) {
                Book book = makeBook(stm, id);
                listBooks.add(book);
            }
        }
        return listBooks;
    }

    // ************************* Заменить автора на другого **********************************
    public static int replacmentAuthor(Connection conn, Author oldAuthor, Author newAuthor) throws SQLException {
        int result = 0;
        try (Statement statement = conn.createStatement()) {
            result = replacmentAuthor(statement, oldAuthor, newAuthor);
        }
        return result;
    }

    public static int replacmentAuthor(Statement statement, Author oldAuthor, Author newAuthor) throws SQLException {
        var oldId = oldAuthor.getId();
        var newId = newAuthor.getId();
        String query = String.format(getQuery(statement, "REPLACEMENT_AUTHOR"), newId, oldId);
        return statement.executeUpdate(query);
    }
//********************************************************************************************
//************************* Работа с таблицой Genre ********************************************
//*************** Создать класс Genre по его идентификатору **************************************
    public static Genre makeGenre(Connection conn, int genreid) throws SQLException {
        Genre genre;
        try (Statement stm = conn.createStatement()) {
            genre = makeGenre(stm, genreid);
        }
        return genre;
    }
    public static Genre makeGenre(Statement stm, int genreid) throws SQLException {
        Genre genre = new Genre();
        String queru = String.format(getQuery(stm, "GENRE_PARAMS"), genreid);
        try (ResultSet rs = stm.executeQuery(queru)) {
            if (rs.next()) {
                genre.setId(genreid);
                String cod = rs.getString(2);
                String desc = rs.getString(3);
                String group = rs.getString(4);
                genre.setGenre(cod);
                genre.setDescription(desc);
                genre.setGroup(group);
            }
        }
        return genre;
    }
    // ******************* Получить список объектов Genre для данной книги ***********************
    public static ArrayList<Genre> makeListGenre(Connection conn, int bookid) throws SQLException {
        ArrayList<Genre> genres;
        try(Statement stm = conn.createStatement()) {
            genres = makeListGenre(stm, bookid);
        }
        return genres;
    }
    public static ArrayList<Genre> makeListGenre(Statement stm, int bookid) throws SQLException {
        ArrayList<Genre> genres = new ArrayList<>();
        ArrayList<Integer> list = getListGenre(stm, bookid);
        for(int id : list) {
            Genre genre = makeGenre(stm, id);
            genres.add(genre);
        }
        return genres;
    }
    // **************************** Новый жанр ***************************************************
    public static int newGenreID(Connection conn, Genre genre) throws SQLException {
        int genreID = getGenreID(conn, genre.getGenre());
        if (genreID > 0)
            return genreID;
        String genrecod = strInQuote(genre.getGenre());
        String desc = strInQuote(genre.getDescription());
        String genremeta = strInQuote(genre.getGroup());
        try(Statement stm = conn.createStatement()) {
            genreID = getMaxValue(stm, "GENRE", "GENREID") + 1;
            String query = String.format(getQuery(stm, "NEW_GENRE"), genreID, genrecod, desc, genremeta);
            var result = stm.executeUpdate(query);
            if(result > 0) {
                return genreID;
            }
            return result;
        }
    }
    // ********************* Ghange Genre settings ***********************************************
    public static void changeGenre(Connection conn, Genre genre) throws SQLException {
        try(Statement stm = conn.createStatement()) {
            changeGenre(stm, genre);
        }
    }
    public static void changeGenre(Statement stm, Genre genre) throws SQLException {
        String genrecod = strInQuote(genre.getGenre());
        String genredesc = strInQuote(genre.getDescription());
        String genremeta = strInQuote(genre.getGroup());
        int id = genre.getId();
        String query = String.format(getQuery(stm, "CHANGE_GENRE"), genrecod, genredesc, genremeta, id);
        stm.executeUpdate(query);
    }
    // ****************************** Get Genre ID by name *************************************
    public static int getGenreID(Connection conn, String cod) throws SQLException {
        try (Statement stm = conn.createStatement()) {
            return getGenreID(stm, cod);
        }
    }
    public static int getGenreID(Statement stm, String cod) throws SQLException {
        String codgenre = strInQuote(cod);
        String query = String.format(getQuery(stm, "GET_GENREID"), codgenre);
        int result = 0;
        try(ResultSet rs = stm.executeQuery(query)) {
            if(rs.next()) {
                result = rs.getInt(1);
            }
        }
        return result;
    }
//********************************************************************************************
//************************* Работа с таблицой Genrebook***************************************
//*************** Получить список идентификаторов жанров для книги ***************************
    public static ArrayList<Integer> getListGenre(Connection conn, int bookid) throws SQLException {
        ArrayList<Integer> list;
        try (Statement stm = conn.createStatement()) {
            list = getListGenre(stm,bookid);
        }
        return list;
    }
    public static ArrayList<Integer> getListGenre(Statement stm, int bookid) throws SQLException {
        ArrayList<Integer> list = new ArrayList<>();
        String query = String.format(getQuery(stm, "GET_LIST_GENRE"), bookid);
        try (ResultSet rs = stm.executeQuery(query)) {
            while (rs.next()) {
                int i = rs.getInt(1);
                list.add(i);
            }
        }
        return list;
    }
    // ************************** Добавить книгу в GENREBOOK *************************************
    public static void addGenreBook(Connection conn, int genreid, int bookid) throws SQLException {
        try (Statement stm = conn.createStatement()) {
            addGenreBook(stm, genreid, bookid);
        }
    }
    public static void addGenreBook(Statement stm, int genreid, int bookid) throws SQLException {
        String query = String.format(getQuery(stm, "INS_GENREBOOK"), genreid, bookid);
        stm.executeUpdate(query);
    }
    // ********************* Удалить книгу из GENREBOOK ******************************************
    public static void delGenreBook(Statement stm, int genreid, int bookid) throws SQLException {
        String query = String.format(getQuery(stm, "DEL_GENREBOOK"), genreid, bookid);
        stm.executeUpdate(query);
    }
    public static void delGenreBook(Connection conn, int genreid, int bookid) throws SQLException {
        try(Statement stm = conn.createStatement()) {
            delGenreBook(stm, genreid, bookid);
        }
    }
    // *************************** Удаление всех книг этого жанра ***********************************
    public static void delBooksOfGenre(Connection conn, ArrayList<Book> listBooks) throws SQLException {
        try(Statement stm = conn.createStatement()) {
            delBooksOfGenre(stm, listBooks);
        }
    }
    public static void delBooksOfGenre(Statement stm, ArrayList<Book> listBooks) throws SQLException {
        for(Book book : listBooks) {
            deleteBook(stm, book);
        }
    }
    //************************** Get a list of books of this genre *******************************
    public static ArrayList<Book> getBooksOfGenre(Connection conn, int genreid) throws SQLException {
        try(Statement stm = conn.createStatement()) {
            return getBooksOfGenre(stm, genreid);
        }
    }
    public static ArrayList<Book> getBooksOfGenre(Statement stm, int genreid) throws SQLException {
        String query = String.format(getQuery(stm, "COUNT_IN_GENRE"), genreid);
        int count = 0;
        ArrayList<Book> listBooks = new ArrayList<>();
        try(ResultSet rs = stm.executeQuery(query)) {
            if(rs.next()) {
                count = rs.getInt(1);
            }
        }
        if(count >0) {
            ArrayList<Integer> listId = new ArrayList<>();
            query = String.format(getQuery(stm, "GENRE_BOOKS"), genreid);
            try (ResultSet rs = stm.executeQuery(query)){
                while (rs.next()) {
                    int id = rs.getInt(1);
                    listId.add(id);
                }
            }
            for (int id : listId) {
                Book book = makeBook(stm, id);
                listBooks.add(book);
            }
        }
        return listBooks;
    }
//********************************************************************************************
//************************* Работа с таблицой Series *******************************************
//*************** Создать класс Series по его идентификаторe *************************************
    public static Series makeSeries(Connection conn, int seriesid, int num) throws SQLException {
        Series series;
        try (Statement stm = conn.createStatement()) {
            series = makeSeries(stm, seriesid,num);
        }
        return series;
    }
    public static Series makeSeries(Statement stm, int seriesid, int num) throws SQLException {
        Series series = new Series();
        String query = String.format(getQuery(stm, "SERIES_PARAMS"), seriesid);
        try (ResultSet rs = stm.executeQuery(query)) {
            if (rs.next()) {
                series.setId(seriesid);
                String name = rs.getString(2);
                series.setName(name);
                if (num > 0) {
                    series.setNum(num);
                }
            }
        }
        return series;
    }
    // ***************** Получить список объектов Series для данной книги ************************
    public static ArrayList<Series> makeListSeries(Connection conn, int bookid) throws SQLException {
        ArrayList<Series> series;
        try(Statement stm = conn.createStatement()) {
            series = makeListSeries(stm, bookid);
        }
        return series;
    }
    public static ArrayList<Series> makeListSeries(Statement stm, int bookid) throws SQLException {
        ArrayList<Integer> list = getListSeries(stm, bookid);
        if(list.isEmpty()){
            return null;
        }
        ArrayList<Series> series = new ArrayList<>();
        for(int i = 0; i < list.size(); i += 2 ) {
            int id = list.get(i);
            int num = list.get(i + 1);
            Series ser = makeSeries(stm, id, num);
            series.add(ser);
        }
        return series;
    }
    // ************************* Новая серия *****************************************************
    public static int newSeriesID(Connection conn, Series series) throws SQLException {
        int seriesID = getSeriesID(conn, series.getName());
        if (seriesID > 0)
            return seriesID;
        String name = strInQuote(series.getName());
        try(Statement stm = conn.createStatement()) {
            seriesID = getMaxValue(stm, "SERIES", "SERIESID") + 1;
            String query = String.format(getQuery(stm, "NEW_SERIES"), seriesID, name);
            var result = stm.executeUpdate(query);
            if(result > 0) {
                return seriesID;
            }
            return result;
        }
    }
    // *************************** Удалить серию *************************************************
    public static void delSeries(Connection conn, int seriesid) throws SQLException {
        try(Statement stm = conn.createStatement()) {
            delSeries(stm, seriesid);
        }
    }
    public static void delSeries(Statement stm, int seriesid) throws SQLException {
        String query = String.format(getQuery(stm, "COUNT_IN_SERIES"), seriesid);
        int count = 0;
        try(ResultSet rs = stm.executeQuery(query)) {
            if(rs.next()) {
                count = rs.getInt(1);
            }
        }
        if(count == 0) {
            query = String.format(getQuery(stm, "DEL_SERIES"), seriesid);
            stm.executeUpdate(query);
        }
    }
    // ********************************** Удалить серию вместе с книгами ****************************
    public static void delSeriesWithBooks(Connection conn, ArrayList<Book> listBooks) throws SQLException {
        try(Statement stm = conn.createStatement()) {
            delSeriesWithBooks(stm, listBooks);
        }
    }
    public static void delSeriesWithBooks(Statement stm, ArrayList<Book> listBooks) throws SQLException {
        for (Book book : listBooks) {
            deleteBook(stm, book);
        }
    }
    // ******************************** Get Series ID by name *******************************
    public static int getSeriesID(Connection conn, String name) throws  SQLException {
        try(Statement stm = conn.createStatement()) {
            return getSeriesID(stm, name);
        }
    }
    public static int getSeriesID(Statement stm, String name) throws SQLException {
        String nameSeries = strInQuote(name);
        String query = String.format(getQuery(stm, "GET_SERIESID"), nameSeries);
        int id = 0;
        try(ResultSet rs = stm.executeQuery(query)) {
            if(rs.next()) {
                id = rs.getInt(1);
            }
        }
        return id;
    }
    // **************************** Change Series settings ***************************************
    public static void changeSeries(Connection conn, Series series) throws SQLException {
        try(Statement stm = conn.createStatement()) {
            changeSeries(stm, series);
        }
    }
    public static void changeSeries(Statement stm, Series series) throws SQLException {
        String name = strInQuote(series.getName());
        int id = series.getId();
        String query = String.format(getQuery(stm, "GANGE_SERIES"), name, id);
        stm.executeUpdate(query);
    }
//********************************************************************************************
//************************* Работа с таблицой Seriesbook**************************************
//*************** Получить список идентификаторов серий для книги ****************************
    public static ArrayList<Integer> getListSeries(Connection conn, int bookid) throws SQLException {
        ArrayList<Integer> list;
        try (Statement stm = conn.createStatement()) {
            list = getListSeries(stm, bookid);
        }
        return list;
    }
    public static ArrayList<Integer> getListSeries(Statement stm, int bookid) throws SQLException {
        ArrayList<Integer> list = new ArrayList<>();
        String query = String.format(getQuery(stm, "GET_LIST_SERIES"), bookid);
        try (ResultSet rs = stm.executeQuery(query)) {
            while (rs.next()) {
                int i = rs.getInt(1);
                list.add(i);
                i = rs.getInt(2); //Номер книги в серии
                list.add(i);
            }
        }
        return list;
    }
    // *********************** Добавить книгу в SERIESBOOK ***************************************
    public static void addSeriesBook(Statement stm, Series series, int bookid) throws SQLException {
        String query = String.format(getQuery(stm, "INS_SERIESBOOK"), series.getId(), bookid, series.getNum());
        stm.executeUpdate(query);
    }
    public static void addSeriesBook(Connection conn, Series series, int bookid) throws SQLException {
        try(Statement stm = conn.createStatement()) {
            addSeriesBook(stm, series, bookid);
        }
    }
    // ***************************** Удалить книгу из SERIESBOOK *********************************
    public static void delSeriesBook(Statement stm, Series series, int bookid) throws SQLException {
        String query = String.format(getQuery(stm, "DEL_SERIESBOOK"), series.getId(), bookid, series.getNum());
        stm.executeUpdate(query);
    }
    public static void delSeriesBook(Connection conn, Series series, int bookid) throws SQLException {
        try(Statement stm = conn.createStatement()) {
            delSeriesBook(stm, series, bookid);
        }
    }
    //*********************** Get list books of the series ***************************************
    public static ArrayList<Book> getSeriesBooks(Connection conn, int seriesid) throws SQLException {
        try (Statement stm = conn.createStatement()){
            return getSeriesBooks(stm, seriesid);
        }
    }
    public static ArrayList<Book> getSeriesBooks(Statement stm, int seriesid) throws SQLException {
        String query = String.format(getQuery(stm, "COUNT_IN_SERIES"), seriesid);
        int count = 0;
        ArrayList<Book> listBooks = new ArrayList<>();
        ArrayList<Integer> listId = new ArrayList<>();
        try(ResultSet rs = stm.executeQuery(query)) {
            if(rs.next()) {
                count = rs.getInt(1);
            }
        }
        if(count > 0) {
            query = String.format(getQuery(stm,"SERIES_BOOKS"), seriesid);
            try (ResultSet rs = stm.executeQuery(query)){
                while (rs.next()) {
                    int id = rs.getInt(1);
                    listId.add(id);
                }
            }
            for(int id : listId) {
                Book book = makeBook(stm, id);
                listBooks.add(book);
            }
        }
        return listBooks;
    }
    // ************************* Заменить автора на другого **********************************
    public static void replacmentSeries(Connection conn, Series oldSeries, Series newSeries) throws SQLException {
        int result = 0;
        try (Statement statement = conn.createStatement()) {
            var oldId = oldSeries.getId();
            var newId = newSeries.getId();
            String query = String.format(getQuery(statement, "REPLACEMENT_SERIES"), newId, oldId);
            result =  statement.executeUpdate(query);
        }
    }
//********************************************************************************************
//************************* Работа с таблицой Folders ****************************************
//******************** Создать класс Folder по идентификатору ********************************
    public static Folder makeFolder(Connection conn, int folderid) throws SQLException {
        Folder folder;
        try (Statement stm = conn.createStatement()) {
            folder = makeFolder(stm,folderid);
        }
        return folder;
    }
    public static Folder makeFolder(Statement stm, int folderid) throws SQLException {
        Folder folder = new Folder();
        String query = String.format(getQuery(stm, "FOLDER_PARAMS"), folderid);
        try (ResultSet rs = stm.executeQuery(query)) {
            if (rs.next()) {
                folder.setId(folderid);
                String name = rs.getString(2);
                folder.setFolder(name);
            }
        }
        return  folder;
    }
    // ******************************** Новая папка **********************************************
    public static int newFolderID(Connection conn, Folder folder) throws SQLException {
        int result = 0;
        try(Statement stm = conn.createStatement()) {
            result = newFolderID(stm, folder);
        }
        return result;
    }
    public static int newFolderID(Statement stm, Folder folder) throws SQLException {
        int folderID = getFolderIdByName(stm, folder.getFolder());
        if (folderID > 0)
            return folderID;
        folderID = getMaxValue(stm, "FOLDERS", "FOLDERID") + 1;
        String name = folder.getFolder();
        String query = String.format(getQuery(stm, "NEW_FOLDER"), folderID, strInQuote(name));
        var result = stm.executeUpdate(query);
        if(result > 0) {
            return folderID;
        }
        return result;
    }
    // ******************** Delete Folder ********************************************************
    public static void deleteFolder(Connection conn, int folderid) throws SQLException {
        int result = 0;
        try(Statement stm = conn.createStatement()) {
            result = deleteFolder(stm, folderid);
        }
    }
    public static int deleteFolder(Statement stm, int folderid) throws SQLException{
        String query = String.format(getQuery(stm, "DEL_FOLDER"), folderid);
        return stm.executeUpdate(query);
    }
    // *********************** Get FolderID by Name **************************************************
    public static int getFolderIdByName(Connection conn, String name) throws SQLException {
        int result = 0;
        try (Statement stm = conn.createStatement()) {
            result = getFolderIdByName(stm, name);
        }
        return result;
    }
    public static int getFolderIdByName(Statement stm, String name) throws SQLException {
        int result = 0;
        if (!name.startsWith("'"))
            name = strInQuote(name);
        String query = String.format(getQuery(stm, "GET_FOLDERID"), name);
        try (ResultSet rs = stm.executeQuery(query)) {
            if (rs.next())
                return rs.getInt(1);
            else return 0;
        }
    }
}