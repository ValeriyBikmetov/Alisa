package valeriy.bikmetov.alisa.utilites;

import valeriy.bikmetov.alisa.model.Author;
import valeriy.bikmetov.alisa.model.Book;
import valeriy.bikmetov.alisa.model.Genre;
import valeriy.bikmetov.alisa.model.Series;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.StringTokenizer;

/**
 *
 * @author Валерий Бикметов
 */
class ParserFB2 {
    public static final String DELIMITERS = "<>/\t\n\r";
    private String encoding;
    private final Book book = new Book();
    private boolean isTitleinfo = false;
    private boolean isGenre = false;
    private boolean isAuthor = false;
    private boolean isFirst = false;
    private boolean isMiddle = false;
    private boolean isLast = false;
    private boolean isTitle = false;
    private boolean isCover = false;
    private String lastName;
    private String firstName;

    public void parse(Path path) {
        try {
            parseFB2(path);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     *
     * @param path путь к файлу
     */
    private void parseFB2(Path path) throws IOException
    {
        String workString;
        book.setPath(path);
        try(FileChannel fChannel = (FileChannel) Files.newByteChannel(path)) {
            long fSize = fChannel.size();
            book.setFsize((int)fSize);
            MappedByteBuffer mBuf = fChannel.map(FileChannel.MapMode.READ_ONLY, 0, fSize);
            setEncoding(mBuf);
            book.setEncoding(encoding);
            workString = getWorkingString(mBuf);
        }
        if(workString != null) {
            StringTokenizer st = new StringTokenizer(workString, DELIMITERS);
            while(st.hasMoreTokens()) {
                String str = st.nextToken().trim();
                if(!str.isEmpty()) {
                    choiceElement(str);
                }
            }
        }
    }

    private String getWorkingString(MappedByteBuffer mBuff) {
        String search = "</description";
        int begin = 8192;
        int delta = 0;
        int end = -1;
        while(end < 0) {
            int length = begin + delta;
            byte[] array = new byte[length];
            mBuff.position(0);
            mBuff.get(array, 0, length);
            String str = new String(array, Charset.forName(encoding));
            end = str.lastIndexOf(search);
            if(end > 0) {
                begin = str.indexOf(search.substring(3));
                return str.substring(begin + 12, end);
            } else {
                delta += 1024;
            }
        }
        return null;
    }

    private void choiceElement(String str) {
        switch(str) {
            case "title-info": isTitleinfo = !isTitleinfo;
                break;
            case "genre": isGenre = !isGenre;
                break;
            case "author": if(isTitleinfo) {
                isAuthor =!isAuthor;
                if(!isAuthor) {
                    var name = (lastName + " " + firstName.trim());
                    Author author = new Author(name, 0);
                    book.addAuthor(author);
                } else {
                    lastName = "";
                    firstName = "";
                }
            }
                break;
            case "first-name": isFirst = !isFirst;
                break;
            case "middle-name": isMiddle = !isMiddle;
                break;
            case "last-name": isLast = !isLast;
                break;
            case "book-title": isTitle = !isTitle;
                break;
            case "coverpage": isCover = !isCover;
                break;
            default: getTextElement(str);
        }
    }

    private void getTextElement(String str) {
        if(isGenre && isTitleinfo) {
            Genre genre = new Genre(0, str, "", "");
            book.addGenre(genre);
        } else if(isFirst && isTitleinfo) {
            firstName = str + " ";
        } else if(isMiddle && isTitleinfo) {
            firstName += str;
        } else if(isLast && isTitleinfo) {
            lastName = str;
        } else if(isTitle && isTitleinfo) {
            book.setTitle(str);
        } else if(isCover && isTitleinfo) {
            getCoverPage(str);
        } else if(str.startsWith("sequence")) {
            getSequence(str);
        }
    }

    private String getParam(String str, String name) {
        String result = null;
        int index = str.indexOf(name);
        if(index > 0) {
            int start = str.indexOf('"', index + 2);
            int end = str.indexOf('"', start + 2);
            if (end > start)
                return str.substring(start + 1, end).trim();
            else return null;
        }
        return result;
    }

    private void getCoverPage(String str) {
        String cover = getParam(str, "href");
        if(cover != null && !cover.isEmpty()) {
            if(cover.startsWith("#")){
                cover = cover.replaceAll("#", "");
            }
            book.setCoverpage(cover);
        }
    }

    private void getSequence(String str) {
        Series series;
        String name = getParam(str, "name");
        String sNum = getParam(str, "number");
        int num = 0;
        if(sNum != null && !sNum.isEmpty()) {
            try {
                num = Integer.parseInt(sNum);
            } catch(NumberFormatException ignored) {
            }
        }
        if(name != null && !name.isEmpty()) {
            series = new Series(name, num);
            book.addSeries(series);
        }
    }

    public Book getBook() {
        return book;
    }

    private void setEncoding(MappedByteBuffer bBuff) {
        byte[] array = new byte[256];
        if (bBuff.get(0) == 0b1111 && bBuff.get(1) == 0b1110) {
            encoding = "UTF-16LE";
        } else if (bBuff.get(0) == 0b1110 && bBuff.get(1) == 0b1111) {
            encoding = "UTF-16BE";
        } else {
            bBuff.position(0);
            bBuff.get(array, 0, 256);
            String str = new String(array);
            int pos = str.indexOf("encoding");
            if(pos > -1) {
                pos = str.indexOf('=', pos);
                pos = str.indexOf('\"', pos);
                int pos1 = str.indexOf('\"', pos + 1);
                encoding = str.substring(pos + 1, pos1);
            }
        }
    }
}
