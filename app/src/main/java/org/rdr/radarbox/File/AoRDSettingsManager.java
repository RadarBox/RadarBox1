package org.rdr.radarbox.File;

import android.os.Environment;

import androidx.preference.PreferenceManager;

import org.rdr.radarbox.RadarBox;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;

/**
 * Класс-надстройка над {@link AoRDFile} для работы с общими настройками AoRD-файлов приложения.
 * @author Шишмарев Ростислав Иванович
 * @version v1.0.1
 */
public class AoRDSettingsManager {
    private static final File defaultDirectory =
            RadarBox.getAppContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
    private static String fileNamePostfix = PreferenceManager.getDefaultSharedPreferences(
            RadarBox.getAppContext()).getString("file_writer_filename", "");

    /**
     * Включение/выключение записи сырых данных в файл.
     * Если true, каждое нажатие на "СТАРТ" будет создавать новый файл и сохрать все данные в него.
     * <br />Default: false;
     */
    public static boolean needSaveData = false;

    // Get methods
    /**
     * Получение AoRD-файла из файловой папки приложения по имени.
     * @param fileName - имя файла.
     * @return null, если {@link AoRDFile#isEnabled()} == false.
     */
    public static AoRDFile getFileByName(String fileName) {
        AoRDFile aordFile = new AoRDFile(defaultDirectory.getAbsolutePath() + "/" +
                fileName);
        if (aordFile.isEnabled()) {
            return aordFile;
        }
        RadarBox.logger.add("WARNING in AoRDSettingsManager.getFileByName(String): from filename "
                + fileName + " returned disable AoRDFile");
        return null;
    }

    /**
     * @return объект {@link File} папки для AoRD-файлов приложения.
     */
    public static File getDefaultDirectory() {
        return defaultDirectory;
    }

    /**
     * Получение списка всех файлов с расширением ".zip" в папке для AoRD-файлов приложения.
     * @return перечень файлов с расширением .zip, пустой список во всех остальных случаях.
     */
    public static String[] getFilesList() {
        String[] listOfFiles = defaultDirectory.list((d, s) -> s.toLowerCase().endsWith(".zip"));
        if (listOfFiles == null)
            listOfFiles = new String[]{};
        return listOfFiles;
    }

    /**
     * @return постфикс имён сохраняемых файлов.
     */
    public static String getFileNamePostfix() {
        return fileNamePostfix;
    }

    /**
     * @return true, если данные должны записываться, false в противном случае.
     */
    public static boolean isNeedSaveData() {
        return needSaveData;
    }

    // Set methods
    /**
     * Задаёт новый постфикс в имени всех последующих AoRD-файлов.
     * Имя файла будет представлять собой следующий формат: <дата>_<время>_<постфикс>.zip
     * @param postfix - постфикс в имени файла.
     */
    public static void setFileNamePostfix(String postfix) {
        fileNamePostfix = postfix;
    }

    /** Метод для включения/выключения записи сырых данных в файл.
     * @param value - если true, каждое нажатие на "СТАРТ" будет создавать новый файл и сохрать все
     *              данные в него.
     */
    public static void setNeedSaveData(boolean value) {
        needSaveData = value;
    }

    // Functional methods
    /**
     * Создание нового AoRD-файла с именем <дата>_<время>_<постфикс>.zip
     * @return - либо новый AoRD-файл (всегда enabled), либо null при ошибке в ходе создания.
     */
    public static AoRDFile createNewAoRDFile() {
        return AoRDFile.createNewAoRDFile(defaultDirectory.getAbsolutePath() + "/" +
                createFileName());
    }

    private static String createFileName() {
        String name = new Timestamp(System.currentTimeMillis()).toString();
        name = name.replace(' ', '_').replace('.',
                '-').replace(':', '-');
        return name + "_" + fileNamePostfix;
    }

    /**
     * Очищает папку для AoRD-файлов от директорий.
     */
    public static void cleanDefaultDir() {
        RadarBox.logger.add("DEBUG: Start cleaning");
        String[] listOfFiles = defaultDirectory.list();
        if (listOfFiles != null) {
            for (String name : listOfFiles) {
                File file = new File(defaultDirectory.getAbsolutePath() + "/" + name);
                if (file.isDirectory()) {
                    try {
                        Helpers.removeTree(file);
                        RadarBox.logger.add("DEBUG: Deleted " + file.getAbsolutePath());
                    } catch (IOException e) {
                        RadarBox.logger.add(e.toString());
                        e.printStackTrace();
                    }
                }
            }
        }
        RadarBox.logger.add("DEBUG: Cleaning ended");
    }
}
