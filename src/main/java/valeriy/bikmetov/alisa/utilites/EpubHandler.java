package valeriy.bikmetov.alisa.utilites;

import valeriy.bikmetov.alisa.model.Book;
import valeriy.bikmetov.alisa.model.Author;
import valeriy.bikmetov.alisa.model.Series;
import valeriy.bikmetov.alisa.model.Genre;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 *
 * @author Валерий Бикметов
 */
public class EpubHandler {
    private final Book book;
    private String cover;
    private String seriesName;
    private int seriesNum = 0;
    private String title;
    private String file_as;
    private boolean inTitle, inSubject, inCover, inAuthor;

    public EpubHandler(Book book){
        this.book = book;
    }

    public void processElement(XMLStreamReader element) throws XMLStreamException {
        String localName = element.getLocalName();
        switch(localName) {
            case "title"    : inTitle = true;
                break;
            case "creator"  : getAuthorParam(element);
                inAuthor = true;
                break;
            case "subject"  : inSubject = true;
                break;
            case "meta"     : execMeta(element);
                break;
            case "item"     : getPathCover(element);
                break;
        }
    }

    private void getAuthorParam(XMLStreamReader element) {
        int count = element.getAttributeCount();
        for(int i = 0; i < count; i++) {
            String name = element.getAttributeLocalName(i);
            String value = element.getAttributeValue(i);
            if(name.equals("role") && !value.equals("aut")){
                return;
            }
            if(name.equals("file-as")) {
                file_as = value;
            }
        }
    }

    private void getAuthor(String text) {
        String first = null;
        String last = null;
        if(file_as != null && file_as.indexOf(',') > 0) {
            int index = file_as.indexOf(',');
            last = file_as.substring(0, index).trim();
            if(index < file_as.length()){
                first = file_as.substring(index + 1).trim();
            }
            index = text.indexOf(last);
            if(index >= 0) {
                String s = text.replaceAll(last, Constants.EMPTY_STRING).trim();
                if(s.length() > first.length()){
                    first = s;
                }
            }
        } else {
            int index = text.lastIndexOf(' ');
            if(index > 0) {
                last = text.substring(index).trim();
                first = text.substring(0, index).trim();
            }
        }
        if(last != null){
            String name = last + " " + first;
            Author author = new Author(name, 0);
            book.addAuthor(author);
        }
    }

    public void processText(String text) {
        if(inTitle) {
            if(title != null && !title.isEmpty()){
                title += Constants.SPACE_STRING + text;
            } else {
                title = text;
            }
            inTitle = false;
        } else if(inSubject) {
            Genre genre = new Genre(0, text, Constants.EMPTY_STRING, Constants.EMPTY_STRING);
            book.addGenre(genre);
            inSubject = false;
        } else if(inAuthor) {
            getAuthor(text);
            inAuthor = false;
        }
    }

    public void finishElement(String name) {
        switch(name) {
            case "metadata": book.setTitle(title); break;
            case "manifest": book.setCoverpage(cover); break;
        }
    }

    private void execMeta(XMLStreamReader element) {
        int count = element.getAttributeCount();
        OUTER:
        for (int i = 0; i < count; i++) {
            String name = element.getAttributeLocalName(i);
            String value = element.getAttributeValue(i);
            if (name.equals("name")) {
                switch (value) {
                    case "series":
                        addSeries();
                        getSeries(element, true);
                        break OUTER;
                    case "series_index":
                        getSeries(element, false);
                        break OUTER;
                    case "cover":
                        getCoverID(element);
                        break OUTER;
                    default:
                        break;
                }
            }
        }
    }

    private void getCoverID(XMLStreamReader element) {
        int count = element.getAttributeCount();
        for(int i = 0; i < count; i++) {
            String name = element.getAttributeLocalName(i);
            String value = element.getAttributeValue(i);
            if(name.equals("content")) {
                cover = value;
            }
        }
    }

    private void getPathCover(XMLStreamReader element) {
        if(inCover){
            return;
        }
        boolean isCover = false;
        int count = element.getAttributeCount();
        for(int i = 0; i < count; i++) {
            String name = element.getAttributeLocalName(i);
            String value = element.getAttributeValue(i);
            if(name.equals("id") && value.equals(cover)) {
                isCover = true;
                continue;
            }
            if(name.equals("href") && isCover) {
                cover = value;
                inCover = true;
            }
        }
    }

    private void getSeries(XMLStreamReader element, boolean serName) {
        int count = element.getAttributeCount();
        for(int i = 0; i < count; i++) {
            String name = element.getAttributeLocalName(i);
            String value = element.getAttributeValue(i);
            if(name.equals("content")) {
                if(serName){
                    seriesName = value;
                    break;
                } else {
                    try {
                        seriesNum = Integer.parseInt(value);
                    }catch(NumberFormatException ex) {
                        seriesNum = 0;
                    }
                    break;
                }
            }
        }
    }

    private void addSeries() {
        if(seriesName != null){
            Series series = new Series(seriesName, seriesNum);
            book.addSeries(series);
            seriesName = null;
            seriesNum = 0;
        }
    }
}

