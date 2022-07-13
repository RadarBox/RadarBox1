package org.rdr.radarbox.File;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import org.rdr.radarbox.RadarBox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.Scanner;

/**
 * Класс-хранилище констант и функций для работы пакета File.
 */
public class Helpers {
    public static final int PERMISSION_STORAGE = 101;

    public static void requestPermissions(Activity activity, int requestCode) {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.addCategory("android.intent.category.DEFAULT");
            intent.setData(Uri.parse(String.format("package:%s", activity.getPackageName())));
            activity.startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            activity.startActivityForResult(intent, requestCode);
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
            RadarBox.logger.add(e.toString());
            e.printStackTrace();
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
