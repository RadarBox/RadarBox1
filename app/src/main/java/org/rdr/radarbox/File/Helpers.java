package org.rdr.radarbox.File;

import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.HashMap;

/**
 * Класс-хранилище констант и функций для работы пакета File.
 */
public class Helpers {
    public static boolean autoRunReader = false;
    static final String defaultUserFilesFolderPath = Environment.DIRECTORY_DOCUMENTS;
    static final Map<String,String> fileNamesMap = makeFileNamesMap();

    private static Map<String,String> makeFileNamesMap() {
        Map<String,String> map = new HashMap<String,String>();
        map.put("config", "config.xml");
        map.put("data", "radar_data.data");
        map.put("status", "status.csv");
        map.put("description", "description.txt");
        map.put("additional_folder", "additional_unzipped");
        map.put("additional_zip", "additional.zip");
        return map;
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
