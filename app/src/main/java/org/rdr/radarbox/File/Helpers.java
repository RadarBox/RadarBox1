package org.rdr.radarbox.File;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.nio.file.NotDirectoryException;
import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Класс-хранилище констант и функций для работы пакета File.
 */
public class Helpers {
    public static boolean autoRunReader = false;
    public static final String AoRD_FILES_DEFAULT_FOLDER_PATH = Environment.DIRECTORY_DOCUMENTS;
    public static final String CONFIG_FILE_NAME = "config.xml";
    public static final String DATA_FILE_NAME = "radar_data.data";
    public static final String STATUS_FILE_NAME = "status.csv";
    public static final String DESC_FILE_NAME = "description.txt";
    public static final String ADDITIONAL_FOLDER_NAME = "additional";
    public static final String ADDITIONAL_ARCH_NAME = ADDITIONAL_FOLDER_NAME + ".zip";

    /**
     * Проверка папки, в которую распакован AoRD-файл, на корректность содержимого.
     * @param aordFileUnzipFolder - объект {@link File} директории.
     * @throws NotDirectoryException - если передана не директория.
     * @throws WrongFileFormatException - если в формате AoRD допущена ошибка.
     */
    public static void checkAoRDFileFolder(File aordFileUnzipFolder)
            throws NotDirectoryException, WrongFileFormatException {
        if (!aordFileUnzipFolder.isDirectory()) {
            throw new NotDirectoryException("Файл " + aordFileUnzipFolder.getAbsolutePath() +
                    " не является директорией");
        }
        for (String fileName : new String[] {CONFIG_FILE_NAME, DATA_FILE_NAME,// STATUS_FILE_NAME,
                DESC_FILE_NAME, ADDITIONAL_FOLDER_NAME}) {
            if (!new File(aordFileUnzipFolder.getAbsolutePath() + "/" +
                    fileName).exists()) {
                throw new WrongFileFormatException(
                        "Некорректный формат AoRD-файла: не хватает файла (папки) " + fileName);
            }
        }
    }
    /**
     * Создание файла с уникальным именем.
     * @param start_name - изначальный путь.
     * @return файл с именем вида <Имя>[_<Номер (если файл уже есть)>]<Расширение>
     */
    public static File createUniqueFile(String start_name) {
        File file = new File(start_name);
        Integer i = 2;
        String[] nameAndExt = splitFIleName(start_name);
        String name = nameAndExt[0];
        String ext = nameAndExt[1];
        while (file.exists()) {
            if (file.isDirectory()) {
                file = new File(start_name + "__" + i.toString());
            } else {
                file = new File(name + "__" + i.toString() + "." + ext);
            }
            ++i;
        }
        return file;
    }

    /**
     * Разделение имени файла на само имя и расширение
     * (простой алгоритм, не работает со сложными случаями).
     * @param fileName - имя файла.
     * @return массив из 2 строк: имя и расширение.
     */
    public static String[] splitFIleName(String fileName) {
        int index = fileName.lastIndexOf(".");
        if (index == -1 || index < fileName.lastIndexOf("/")) {
            return new String[] {fileName, ""};
        }
        String name = fileName.substring(0, fileName.lastIndexOf("."));
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1);
        return new String[] {name, ext};
    }

    /**
     * Проверка файла на существование.
     * @param file - файл.
     * @throws FileNotFoundException - если файла не существует.
     */
    public static void checkFileExistence(File file) throws FileNotFoundException {
        if (!file.exists()) {
            throw new FileNotFoundException("No such file or directory: " +
                    file.getAbsolutePath());
        }
    }

    /**
     * Чтение текстового файла.
     * @param file - текстовый файл.
     * @return содержимое файла в одну строку.
     * @throws IOException - при ошибке системы ввода/вывода.
     */
    public static String readTextFile(File file) throws IOException {
        FileReader fileReader = new FileReader(file);
        Scanner reader = new Scanner(fileReader);
        String result = "";
        while (reader.hasNextLine()) {
            result += reader.nextLine() + "\n";
        }
        reader.close();
        fileReader.close();
        if (result.length() >= 1) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * Запись в текстовый файл.
     * @param file - текстовый файл.
     * @param text - строка, которую нужно записать.
     * @param append - если true, строка добавляется в конец файла, если false,
     *               то содержимое перезаписывается.
     * @throws IOException - при ошибке системы ввода/вывода.
     */
    public static void writeToTextFile(File file, String text, boolean append)
            throws IOException {
        FileWriter writer = new FileWriter(file, append);
        writer.write(text);
        writer.flush();
    }

    /**
     * Копирование файла.
     * @param source - файл, который нужно скопировать.
     * @param destination - файл, куда нужно скопировать.
     * @return true, если операция удалась, false в противном случае.
     */
    public static boolean copyFile(File source, File destination) {
        if (destination.exists() || !source.exists()) {
            return false;
        }
        try {
            FileInputStream fileInputStream = null;
            FileOutputStream fileOutputStream = null;
            try {
                fileInputStream = new FileInputStream(source);
                fileOutputStream = new FileOutputStream(destination);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fileInputStream.read(buffer)) > 0) {
                    fileOutputStream.write(buffer, 0, length);
                }
            } finally {
                try {fileInputStream.close();} catch (NullPointerException ignored) {}
                try {fileOutputStream.close();} catch (NullPointerException ignored) {}
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

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
        checkFileExistence(folder);
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
 * Класс исключения, означающего неверный формат файла.
 */
class WrongFileFormatException extends IOException {
    WrongFileFormatException() {}
    WrongFileFormatException(String message) {
        super(message);
    }
}
