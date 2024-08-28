package valeriy.bikmetov.alisa.utilites;

import valeriy.bikmetov.alisa.model.Book;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;

import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.CHARACTERS;

/**
 *
 * @author Валерий Бикметов
 */
public class ParserEpub {
    private Book book = null;
    private final FileSystemProvider zipProvider;
    private Map<String, String> env = null;

    public ParserEpub() {
        zipProvider = ZipFileUtil.getZipFSProvider();
        if(zipProvider != null) {
            book = new Book();
            book.setEncoding("UTF-8");
            env = ZipFileUtil.getZipEnv(false);
        } else {
            Utilities.showMessage("Error", "Работа с файловой системой", "Zip FS не поддерживается!");
        }
    }

    public Book getBook() {
        return book;
    }

    public void parseEpubBook(Path path) throws XMLStreamException, IOException {
        if(book == null) {
            return;
        }
        book.setPath(path);
        book.setFsize((int) Files.size(path));
        try(FileSystem zfs = zipProvider.newFileSystem(path, env)){
            Path pth = zfs.getPath("META-INF/container.xml");
            String full_path = getFullPath(pth);
            if(full_path == null) {
                return;
            }
            pth = zfs.getPath(full_path);
            EpubHandler handler = new EpubHandler(book);
            try (InputStream inStream = Files.newInputStream(pth)) {
                XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(inStream);
                int event;
                while(xmlReader.hasNext()) {
                    event = xmlReader.next();
                    switch(event) {
                        case START_ELEMENT: handler.processElement(xmlReader);break;
                        case CHARACTERS: handler.processText(xmlReader.getText()); break;
                        case END_ELEMENT: handler.finishElement(xmlReader.getLocalName()); break;
                    }
                }
                xmlReader.close();
            }
        }
    }

    private String getFullPath(Path path) throws IOException, XMLStreamException, IOException {
        String result = null;
        try (InputStream inStream = Files.newInputStream(path)) {
            XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(inStream);
            int event;
            while(xmlReader.hasNext()) {
                event = xmlReader.next();
                if(event == START_ELEMENT && xmlReader.getLocalName().equals("rootfile")) {
                    int count = xmlReader.getAttributeCount();
                    for(int i = 0; i < count; i++) {
                        String name = xmlReader.getAttributeLocalName(i);
                        if(name.equals("full-path")) {
                            result = xmlReader.getAttributeValue(i);
                        }
                    }
                }
            }
            xmlReader.close();
        }
        return result;
    }

   /* public void parse(Path path) {
        try {
            parseEpubBook(path);
        } catch (XMLStreamException | IOException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
        }
    } */
}