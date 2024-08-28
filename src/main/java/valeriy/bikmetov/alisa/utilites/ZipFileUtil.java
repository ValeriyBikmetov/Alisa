package valeriy.bikmetov.alisa.utilites;

import valeriy.bikmetov.alisa.Alisa;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class ZipFileUtil {

    private ZipFileUtil() {}

    /**
     * Проверяем доступен ли в системе FileSystemProvider для работы с zip-файлами
     * @return - FileSystemProvider для работы с zip-файлами
     */
    public static FileSystemProvider getZipFSProvider() {
        for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
            if ("jar".equals(provider.getScheme())) {
                return provider;
            }
        }
        return null;
    }

    /**
     * Создаем окружение для работы с zip-файлами
     * @param create - Если true, пишем zip-файл, иначе читаем
     * @return HashMap - с параметрами
     */
    public static Map<String, String> getZipEnv(boolean create) {
        Map<String, String> zipEnv = new HashMap<>();
        if(create) {
            zipEnv.put("create", "true");
        } else {
            zipEnv.put("create", "false");
        }
        zipEnv.put("encoding", "CP866");
        return zipEnv;
    }

    /**
     * Проверяем зазипованный ли это файл
     * @param filename - путь к файлу
     * @return true - если файл зазипован
     */
    public static boolean isZipped(String filename) {
        return filename.endsWith(".zip");
    }

    /**
     * Копируем разархивированный файл во временный каталог отделя файлы со старой кодировкой от новой
     * @return возвращаем путь к файлу во временном каталоге
     */
    public static Path copyZipFileToDir(Path source, String dir) {
        try {
            return extractZipFile(source, dir, "cp866"); //copyOldZipFile(source, dir);
        } catch (Exception ex) {
            try {
                return extractZipFile(source, dir, "utf8");
            } catch (Exception e) {
                Alisa.MYLOG.log(Level.INFO, ex.getMessage(), e);
                Utilities.showMessage(Constants.TITLE_EXCEPTION,"Ошибка разархивации файла", e.getMessage());
                return  null;
            }
        }
    }

    public static Path writeZipFile (Path sourceFile, String distFolder, String filename) {
         String distFile = distFolder + File.separator + filename + ".zip";
        try (ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(distFile));
             FileInputStream fis= new FileInputStream(sourceFile.toString())) {
            ZipEntry entry = new ZipEntry(filename);
            zout.putNextEntry(entry);
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            zout.write(buffer);
            zout.closeEntry();
        } catch(Exception ex) {
            Alisa.MYLOG.log(Level.INFO, ex.getMessage(), ex);
            Utilities.showMessage(Constants.TITLE_EXCEPTION,"Ошибка архивирования файла", ex.getMessage());
            return  null;
        }
        return Path.of(distFile);
    }

    public  static Path extractZipFile(Path pathZip, String dir, String coding) throws IOException {
        Path pathOutput;
        try (var fis = new ZipFile(pathZip.toString(), Charset.forName(coding))) {
            var entries = fis.entries();
            var entry = entries.nextElement();
            String name;
            if (entry != null)
                name = entry.getName();
            else throw new RuntimeException("Файл не  содержит архива");
            pathOutput = Path.of(dir, name);
            if (Files.exists(pathOutput))
                return pathOutput;
            try (var is = fis.getInputStream(entry); var bis = new BufferedInputStream(is)) {
                try (var os = new FileOutputStream(pathOutput.toFile()); var bos = new BufferedOutputStream(os)) {
                    while (bis.available() > 0) {
                        bos.write(bis.read());
                    }
                }
            }
        }
        return pathOutput;
    }
}
