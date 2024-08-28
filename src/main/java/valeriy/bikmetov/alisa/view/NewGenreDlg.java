package valeriy.bikmetov.alisa.view;

import valeriy.bikmetov.alisa.Alisa;
import valeriy.bikmetov.alisa.model.Genre;
import valeriy.bikmetov.alisa.model.Library;
import valeriy.bikmetov.alisa.utilites.Constants;
import valeriy.bikmetov.alisa.utilites.H2Operations;
import valeriy.bikmetov.alisa.utilites.Utilities;
import javafx.scene.control.*;
import javafx.util.Callback;

import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;

public class NewGenreDlg {
    private Dialog<Genre> dialog;
    private Genre current;

    public NewGenreDlg(Genre genre) {
        this.current = genre;
        if(genre != null) {
            dialog.setTitle("Изменить параметры жанра");
            dialog.setHeaderText("Внесите изменения и нажмите кнопку 'Записать'");
        } else {
            dialog.setTitle("Новый жанр");
            dialog.setHeaderText("Введите параметры и нажмите кнопку 'Создать'");
        }
        Label lblCode = new Label("Код жанра");
        Label lblDesc = new Label("Наименование ");
        Label lblMeta = new Label("Группа");
        TextField txtCode = new TextField();
        txtCode.setPrefColumnCount(25);
        TextField txtDesc = new TextField();
        txtDesc.setPrefColumnCount(25);
        TextField txtMeta = new TextField();
        txtMeta.setPrefColumnCount(25);
        if(genre != null) {
            txtCode.setText(genre.getGenre());
            txtDesc.setText(genre.getDescription());
            txtMeta.setText(genre.getGroup());
        }
        ButtonType btnOk;
        if(current != null) {
            btnOk = new ButtonType("Записать", ButtonBar.ButtonData.OK_DONE);
        } else {
            btnOk = new ButtonType("Создать", ButtonBar.ButtonData.OK_DONE);
        }
        ButtonType btnCancel = new ButtonType("Закрыть", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnOk, btnCancel);

        dialog.setResultConverter(new Callback<ButtonType, Genre>() {
            @Override
            public Genre call(ButtonType param) {
                if(param == btnOk) {
                    String code = txtCode.getText().trim();
                    String desc = txtDesc.getText().trim();
                    String meta = txtMeta.getText().trim();
                    if(current != null) {
                        current.setGenre(code);
                        current.setDescription(desc);
                        current.setGroup(meta);
                        return current;
                    } else {
                        Genre genre1 = new Genre();
                        genre1.setGroup(code);
                        genre1.setDescription(desc);
                        genre1.setGroup(meta);
                        return genre1;
                    }
                }
                return null;
            }
        });
    }

    public Genre getResult(Library library) {
        Optional<Genre> result = dialog.showAndWait();
        Genre genre = null;
        if(result.isPresent()) {
            genre = result.get();
            try {
                if(genre.getId() == 0) {
                    int id = H2Operations.getGenreID(library.getConnection(), genre.getGenre());
                    if(id > 0) {
                        Utilities.showMessage("Error", "Дубликат жанра", "Такой жанр уже существует");
                        return null;
                    } else {
                        id = H2Operations.newGenreID(library.getConnection(), genre);
                        genre.setId(id);
                    }
                } else {
                    H2Operations.changeGenre(library.getConnection(), genre);
                }
            }catch(SQLException ex) {
                Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
                Utilities.showMessage(Constants.TITLE_EXCEPTION, this.getClass().getName(), ex.getMessage());
                return null;
            }
        }
        return genre;
    }
}

