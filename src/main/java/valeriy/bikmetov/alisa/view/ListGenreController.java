package valeriy.bikmetov.alisa.view;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import valeriy.bikmetov.alisa.Alisa;
import valeriy.bikmetov.alisa.model.Genre;
import valeriy.bikmetov.alisa.model.Item;
import valeriy.bikmetov.alisa.model.Library;
import valeriy.bikmetov.alisa.utilites.Constants;
import valeriy.bikmetov.alisa.utilites.H2Operations;
import valeriy.bikmetov.alisa.utilites.Utilities;
import valeriy.bikmetov.alisa.utilites.BookUtil;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.sql.*;
import java.util.HashMap;
import java.util.logging.Level;

/**
 *
 * @author valer
 */
public class ListGenreController {
    private Stage dlgStage;
    private Library currentLibrary;
    private int currentIndex = 0;
    private Genre currentGenre;
    private HashMap<Integer, Integer> mapItems; // ID and position

    @FXML
    private TreeView<Item> tree;
    @FXML
    private Button okBtn;
    @FXML
    private Button cancelBtn;
    @FXML
    private MenuItem mnuAdd;
    @FXML
    private MenuItem mnuDel;
    @FXML
    private MenuItem mnuChange;
    @FXML
    AnchorPane anchorPane;

    @FXML
    public void initialize() {
        okBtn.setOnAction(event -> {
            currentGenre = getSelectedItem();
            dlgStage.close();
        });
        cancelBtn.setOnAction(event -> {
            currentGenre = null;
            dlgStage.close();
        });
        mnuAdd.setOnAction(event -> onAddGenre());
        mnuDel.setOnAction(event -> onDelGenre());
        mnuChange.setOnAction(event -> onChangeGenre());
    }

    public void processing(Stage stage, Library library) {
        this.dlgStage = stage;
        this.currentLibrary = library;
        this.tree = makeTree();
    }

    /**
     * Для формирования дерева жанров выбираем сначала наименования всех групп
     * жанров - это будут ветви (узлы) дерева. Затем для каждой группы выбираем
     * нименования жанра на русском и его идентификатор.
     */
    private TreeView<Item> makeTree() {
        ObservableList<Node> children = anchorPane.getChildren();
        mapItems = new HashMap<>();
        int position = 1;
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
                ++position;
                String meta = rs.getString(1);
                Item itemMeta = new Item(branch, meta);
                // TODO добавить картинки для узлоа дерева(типа папок)
                TreeItem<Item> branchMeta = new TreeItem<>(itemMeta);
                pstm.setString(1, meta);
                try (ResultSet r = pstm.executeQuery()) {
                    while(r.next()) {
                        ++position;
                        int id = r.getInt(1);
                        String genre = r.getString(2);
                        Item itemLeaf = new Item(id, genre);
                        TreeItem<Item> leaf = new TreeItem<>(itemLeaf);
                        branchMeta.getChildren().add(leaf);
                        mapItems.put(id, position);
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
                currentGenre = getSelectedItem();
            }
        });
        children.add(treeView);
        return treeView;
    }

    public Genre getSelectedItem() {
        Item item = null;
        Genre g = null;
        tree.getSelectionModel().select(currentIndex);
        TreeItem<Item> treeItem = tree.getSelectionModel().getSelectedItem();
        if (treeItem.isLeaf()) {
            item = tree.getSelectionModel().getSelectedItem().getValue();
            try {
                g = H2Operations.makeGenre(currentLibrary.getConnection(), item.getId());
            } catch (SQLException ex) {
                Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
                Utilities.showMessage(Constants.TITLE_EXCEPTION, this.getClass().getName(), ex.getMessage());
                g = null;
            }
        }
        return g;
    }

    private void onAddGenre() {
        NewGenreDlg dialog = new NewGenreDlg(null);
        Genre item = dialog.getResult(currentLibrary);
        if(item != null) {
            tree = null;
            tree = makeTree();
            findGenre(item.getId());
        }
    }

    private void onDelGenre() {
        BookUtil.deleteBooksOfGenre(currentLibrary, currentGenre.getId());
    }

    private void onChangeGenre() {
        NewGenreDlg dialog = new NewGenreDlg(currentGenre);
        Genre item = dialog.getResult(currentLibrary);
        if(item != null) {
            tree = null;
            tree = makeTree();
            findGenre(item.getId());
        }
    }

    private void findGenre(int id) {
        if(mapItems.containsKey(id)) {
            int position = mapItems.get(id);
            tree.getSelectionModel().select(position);
            currentIndex = position;
            currentGenre = getSelectedItem();
        }
    }
}