package valeriy.bikmetov.alisa.utilites;

import valeriy.bikmetov.alisa.Alisa;
import javafx.scene.image.Image;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.logging.Level;

import static valeriy.bikmetov.alisa.utilites.ZipFileUtil.getZipEnv;
import static valeriy.bikmetov.alisa.utilites.ZipFileUtil.getZipFSProvider;
import static javax.xml.stream.XMLStreamConstants.*;

/**
 *
 * @author Валерий Бикметов
 */
public class CoverEPUB {
    private final FileSystemProvider zipProvider;
    private Map<String, String> env = null;
    private String annotation;
    private Image cover;

    public CoverEPUB() {
        zipProvider = getZipFSProvider();
        if(zipProvider != null) {
            env = getZipEnv(false);
        } else {
            Utilities.showMessage("Error", "Работа с файловой системой", "Zip FS не поддерживается!");
        }
    }

    public Image getImage() {
        return cover;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void extractCoverAndAnnotation(Path path, String strCover) {
        if(zipProvider == null) {
            return;
        }
        try {
            try (FileSystem zfs = zipProvider.newFileSystem(path, env)) {
                Path pth = zfs.getPath("META-INF/container.xml");
                String full_path = getFullPath(pth);
                if (full_path == null) {
                    return;
                }
                pth = zfs.getPath(full_path);
                annotation = getAnnotation(pth);
                String root;
                if (strCover != null && !strCover.isEmpty()) {
                    int i = full_path.indexOf('/');
                    if(i > 0) {
                        root = full_path.substring(0, i);
                        pth = zfs.getPath(root, strCover);
                    } else {
                        pth = zfs.getPath(strCover);
                    }
                    InputStream inStream = Files.newInputStream(pth);
                    cover = new Image(inStream, (double)Constants.WIDTH_IMAGE, (double)Constants.HEIGHT_IMAGE, true, true);
                }
            }
        } catch (XMLStreamException | IOException ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
        }
    }

    private String getFullPath(Path path) throws FileNotFoundException, XMLStreamException, IOException {
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

    private String getAnnotation(Path path) throws IOException, XMLStreamException {
        StringBuilder result = null;
        boolean inDescription = false;
        try (InputStream inStream = Files.newInputStream(path)) {
            XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(inStream);
            int event;
            while(xmlReader.hasNext()) {
                event = xmlReader.next();
                if(event == START_ELEMENT && xmlReader.getLocalName().equals("description")) {
                    inDescription = true;
                } else if(event == CHARACTERS && inDescription) {
                    if(result == null) {
                        result = new StringBuilder(xmlReader.getText());
                    } else {
                        result.append(Constants.SPACE_STRING).append(xmlReader.getText());
                    }
                } else if(event == END_ELEMENT && xmlReader.getLocalName().equals("description")) {
                    inDescription = false;
                }
            }
            xmlReader.close();
        }
        assert result != null;
        return result.toString();
    }
}
