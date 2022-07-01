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

public class ZipManager {
    private File mainZipFile = null;
    private File mainUnzippedFolder = null;

    // Initialize methods
    ZipManager(File fileRead) throws NoAZipFileException {
        checkFile(fileRead);
        mainZipFile = fileRead;
    }

    ZipManager(String absolutePath) throws FileNotFoundException, NoAZipFileException {
        if (Files.exists(Paths.get(absolutePath))) {
            throw new FileNotFoundException("No such file or directory: " + absolutePath);
        }
        File file = new File(absolutePath);
        checkFile(file);
        mainZipFile = file;
    }

    private void checkFile(File file) throws NoAZipFileException {
        if (!file.getName().endsWith(".zip")) {
            throw new NoAZipFileException("File " + file.getName() + " is not a zip-archive");
        }
    }

    // Unzip methods
    public void unzipFile() throws IOException {
        mainUnzippedFolder = new File(mainZipFile.getParent() + "/" +
                getUnzippedFolderName(mainZipFile));
        unzipFileRecursive(mainZipFile);
    }

    private void unzipFileRecursive(File zipFile) throws IOException {
        String folderName = getUnzippedFolderName(zipFile);
        String parentName = zipFile.getParent();
        File unzippedFolder = new File(parentName + "/" + folderName);
        removeTreeIfExists(unzippedFolder);
        unzippedFolder.mkdir();

        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile));
        BufferedInputStream in = new BufferedInputStream(zipInputStream);
        ZipEntry entry;
        LinkedList<File> filesToUnzip = new LinkedList<File>();
        while((entry = zipInputStream.getNextEntry())!=null){
            String entryName = entry.getName();
            File entryFile = new File(unzippedFolder.getAbsolutePath() + "/" + entryName);
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
            fileToUnzip.delete();
        }
    }

    private String getUnzippedFolderName(File zipFile) {
        return zipFile.getName().substring(0, zipFile.getName().lastIndexOf('.')) + "_unzipped";
    }

    // Archive methods
    public static void archiveFolder(File folderToBeArchived) throws IOException {}

    // Get methods
    File getZipFile() {
        return mainZipFile;
    }

    File getUnzipFolder() {
        return mainUnzippedFolder;
    }

    // Help methods
    public static void removeTree(File folder) {
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

    public static void removeTreeIfExists(File folder) {
        if (Files.exists(Paths.get(folder.getAbsolutePath()))) {
            removeTree(folder);
        }
    }
}

class NoAZipFileException extends Exception {
    NoAZipFileException() {}
    NoAZipFileException(String message) {
        super(message);
    }
}
