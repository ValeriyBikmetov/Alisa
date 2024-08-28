package valeriy.bikmetov.alisa.utilites;

import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.StringTokenizer;

/**
 *
 * @author Валерий Бикметов
 */
public class CoverFB2 {
    private Image coverPage = null;
    private String annotation = null;
    private long channelSize;
    //private int currentPos;

    public CoverFB2() {
    }

    public String getAnnotation() {
        if(annotation != null && !annotation.isEmpty()){
            return annotation;
        } else {
            return null;
        }
    }

    public Image getImage() {
        return coverPage;
    }

    public void extractCoverAndAnnotation(Path path, String encoding, String coverpage) throws IOException{
        if (path == null)
            return;
        StandardOpenOption openOption;
        if(path.startsWith(Path.of(Constants.TEMP_DIR))) {
            openOption = StandardOpenOption.DELETE_ON_CLOSE;
        } else {
            openOption = StandardOpenOption.READ;
        }
        String sCover = null;
        try(FileChannel fChannel = (FileChannel) Files.newByteChannel(path, openOption)) {
            channelSize = fChannel.size();
            MappedByteBuffer mBuf = fChannel.map(FileChannel.MapMode.READ_ONLY, 0, channelSize);
            getAnnotation(mBuf, encoding);
            if(coverpage != null && !coverpage.isEmpty()) {
                sCover = getCoverString(mBuf, coverpage, encoding);
            }
        }
        if(sCover != null && !sCover.isEmpty()) {
            getCoverImage(sCover);
        }
    }

    private String getCoverString(MappedByteBuffer mBuf, String cover, String encoding) {
        int length = (int)channelSize;
        byte[] array = new byte[length];
        String str = getStringFromBuff(mBuf, 0, array, length, encoding);
        int body = str.indexOf("<body") + 512;
        int begin = str.indexOf(cover, body);
        int end = str.indexOf("</binary>", begin);
        if(begin >= 0 && end > begin) {
            begin = str.indexOf(">", begin + cover.length());
            return str.substring(begin + 1, end);
        } else {
            return null;
        }
    }

    private void getAnnotation(MappedByteBuffer mBuf, String encodieng) {
        final String search = "annotation";
        String work = getWorkingString(mBuf, encodieng);
        if(work != null) {
            int begin = work.indexOf(search);
            if(begin >= 0){
                int end = work.indexOf(search, begin + 10);
                if(end > 0 && end > begin) {
                    StringBuilder sb = new StringBuilder();
                    String annot = work.substring(begin + 10, end);
                    StringTokenizer st = new StringTokenizer(annot, ParserFB2.DELIMITERS);
                    while(st.hasMoreTokens()) {
                        String str = st.nextToken().trim();
                        if(!str.isEmpty() && !str.equalsIgnoreCase("p")) {
                            sb.append(str);
                            sb.append("\n");
                        }
                    }
                    annotation = sb.toString().replaceAll("(<.*?>)", " ");
                }
            }
        }
    }

    private void getCoverImage(String sCover) {
        byte[] bytes;
        Base64.Decoder decoder = Base64.getMimeDecoder();
        bytes = decoder.decode(sCover);
        ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
        coverPage = new Image(bin, Constants.WIDTH_IMAGE, Constants.HEIGHT_IMAGE, true, true);
    }

    private String getWorkingString(MappedByteBuffer mBuff, String encodieng) {
        String search = "</description";
        int begin = 8192;
        int delta = 0;
        int end = -1;
        while(end < 0) {
            int length = begin + delta;
            byte[] array = new byte[length];
            String str = getStringFromBuff(mBuff, 0, array, length, encodieng);
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

    private String getStringFromBuff(MappedByteBuffer mBuf, int position, byte[] array,
                                     int length, String encoding) {
        mBuf.position(position);
        mBuf.get(array, 0, length);
        return new String(array, Charset.forName(encoding));
    }
}
