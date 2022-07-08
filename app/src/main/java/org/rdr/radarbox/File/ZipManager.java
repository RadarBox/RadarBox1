package org.rdr.radarbox.File;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Paths;

import java.util.LinkedList;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Универсальный класс для работы с zip-архивами.
 * @author Шишмарев Ростислав Иванович
 * @version v1.0.1
 */
public class ZipManager {
    private File mainZipFile = null;
    private File mainUnzippedFolder = null;

    // Initialize methods
    /**
     * Конструктор, принимающий объект {@link File}.
     * @param zipFile - объект {@link File} zip-архива.
     * @throws FileNotFoundException - если файл не найден;
     * @throws WrongFileFormatException - если передан не zip-файл.
     */
    public ZipManager(File zipFile) throws FileNotFoundException, WrongFileFormatException {
        checkFile(zipFile);
        mainZipFile = zipFile;
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
     * @throws WrongFileFormatException - если передан не zip-файл.
     */
    public void setZipFile(File zipFile) throws FileNotFoundException, WrongFileFormatException {
        checkFile(zipFile);
        mainZipFile = zipFile;
        mainUnzippedFolder = null;
    }

    // <Main methods>
    // Unzip methods
    /**
     * Рекурсивно распаковывает файлы zip-архива, переданного в конструктор,
     * в папку <Имя архива (без .zip)>[_<Номер (если папка уже есть)>].
     * <b>Если такая папка уже существует, она будет удалена</b>.
     * Директории в архиве игнорирует.
     * @throws IOException - при ошибке системы ввода/вывода.
     */
    public void unzipFile() throws IOException {
        mainUnzippedFolder = new File(mainZipFile.getParent() + "/" +
                getUnzippedFolderName(mainZipFile));
        Helpers.removeTreeIfExists(mainUnzippedFolder);
        unzipFileRecursive(mainZipFile);
    }

    /**
     * Скрытая реалиэация метода {@link #unzipFile()}.
     * @param zipFile - zip-файл, который нужно распаковать.
     * @throws IOException - при ошибке системы ввода/вывода.
     */
    private void unzipFileRecursive(File zipFile) throws IOException {
        String folderName = getUnzippedFolderName(zipFile);
        String parentPath = zipFile.getParent();
        File unzippedFolder = Helpers.createUniqueFile(parentPath + "/" + folderName);
        if (!unzippedFolder.mkdir()) {
            throw new IOException("Error on creation directory for unzip");
        };

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
        return zipFile.getName().substring(0, zipFile.getName().lastIndexOf('.'));
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
            Helpers.removeTreeIfExists(mainUnzippedFolder);
        }
        mainZipFile = null;
        mainUnzippedFolder = null;
    }

    // Archive methods
    /**
     * Рекурсивно архивирует директорию в файл <Имя папки>[_<Номер (если папка уже есть)>].zip
     * (все вложенные папки тоже архивируются).
     * @param folderToBeArchived - объект {@link File} директории, которую надо архивировать.
     * @throws IOException - при ошибке системы ввода/вывода.
     * @return объект {@link File} zip-архива.
     */
    public static File archiveFolder(File folderToBeArchived)
            throws IOException, NotDirectoryException, FileNotFoundException {
        if (!folderToBeArchived.exists()) {
            throw new FileNotFoundException("No such file or directory: " +
                    folderToBeArchived.getAbsolutePath());
        }
        if (!folderToBeArchived.isDirectory()) {
            throw new NotDirectoryException("Path " + folderToBeArchived.getAbsolutePath() +
                    " is not a path to directory");
        }
        return archiveFolderRecursive(folderToBeArchived);
    }

    /**
     * Скрытая реалиэация метода {@link #archiveFolder(File)}.
     * @param folder - объект {@link File} директории, которую надо архивировать.
     * @throws IOException - при ошибке системы ввода/вывода.
     * @return объект {@link File} zip-архива.
     */
    private static File archiveFolderRecursive(File folder) throws IOException {
        String[] listOfFiles = folder.list();
        File zipFile = Helpers.createUniqueFile(folder.getAbsolutePath() + ".zip");
        FileOutputStream fileOutputStream = new FileOutputStream(zipFile);
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);

        LinkedList<File> filesToArchive = new LinkedList<File>();
        if (listOfFiles != null) {
            for (String fileName : listOfFiles) {
                File file = new File(folder.getAbsolutePath() + "/" + fileName);
                if (file.isDirectory()) {
                    filesToArchive.add(archiveFolderRecursive(file));
                    continue;
                }
                addEntryToZip(zipOutputStream, file);
            }
        }
        for (File file : filesToArchive) {
            addEntryToZip(zipOutputStream, file);
            file.delete();
        }
        zipOutputStream.close();
        fileOutputStream.close();
        return zipFile;
    }

    private static void addEntryToZip(ZipOutputStream zipOutputStream, File file)
            throws IOException {
        ZipEntry entry = new ZipEntry(file.getName());
        zipOutputStream.putNextEntry(entry);
        byte[] data = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        zipOutputStream.write(data, 0, data.length);
        zipOutputStream.closeEntry();
    }

    // Help methods
    private void checkFile(File file) throws FileNotFoundException, WrongFileFormatException {
        Helpers.checkFileExistence(file);
        if (!file.getName().endsWith(".zip") || !file.isFile()) {
            throw new WrongFileFormatException("File " + file.getName() + " is not a zip-archive");
        }
    }
}
