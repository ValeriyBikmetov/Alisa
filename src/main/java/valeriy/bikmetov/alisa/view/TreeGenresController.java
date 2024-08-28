package valeriy.bikmetov.alisa.view;

import javafx.scene.Node;
import valeriy.bikmetov.alisa.Alisa;
import valeriy.bikmetov.alisa.model.IObservable;
import valeriy.bikmetov.alisa.model.IObserver;
import valeriy.bikmetov.alisa.model.Item;
import valeriy.bikmetov.alisa.model.Library;
import valeriy.bikmetov.alisa.utilites.Constants;
import valeriy.bikmetov.alisa.utilites.H2Operations;
import valeriy.bikmetov.alisa.utilites.Utilities;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Screen;

import java.sql.*;
import java.util.logging.Level;

/**
 * FXML Controller class
 * Для формирования дерева жанров выбираем сначала наименования всех групп
 * жанров - это будут ветви (узлы) дерева. Затем для каждой группы выбираем
 * нименования жанра на русском и его идентификатор.
 * @author Валерий Бикметов
 */
public class TreeGenresController implements IObserver {
    //private final ArrayList<IObserver> observers = new ArrayList<>();
    private Constants.TABS tabIndex;
    private Library currentLibrary;
    private int currentIndex = 0;
    private TreeView tree;
    private MainWinController mainView;

    @FXML
    private AnchorPane anchorPane;

    @FXML
    public void initialize() {
    }

    public void setTabIndex(Constants.TABS index) {
        this.tabIndex = index;
    }

    public Constants.TABS getTabIndex() {
        return this.tabIndex;
    }

    @Override
    public void refreshData(IObservable subject, Object object) {
        if(object instanceof Library && !object.equals(currentLibrary)){
            // Меняем текущую библиотеку
            currentLibrary = (Library) object;
            tree = makeTree();
        }
    }

    /**
     * Для формирования дерева жанров выбираем сначала наименования всех групп
     * жанров - это будут ветви (узлы) дерева. Затем для каждой группы выбираем
     * нименования жанра на русском и его идентификатор.
     */
    private TreeView makeTree() {
        ObservableList<Node> children = anchorPane.getChildren();
        if(!children.isEmpty()) {
            children.clear();
        }
        Item rootItem = new Item(0, "Жанры");
        // TODO добавить картинки для узлоа дерева(типа папок)
        TreeItem<Item> root = new TreeItem<>(rootItem);
        root.setExpanded(true);
        int branch = 0;
        Connection conn = currentLibrary.getConnection();
        try(Statement stm = conn.createStatement();
            ResultSet rs = stm.executeQuery(H2Operations.getQuery(conn, "QUERY_GENREMETA"));
            PreparedStatement pstm = conn.prepareStatement(H2Operations.getQuery(conn, "QUERY_GENREDESC"))) {
            while(rs.next()) {
                ++branch;
                String meta = rs.getString(1);
                Item itemMeta = new Item(branch, meta);
                // TODO добавить картинки для узлоа дерева(типа папок)
                TreeItem<Item> branchMeta = new TreeItem<>(itemMeta);
                pstm.setString(1, meta);
                try (ResultSet r = pstm.executeQuery()) {
                    while(r.next()) {
                        int id = r.getInt(1);
                        String genre = r.getString(2);
                        Item itemLeaf = new Item(id, genre);
                        TreeItem<Item> leaf = new TreeItem<>(itemLeaf);
                        branchMeta.getChildren().add(leaf);
                    }
                }
                if(!branchMeta.getChildren().isEmpty()) {
                    root.getChildren().add(branchMeta);
                }
            }
        } catch (SQLException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION, this.getClass().getName(), ex.getMessage());
        }
        TreeView<Item> treeView = new TreeView<>(root);
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        treeView.setPrefHeight(primaryScreenBounds.getHeight() - 200);
        treeView.setPrefWidth(300);
        treeView.getSelectionModel().selectedItemProperty().addListener((ov, old_item, new_item) -> {
            if(new_item != null && new_item.isLeaf()) {
                Item item = new_item.getValue();
                currentIndex = treeView.getSelectionModel().getSelectedIndex();
                mainView.getCurrentItem().setValue(item);
            }
        });
        children.add(treeView);
        return treeView;
    }

    public void getSelectedItem() {
        Item item = null;
        tree.getSelectionModel().select(currentIndex);
        TreeItem treeItem = (TreeItem) tree.getSelectionModel().getSelectedItem();
        if (treeItem.isLeaf()) {
            item = (Item) ((TreeItem<?>) tree.getSelectionModel().getSelectedItem()).getValue();
        }
        if (item != null) {
            mainView.getCurrentItem().setValue(item);
        }
    }

    public void setMainView(MainWinController mainView) {
        this.mainView = mainView;
    }
}

