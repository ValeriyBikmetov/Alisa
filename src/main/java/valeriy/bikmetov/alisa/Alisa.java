package valeriy.bikmetov.alisa;

import valeriy.bikmetov.alisa.model.Library;
import valeriy.bikmetov.alisa.utilites.Constants;
import valeriy.bikmetov.alisa.utilites.FileUtil;
import valeriy.bikmetov.alisa.view.ListItemsController;
import valeriy.bikmetov.alisa.view.MainWinController;
import valeriy.bikmetov.alisa.view.RootLayoutController;
import valeriy.bikmetov.alisa.view.TreeGenresController;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.logging.*;


/**
 * Загружаем основные элементы главного окна приложения и регистрируем связи
 * передачи сообщений между ними.
 * RootLayout регистрирует в качестве слушателей MainView и ListXXX.
 * К ним от него идет сигнал о смене библиотеке (источник RootLayout,
 * тело сообщения Library) и закрытии приложения (источник RootLayout,
 * тело сообщения null).
 * MainView должна по этим сигналам сделать следующее:
 * 1. Очистить таблицу TableView(не удаляя колнки), закрыть в ней все открытые
 * PreparedStatement и, если в теле сообщения null, ничего больше с TableView
 * не делать. Если в теле сообщения Library - вновь подготовить все необходимые
 * PreparedStatement.
 * 2. Очистить ImageView и TextArea.
 * 3. Закрыть все свои PreparedStatement и если в теле сообщения Library
 * открыть их вновь в новом соединении.
 * ListXXX по получении этих сигналов:
 * 1. Очищают свои списки.
 * 2. Закрывают PreparedStatement и, если тело сгнала не null, вновь открывают
 * их в новом соединении.
 * MainView регистрирует ListXXX в качестве слушателя сигнала о смене закладки.
 * Если вкладка открывается, то если список не пуст и есть выделенный элемент,
 * деелаем скроллин по списку до ранее выделенного элемента и выделяем его
 * (соответственно долно уйти сообщение о выбранном элементе списка),
 * если вкладка закрывается, то передаем сообщение с нулевым значением
 * идентификатора Item и пустрой строкой Item.name
 * ListXXX регистрируют MainView в качестве наблюдателя и посылают ему сигнал
 * при смене "владельца" книги (источник ListXXX, тело ChangeOwnerBook,
 * которое в свою очередь содержит индекс закладки и Item, содержащая номер
 * "владельца" в базе данных и его имя).
 * MainView извлекает из сообщения индекс закладки и сравнивает его с текущим.
 * Если они равны передает номер "владельца" в TableView для формирования списка книг.
 * Если индексы закладок не равны то:
 * 1. Очищает таблицу с удалением колонок и закрытием в ней PreparedStatement.
 * 2. Очищает ImageView и TextArea.
 * 3. Вставляет в таблицу новые колонки в соответствии с индексом закладки
 * и открывает PrparedStatement.
 * 4. Передает номер "владельца" в TableView для формирования списка книг.
 * В MainView определяем IntegerProperty и связывем его с номером выбранной
 * книги. Назначаем этому свойству слушателя, который при изменении этого
 * свойства формирует информацию о книге и ее обложке и выводит ее в
 * соответствущие окна.
 * @author Валерий Бикметов
 */
public class Alisa extends Application {
    public static final Logger MYLOG = Logger.getLogger(Alisa.class.getName());
    private static Stage primaryStage;
    private BorderPane rootLayout;
    private RootLayoutController rootLayoutController;
    private MainWinController mainViewController;

    @Override
    public void start(Stage primaryStage) {
        try {
            MYLOG.setLevel(Level.INFO);
            MYLOG.setUseParentHandlers(false);
            Handler consolHandler = new ConsoleHandler();
            consolHandler.setLevel(Level.INFO);
            MYLOG.addHandler(consolHandler);
            final int LOG_ROTATION_COUNT = 10;
            final int LOG_LIMIT = 1000000;
            Path path = Path.of("./Alisa.log");
            Handler fhandler = new FileHandler(path.toString(), LOG_LIMIT, LOG_ROTATION_COUNT, true);
            fhandler.setLevel(Level.INFO);
            MYLOG.addHandler(fhandler);
        } catch (IOException | SecurityException ex) {
            MYLOG.log(Level.INFO, ex.getMessage(), ex);
        }
        Alisa.primaryStage = primaryStage;
        Alisa.primaryStage.setTitle("Библиотека электронных книг Алиса");
        Alisa.primaryStage.getIcons().add(new Image(String.valueOf(Alisa.class.getResource("images/cat.png"))));

        initRootLayout();
        showMainWin();
        rootLayoutController.startApp();
        Runnable r = () -> {
            try {
                FileUtil.deleteFromDir(Constants.TEMP_DIR);
            } catch (IOException ex) {
                Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            }
        };
        Thread t = new Thread(r);
        t.start();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    private void initRootLayout() {
        try {
            FXMLLoader loader = new FXMLLoader(Alisa.class.getResource("RootLayout.fxml"));
            rootLayout = loader.load();
            rootLayoutController = loader.getController();
            //set Stage boundaries to visible bounds of the main screen
            Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
            primaryStage.setX(primaryScreenBounds.getMinX());
            primaryStage.setY(primaryScreenBounds.getMinY());
            primaryStage.setWidth(primaryScreenBounds.getWidth());
            primaryStage.setHeight(primaryScreenBounds.getHeight());

            Scene scene = new Scene(rootLayout);
            primaryStage.setScene(scene);

            primaryStage.show();

        } catch (IOException ex) {
            MYLOG.log(Level.INFO, ex.getMessage(), ex);
        }
    }

    /**
     * Загружаем главное окно программы. Регистрируем главное окно как
     * наблюдателя  RootLayout (следим за сменой библиотек)
     */
    private void showMainWin() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Alisa.class.getResource("MainWin.fxml"));
            AnchorPane mainView = loader.load();
            rootLayout.setCenter(mainView);
            mainViewController = loader.getController();
            TabPane tabPane = mainViewController.getTabPane();
            fillTabPane(tabPane);
            mainViewController.initalize();
            mainViewController.setPrimaryStage(primaryStage);
            rootLayoutController.register(mainViewController);
        } catch(IOException ex) {
            MYLOG.log(Level.INFO, ex.getMessage(), ex);
        }
    }

    /**
     * Заполняем TabPane соответствующими списками авторов, серий, жанров.
     * Регистрируем списки и дерево жанров в качестве наблюдателя за RootLayout,
     * чтобы следить за сменой библиотек
     */
    private void fillTabPane(TabPane tabPane) throws IOException {
        ObservableList<Tab> tabs = tabPane.getTabs();
        for(int i = 0; i < tabs.size(); i++) {
            if(i < 2) {
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(Alisa.class.getResource("ListItems.fxml"));
                AnchorPane item = loader.load();
                ListItemsController controller = loader.getController();
                if(i == 0) {
                    tabs.get(i).setText("Авторы");
                    controller.setTabIndex(Constants.TABS.TAB_AUTHOR);
                    // Запоминаем ссылку на этот список в MainView
                    mainViewController.setListAuthor(controller);
                    // Запоминаем ссылку на MainView в этом списке
                    controller.setMainView(mainViewController);
                } else {
                    tabs.get(i).setText("Серии");
                    controller.setTabIndex(Constants.TABS.TAB_SERIES);
                    // Запоминаем ссылку на этот список в MainView
                    mainViewController.setListSeries(controller);
                    // Запоминаем ссыдку на MainView в этом списке
                    controller.setMainView(mainViewController);
                }
                tabs.get(i).setContent(item);
                // Регистрируем список как наблюдателя за RootLayout
                rootLayoutController.register(controller);
            } else if(i == 2){
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(Alisa.class.getResource("TreeGenres.fxml"));
                AnchorPane item = loader.load();
                TreeGenresController controller = loader.getController();
                controller.setTabIndex(Constants.TABS.TAB_GENRE);
                tabs.get(i).setText("Жанры");
                tabs.get(i).setContent(item);
                // Регистрируем дерево жанров ка наблюдателя за RootLayout
                rootLayoutController.register(controller);
                // Запоминаем ссылку на дерево в MainView
                mainViewController.setTreeGenres(controller);
                // Запоминаем ссыдку на MainView
                controller.setMainView(mainViewController);
            }
        }
    }

    /**
     * Возвращаем ссылку на главные подмостки.
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Послать сообщение с null. Это означает, что мы выключаемся и надо
     * закрыть все PreparedStatment и при необходимости переслать null
     * в качестве текущей библиотеки далее.
     * Затем закрыть все открытые соединения библиотек.
     */
    @Override
    public void stop() throws SQLException {
        rootLayoutController.notifyObservers(null);
        ObservableList<Library> libraries = rootLayoutController.getLibraries().getItems();
        for(Library lib : libraries) {
            lib.getConnection().close();
        }
        Runnable r = () -> {
            try {
                FileUtil.deleteFromDir(Constants.TEMP_DIR);
            } catch (IOException ex) {
                Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            }
        };
        Thread t = new Thread(r);
        t.start();
    }
}