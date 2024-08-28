package valeriy.bikmetov.alisa.view;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import valeriy.bikmetov.alisa.Alisa;
import valeriy.bikmetov.alisa.model.*;
import valeriy.bikmetov.alisa.utilites.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;

import static valeriy.bikmetov.alisa.utilites.FileUtil.directoryChooser;

/**
 * FXML Controller class
 *
 * @author Валерий Бикметов
 */
public class BookInfoDlgController {
    private Stage dlgStage;
    private Library currentLibrary;
    private Image image;
    private Book currentBook;
//    private BooksOfDirController parent;
    private FlagsActions flagsActions;
    @FXML
    public Button btnSevenpence;
    @FXML
    public ImageView imageView;
    @FXML
    private TextField title;
    @FXML
    private TextField typeBook;
    @FXML
    private TextField folder;
    @FXML
    private ListView<Author> authors;
    @FXML
    private ListView<Series> series;
    @FXML
    private ListView<Genre> genres;
    @FXML
    public TextArea textArea;
    @FXML
    private Button btnOk;
    @FXML
    private Button btnCancel;
    @FXML
    private MenuItem mnuChangeFolder;
    @FXML
    private MenuItem mnuPrevFolder;
    @FXML
    private MenuItem mnuAddAuthor;
    @FXML
    private MenuItem mnuDelAuthor;
    @FXML
    private MenuItem mnuAddSeries;
    @FXML
    private MenuItem mnuDelSeries;
    @FXML
    private MenuItem mnuAddGenre;
    @FXML
    private MenuItem mnuDelGenre;
    @FXML
    private MenuItem mnuPrevSeries;
    @FXML
    private MenuItem mnuPrevGenre;
    @FXML
    private MenuItem mnuForPath;
    @FXML
    private MenuItem mnuSeriesToPath;
    @FXML
    private MenuItem mnuSeriesStore;
    @FXML
    private MenuItem mnuGenreStore;
    @FXML
    private MenuItem mnuChangeNum;

    @FXML
    public void initialize() {
        btnOk.setOnAction((event) -> {
            if (!currentBook.isFresh())
                makeChange();
            else writeNewBook();
        });
        btnCancel.setOnAction(event -> dlgStage.close());
        btnSevenpence.setOnAction(actionEvent -> {
            flagsActions.setBreakAct(true);
            dlgStage.close();
        });
        // ************ Menu Author *****************************************
        mnuAddAuthor.setOnAction(event -> onAddAuthor());
        mnuDelAuthor.setOnAction(event -> {
            Author author = authors.getSelectionModel().getSelectedItem();
            authors.getItems().remove(author);
        });
        mnuForPath.setOnAction(event -> onForPath());
        // ************ Menu Series *****************************************
        mnuAddSeries.setOnAction(event -> onAddSeries());
        mnuDelSeries.setOnAction(event -> {
            Series serie = series.getSelectionModel().getSelectedItem();
            series.getItems().remove(serie);
        });
        mnuPrevSeries.setOnAction(event -> onPrevSeries());
        mnuSeriesToPath.setOnAction(event -> onSeriesToPath());
        mnuSeriesStore.setOnAction(actionEvent -> onSeriesStore());
        mnuChangeNum.setOnAction(event -> onChangeNumSeries());
        // ************ Menu Genre *****************************************
        mnuAddGenre.setOnAction(event -> onAddGenre());
        mnuDelGenre.setOnAction(event -> {
            Genre genre = genres.getSelectionModel().getSelectedItem();
            genres.getItems().remove(genre);
        });
        mnuPrevGenre.setOnAction(event -> onPrevGenre());
        mnuGenreStore.setOnAction(actionEvent -> onGenreStore());
        // **************** Menu Folder ************************************
        mnuChangeFolder.setOnAction(event -> onChangeFolder());
        mnuPrevFolder.setOnAction(event -> onPrevFolder());
        // Любое изменение пути сохраняется в Constants.PREV_FOLDER.
        folder.textProperty().addListener((observableValue, s, t1) -> Constants.PREV_FOLDER = t1);
    }

    public void setCurrentBookInfo(int bookid) {
        Connection conn = currentLibrary.getConnection();
        try {
            if (bookid != 0)
                currentBook = H2Operations.makeBook(conn, bookid);
            String tmp = currentBook.getTitle().replaceAll("[/:*?\"<>«»|']", " ");
            title.setText(tmp);
            typeBook.setText(currentBook.getTypeBook());
            ArrayList<Author> lstAuthor = currentBook.getAuthors();
            if (!lstAuthor.isEmpty()) {
                if (currentBook.isFresh())
                    Utilities.correctionAuthors(lstAuthor, currentLibrary.getConnection());
                lstAuthor.forEach((item) -> {
                    authors.getItems().add(item);
                });
            }
            String path;
            if (currentBook.isFresh()) {
                if (!lstAuthor.isEmpty())  {
                    var name = lstAuthor.getFirst().getName();
                    var firstChar = name.substring(0, 1).toUpperCase();
                    path = currentLibrary.getPathToBooks().toString() + File.separator + firstChar + File.separator + name;
                } else path = "";
            } else path = currentLibrary.getPathToBooks().toString() + File.separator + currentBook.getFolder().getFolder();
            folder.setText(path);
            ArrayList<Series> lstSeries = currentBook.getSeries();
            if(!lstSeries.isEmpty()) {
                if (currentBook.isFresh())
                    Utilities.correctionSeries(lstSeries, currentLibrary.getConnection());
                lstSeries.forEach((series) -> {
                    this.series.getItems().add(series);
                });
            }
            ArrayList<Genre> lstGenre = currentBook.getGenre();
            if (currentBook.isFresh() && !lstGenre.isEmpty()) {
                Utilities.correctionGenre(lstGenre, currentLibrary.getConnection());
            }
            if (!lstGenre.isEmpty()) {
                lstGenre.forEach((genre) -> {
                    genres.getItems().add(genre);
                });
            }
            if(image != null) {
                imageView.setImage(image);
            }
        } catch(SQLException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION, this.getClass().getName(), ex.getMessage());
        }
    }

    public void setNewBookInfo(Book book) {
        currentBook = book;
        setCurrentBookInfo(0);
    }

    public void setVisibleBtn() {
        btnSevenpence.setVisible(true);}

    public void setFlagsActions(FlagsActions flags) {flagsActions = flags;}

    public void setDlgStage(Stage stage) {
        dlgStage = stage;
    }

    public void setCurrentLibrary(Library lib) {
        this.currentLibrary = lib;
    }

    public void setImage(Image image) {this.image = image;}

    private void onAddAuthor() {
        try {
            FXMLLoader loader = new FXMLLoader(Alisa.class.getResource("ListAuthor.fxml"));
            AnchorPane choiceAuthor = loader.load();
            Stage dlgChoice = new Stage();
            dlgChoice.setTitle("Выбрать или добавить автора");
            dlgChoice.initModality(Modality.WINDOW_MODAL);
            dlgChoice.initOwner(Alisa.getPrimaryStage());
            Scene scene = new Scene(choiceAuthor);
            dlgChoice.setScene(scene);
            ListAuthorController controller = loader.getController();
            controller.processing(dlgChoice, currentLibrary, Constants.EMPTY_STRING);
            dlgChoice.showAndWait();
            Author author = controller.getAuthor();
            if(author != null) {
                authors.getItems().add(author);
            }
        } catch (IOException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION, this.getClass().getName(), ex.getMessage());
        }
    }

    private void onAddSeries() {
        int index = -1;
        try {
            FXMLLoader loader = new FXMLLoader(Alisa.class.getResource("ListSeries.fxml"));
            AnchorPane choiceSeries = loader.load();
            Stage dlgChoice = new Stage();
            dlgChoice.setTitle("Выбрать или добавить серия");
            dlgChoice.initModality(Modality.WINDOW_MODAL);
            dlgChoice.initOwner(Alisa.getPrimaryStage());
            Scene scene = new Scene(choiceSeries);
            dlgChoice.setScene(scene);
            ListSeriesController controller = loader.getController();
            controller.processing(dlgChoice, currentLibrary, Constants.EMPTY_STRING);
            dlgChoice.showAndWait();
            Series serie = controller.getSeries();
            if(serie != null) {
                TextInputDialog dialog = new TextInputDialog(Integer.toString(serie.getNum()));
                dialog.setTitle("Номер в серии");
                dialog.setHeaderText("Введите номер книги в серии");
                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()) {
                    try {
                        String sNum = result.get();
                        int number = Integer.parseInt(sNum);
                        serie.setNum(number);
                    } catch (NumberFormatException ex) {
                        serie.setNum(0);
                    }
                }
                series.getItems().add(serie);
            }
        } catch (IOException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION, this.getClass().getName(), ex.getMessage());
        }
    }

    private void onAddGenre() {
        try {
            FXMLLoader loader = new FXMLLoader(Alisa.class.getResource("ListGenre.fxml"));
            AnchorPane choiceGenre = loader.load();
            Stage dlgChoice = new Stage();
            dlgChoice.setTitle("Выбрать или добавить жанр");
            dlgChoice.initModality(Modality.WINDOW_MODAL);
            dlgChoice.initOwner(Alisa.getPrimaryStage());
            Scene scene = new Scene(choiceGenre);
            dlgChoice.setScene(scene);
            ListGenreController controller = loader.getController();
            controller.processing(dlgChoice, currentLibrary);
            dlgChoice.showAndWait();
            Genre genre = controller.getSelectedItem();
            if(genre != null) {
                genres.getItems().add(genre);
            }
        } catch (IOException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION, this.getClass().getName(), ex.getMessage());
        }
    }

    private void onForPath() {
        Author author = authors.getSelectionModel().getSelectedItem();
        if(author != null) {
            String name = author.getName();
            String path = currentLibrary.getPathToBooks().toString() + File.separator +
                    name.substring(0, 1).toUpperCase() +
                    File.separator +
                    name;
            folder.setText(path);
        }
    }

    private void onPrevFolder()  {
        if (Constants.PREV_FOLDER != null && !Constants.PREV_FOLDER.isEmpty())
            folder.setText(Constants.PREV_FOLDER);
    }

    private void onPrevSeries() {
        TextInputDialog dialog = new TextInputDialog(Integer.toString(Constants.PREV_SERIES.getNum()));
        dialog.setTitle("Номер в серии");
        dialog.setHeaderText("Введите номер книги в серии");
        Optional<String> num = dialog.showAndWait();
        if (num.isPresent()) {
            int number;
            try {
                number = Integer.parseInt(num.get());
            } catch (NumberFormatException ex) {
                number = 0;
            }
            Constants.PREV_SERIES.setNum(number);
            var prevSeries = new Series(Constants.PREV_SERIES);
            series.getItems().add(prevSeries);
        }
    }

    private void onPrevGenre() {
        if (Constants.PREV_GENRE != null) {
            var prevGenre = new Genre(Constants.PREV_GENRE);
            genres.getItems().add(prevGenre);
        }
    }

    private void onSeriesToPath() {
        Series serie = series.getSelectionModel().getSelectedItem();
        if (serie != null) {
            String path = folder.getText() + File.separator + serie.getName();
            folder.setText(path);
        }
    }

    private void onChangeFolder() {
        Path root = currentLibrary.getPathToBooks();
        Path path = directoryChooser(dlgStage, "Выбор каталога", root);
        if (path != null)
            folder.setText(path.toString());
    }

    private void onSeriesStore() {
        Series serial = series.getSelectionModel().getSelectedItem();
        if (serial != null)
            Constants.PREV_SERIES = new Series(serial);
    }

    private void onGenreStore() {
        Genre genre = genres.getSelectionModel().getSelectedItem();
        if (genre != null)
            Constants.PREV_GENRE = new Genre(genre);
    }

    private void onChangeNumSeries() {
        Series serial = series.getSelectionModel().getSelectedItem();
        if (serial != null) {
            var seriesNum = serial.getNum();
            TextInputDialog dialog = new TextInputDialog(Integer.toString(seriesNum));
            dialog.setTitle("Номер в серии");
            dialog.setHeaderText("Введите номер книги в серии");
            Optional<String> num = dialog.showAndWait();
            if (num.isPresent()) {
                int number;
                try {
                    number = Integer.parseInt(num.get());
                } catch (NumberFormatException ex) {
                    number = 0;
                }
                serial.setNum(number);
            }
        }
    }

    private void makeChange() {
        try {
            var copyBook = makeBookOfData();
            copyBook.setFresh(false);
            if (!checkBook(copyBook))
                return;
            if (!copyBook.getFile().equalsIgnoreCase(currentBook.getFile()))
                FileUtil.moveFile(currentLibrary, currentBook, copyBook);
            H2Operations.changeBookProperty(currentLibrary.getConnection(), copyBook, currentBook);
        } catch (SQLException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION,"Ошибка изменения параметров книги", ex.getMessage());
        }
        dlgStage.close();
    }

    private void writeNewBook() {
        var newBook = makeBookOfData();
        if (!checkBook(newBook))
            return;
        ArrayList<Integer> findBooks = null;
        try {
            findBooks = H2Operations.findBooks(currentLibrary.getConnection(), newBook);
        } catch (SQLException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION,"Ошибка поиска аналогичной книги.",
                    ex.getMessage());
            return;
        }
        if (!findBooks.isEmpty()) {
            try {
                FXMLLoader loader = new FXMLLoader(Alisa.class.getResource("FindBooks.fxml"));
                AnchorPane findInfo = loader.load();
                Stage dlgFind = new Stage();
                dlgFind.setTitle("Найденные книги");
                dlgFind.initModality(Modality.WINDOW_MODAL);
                dlgFind.initOwner(Alisa.getPrimaryStage());
                Scene scene = new Scene(findInfo);
                dlgFind.setScene(scene);
                FindBooksController controller = loader.getController();
                controller.setDlgStage(dlgFind);
                controller.setCurrentBook(newBook);
                controller.setCurrentLibrary(currentLibrary);
                controller.fillListBooks(findBooks);
                dlgFind.showAndWait();
            } catch (IOException ex) {
                Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
                Utilities.showMessage(Constants.TITLE_EXCEPTION, this.getClass().getName(), ex.getMessage());
            }
        } else {
            Path path;
            if (flagsActions.isNoCopy()) {
                path = currentBook.getPath();
            } else {
                path = FileUtil.appendNewBook(currentLibrary, newBook);
            }
            if (path != null) {
                try {
                    H2Operations.newBookID(currentLibrary.getConnection(), newBook);
                } catch (SQLException ex) {
                    Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
                    Utilities.showMessage(Constants.TITLE_EXCEPTION,"Ошибка добавления книги. Файл будет удален.",
                            ex.getMessage());
                    FileUtil.deleteFileAndFolder(currentLibrary, newBook);
                }
            }
        }
        dlgStage.close();
    }

    private Book makeBookOfData() {
        Book copyBook = new Book();
        copyBook.setTitle(title.getText().trim());
        copyBook.setId(currentBook.getId());
        copyBook.setEncoding(currentBook.getEncoding());
        copyBook.setCoverpage(currentBook.getCoverpage());
        copyBook.setFsize(currentBook.getFsize());
        copyBook.setPath(currentBook.getPath());
        String root = currentLibrary.getPathToBooks().toString();
        String sFolder = folder.getText().replace(root, "").trim();
        if (sFolder.startsWith("/") || sFolder.startsWith("\\"))
            sFolder = sFolder.substring(1);
        Folder cFolder = new Folder(0, sFolder);
        try {
            cFolder.setId(H2Operations.getFolderIdByName(currentLibrary.getConnection(), sFolder));
            if (cFolder.getId() < 1)
                cFolder.setId(H2Operations.newFolderID(currentLibrary.getConnection(), cFolder));
            copyBook.setFolder(cFolder);
            ObservableList<Author> lstAuthor = authors.getItems();
            lstAuthor.forEach(copyBook::addAuthor);
            ObservableList<Series> lstSeries = series.getItems();
            lstSeries.forEach(copyBook::addSeries);
            ObservableList<Genre> lstGenre = genres.getItems();
            lstGenre.forEach(copyBook::addGenre);
            var name = lstAuthor.getFirst().getName().split(Constants.SPACE_STRING)[0];
            StringBuilder copyFile = new StringBuilder(name + " - " + title.getText() + "." + typeBook.getText());
            if (currentBook.isFresh()) {
                var ext = FileUtil.getExtension(currentBook.getPath().toString()).toLowerCase();
                if (Arrays.asList(Constants.ZIPED_FILES).contains(typeBook.getText().toLowerCase()))
                    copyFile.append(".zip");
            } else {
                if (ZipFileUtil.isZipped(currentBook.getFile())) {
                    copyFile.append(".zip");
                }
            }
            copyBook.setFile(copyFile.toString());
        } catch (SQLException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION, "Ошибка изменения параметров книги", ex.getMessage());
        }
        return copyBook;
    }

    private boolean checkBook(Book book) {
        ArrayList<Author> authors1 = book.getAuthors();
        if (authors1.isEmpty()) {
            Utilities.showMessage("Error", "Список авторов", "Список пуст");
            return false;
        }
        ArrayList<Genre> genres1 = book.getGenre();
        if (genres1.isEmpty()) {
            Utilities.showMessage("Error", "Список жанров", "Список пуст");
            return false;
        }
        boolean nullGenre = false;
        for (Genre genre : genres1)
            if (genre.getId() == 0) {
                nullGenre = true;
                break;
            }
        if (nullGenre) {
            Utilities.showMessage("Error", "Список жанров", "Неопределенный жанр");
            return false;
        }
        return true;
    }
}
