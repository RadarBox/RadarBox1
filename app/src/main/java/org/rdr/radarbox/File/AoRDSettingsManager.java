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
 * @version v1.0.0
 */
public class AoRDSettingsManager {
    private static final File defaultDirectory =
            RadarBox.getAppContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
    private static String fileNamePostfix = PreferenceManager.getDefaultSharedPreferences(
            RadarBox.getAppContext()).getString("file_writer_filename", "");

    public static boolean needSaveData = false;

    /**
     * Получение AoRD-файла из файловой папки приложения по имени.
     * @param fileName - имя файла.
     * @return
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

    public static File getFolder() {
        return defaultDirectory;
    }

    /** Получение списка всех файлов в директории с расширением .zip
     * @return перечень файлов с расширением .zip, пустой список во всех остальных случаях
     */
    public static String[] getFilesList() {
        String[] listOfFiles = defaultDirectory.list((d, s) -> s.toLowerCase().endsWith(".zip"));
        if (listOfFiles == null)
            listOfFiles = new String[]{};
        return listOfFiles;
    }

    public static String getFileNamePostfix() {
        return fileNamePostfix;
    }

    public static void setFileNamePostfix(String postfix) {
        fileNamePostfix = postfix;
    }

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

    public static void cleanDefaultDir() {
        for (String name : getFilesList()) {
            File file = new File(defaultDirectory.getAbsolutePath() + "/" + name);
            if (file.isDirectory()) {
                try {
                    Helpers.removeTree(file);
                } catch (IOException e) {
                    RadarBox.logger.add(e.toString());
                    e.printStackTrace();
                }
            }
        }
    }
}
