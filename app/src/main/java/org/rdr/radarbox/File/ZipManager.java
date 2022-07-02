package org.rdr.radarbox.File;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.LinkedList;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Универсальный класс для работы с zip-архивами.
 * @author Шишмарев Ростислав Иванович
 * @version -
 */
public class ZipManager {
    private File mainZipFile = null;
    private File mainUnzippedFolder = null;

    // Initialize methods
    /**
     * Конструктор, принимающий объект {@link File}.
     * @param zipFile - объект {@link File} zip-архива.
     * @throws FileNotFoundException - если файл не найден;
     * @throws NoAZipFileException - если передан не zip-файл.
     */
    public ZipManager(File zipFile) throws FileNotFoundException, NoAZipFileException {
        checkFile(zipFile);
        mainZipFile = zipFile;
    }

    /**
     * Конструктор, принимающий путь до файла.
     * @param absolutePath - абсолютный путь до zip-архива.
     * @throws FileNotFoundException - если файл не найден;
     * @throws NoAZipFileException - если передан путь не к zip-файлу.
     */
    public ZipManager(String absolutePath) throws FileNotFoundException, NoAZipFileException {
        File file = new File(absolutePath);
        checkFile(file);
        mainZipFile = file;
    }

    private void checkFile(File file) throws FileNotFoundException, NoAZipFileException {
        if (!file.exists()) {
            throw new FileNotFoundException("No such file or directory: " + file.getAbsolutePath());
        }
        if (!file.getName().endsWith(".zip") || !file.isFile()) {
            throw new NoAZipFileException("File " + file.getName() + " is not a zip-archive");
        }
    }

    // Get methods
    /** Возвращает zip-файл, с которым работает ZipManager.
     * @return null, если работа завершена.
     */
    public File getZipFile() {
        return mainZipFile;
    }

    /** Возвращает объект {@link File} папки, в которую был распакован архив.
     * @return null, если работа завершена или файл ещё не был распакрван.
     */
    public File getUnzipFolder() {
        return mainUnzippedFolder;
    }

    // Set methods
    /**
     * Смена файла, с которым работает ZipManager.
     * @param zipFile - новый zip-архив.
     * @throws FileNotFoundException - если файл не найден;
     * @throws NoAZipFileException - если передан не zip-файл.
     */
    public void setZipFile(File zipFile) throws FileNotFoundException, NoAZipFileException {
        checkFile(zipFile);
        mainZipFile = zipFile;
        mainUnzippedFolder = null;
    }

    /**
     * Смена файла, с которым работает ZipManager.
     * @param absolutePath - абсолютный путь до zip-архива.
     * @throws FileNotFoundException - если файл не найден;
     * @throws NoAZipFileException - если передан путь не к zip-файлу.
     */
    public void setZipFile(String absolutePath) throws FileNotFoundException, NoAZipFileException {
        File zipFile = new File(absolutePath);
        checkFile(zipFile);
        mainZipFile = zipFile;
        mainUnzippedFolder = null;
    }

    // <Main methods>
    // Unzip methods
    /**
     * Рекурсивно распаковывает файлы zip-архива, переданного в конструктор,
     * в папку <Имя архива (без .zip)>_unzipped. Директории в архиве игнорирует.
     * <b>Если такая папка уже существует, она будет удалена</b>.
     * @throws IOException
     */
    public void unzipFile() throws IOException {
        mainUnzippedFolder = new File(mainZipFile.getParent() + "/" +
                getUnzippedFolderName(mainZipFile));
        removeTreeIfExists(mainUnzippedFolder);
        unzipFileRecursive(mainZipFile);
    }

    /**
     * Скрытая реалиэация метода {@link #unzipFile()}.
     * @param zipFile - zip-файл, который нужно распаковать.
     * @throws IOException
     */
    private void unzipFileRecursive(File zipFile) throws IOException {
        String folderName = getUnzippedFolderName(zipFile);
        String parentPath = zipFile.getParent();
        File unzippedFolder = new File(parentPath + "/" + folderName);
        unzippedFolder.mkdir();

        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile));
        BufferedInputStream in = new BufferedInputStream(zipInputStream);
        ZipEntry entry;
        LinkedList<File> filesToUnzip = new LinkedList<File>();
        while((entry = zipInputStream.getNextEntry())!=null){
            String entryName = entry.getName();
            File entryFile = new File(unzippedFolder.getAbsolutePath() + "/" + entryName);
            if (entryFile.isDirectory()) {
                continue;
            }
            if (entryFile.getName().endsWith(".zip")) {
                filesToUnzip.add(entryFile);
            }

            FileOutputStream fileOutputStream = new FileOutputStream(entryFile.getAbsolutePath());
            BufferedOutputStream out = new BufferedOutputStream(fileOutputStream);
            byte b[] = new byte[1024];
            int n;
            while ((n = in.read(b,0,1024)) >= 0) {
                out.write(b, 0, n);
            }
            zipInputStream.closeEntry();
            out.close();
            fileOutputStream.close();
        }
        in.close();
        zipInputStream.close();

        for (File fileToUnzip : filesToUnzip) {
            unzipFileRecursive(fileToUnzip);
        }
    }

    private String getUnzippedFolderName(File zipFile) {
        return zipFile.getName().substring(0, zipFile.getName().lastIndexOf('.')) + "_unzipped";
    }

    /**
     * Эавершение работы ZipManager (в том числе удаление папки, в которую был распакован архив).
     */
    public void close() {
        close(true);
    }

    /**
     * Эавершение работы ZipManager.
     * @param deleteUnzippedFolder - false, если папку, в которую был распакован архив,
     *                             не надо удалять, true в противном случае.
     */
    public void close(boolean deleteUnzippedFolder) {
        if(deleteUnzippedFolder) {
            removeTreeIfExists(mainUnzippedFolder);
        }
        mainZipFile = null;
        mainUnzippedFolder = null;
    }

    // Archive methods (Isn`t ready)
    public static void archiveFolder(File folderToBeArchived)
            throws IOException, FileNotFoundException {
        if (!folderToBeArchived.exists()) {
            throw new FileNotFoundException("No such file or directory: " +
                    folderToBeArchived.getAbsolutePath());
        }
        archiveFolderRecursive(folderToBeArchived);
    }

    private static void archiveFolderRecursive(File folder) throws IOException {}
    // </ Main methods>

    // Help methods
    /**
     * Удаление папки со всем её содержимым (при её наличии).
     * @param folder - папка.
     * @return true, если директория существовала, false в противном случае.
     */
    public static boolean removeTreeIfExists(File folder) {
        try {
            removeTree(folder);
        } catch (FileNotFoundException e) {
            return false;
        }
        return true;
    }

    /**
     * Удаление папки со всем её содержимым.
     * @param folder - папка.
     * @throws FileNotFoundException - если директория не найдена.
     */
    public static void removeTree(File folder) throws FileNotFoundException {
        if (!folder.exists()) {
            throw new FileNotFoundException("No such file or directory: " +
                    folder.getAbsolutePath());
        }
        File[] contents = folder.listFiles();
        if (contents == null){
            return;
        }
        for (File file : contents) {
            if (file.isFile()) {
                file.delete();
            } else {
                removeTree(file);
            }
        }
        folder.delete();
    }
}

/**
 * Класс исключения, выбрасываемого, если файл не является zip-архивом.
 */
class NoAZipFileException extends Exception {
    NoAZipFileException() {}
    NoAZipFileException(String message) {
        super(message);
    }
}
