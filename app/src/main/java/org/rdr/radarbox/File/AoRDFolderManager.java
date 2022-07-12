package org.rdr.radarbox.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import org.rdr.radarbox.R;
import org.rdr.radarbox.RadarBox;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;

public class AoRDFolderManager {
    private static final File defaultDirectory =
            RadarBox.getAppContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
    private static String fileNamePostfix = "";
    public static boolean needSaveData = false;

    public static AoRDFile getFileByName(String fileName) {
        return new AoRDFile(defaultDirectory.getAbsolutePath() + "/" + fileName);
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
}
