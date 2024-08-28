package valeriy.bikmetov.alisa.utilites;

import valeriy.bikmetov.alisa.model.Genre;
import valeriy.bikmetov.alisa.model.Series;

import java.nio.file.Path;
import java.util.ArrayList;

public final class Constants {
    // Статические не настраиваемы поля

    public static final String EMPTY_STRING = "";
    public static final String SPACE_STRING = " ";
    public static final String TITLE_EXCEPTION = "Excption";
    public static final String DJVU_BOOK = "djvu";
    public static final String PDF_BOOK = "pdf";
    public static final String FB2_BOOK = "fb2";
    public static final String EPUB_BOOK = "epub";
    public static final String AUTO_SERVER_DB = ";AUTO_SERVER=TRUE";
    public static final String PREFIX_URL_DB = "jdbc:h2:";
    public static final String NAME_DRIVER_DB = "org.h2.Driver";
    public static final String PREFIX_DB = "mylib";
    public static final int WIDTH_IMAGE = 200;
    public static final int HEIGHT_IMAGE = 314;
    public static final int LENGTH_TEXT_FIELD = 32;
    public static final String APP_PROPERTY = "app.properties";
    public enum TABS {TAB_AUTHOR, TAB_SERIES, TAB_GENRE};
    public static final String[] ZIPED_FILES = {"doc", "docx", "fb2", "rtf", "txt"};
    public static final String REMOVE_IT = "[\\/:*?\"<>«»|'`]";


    // Настраиваемые поля
    public static String SUFFIX_DB = ".mv.db";
    public static String TEMPLATE_DB = "mylib01.mv.db";
    public static String FOLDER_DB = "D:/Library/data";
    public static String READ_FB2 = "F:/Library/AllReader/alleader.exe";
    public static String READ_DJVU = "F:/Library/StdView/stdview.exe";
    public static String READ_PDF = "F:/Library/StdView/stdview.exe";
    public static String TEMP_DIR = "D:/temp";
    public static String ADMIN_DB = "sa";
    public static String PASSWORD_DB = EMPTY_STRING;
    public static String PREV_FOLDER = null;
    public static Series PREV_SERIES = null;
    public static Genre PREV_GENRE = null;
    public static Path CURRENT_PATH = null;
    private Constants() {}
}
