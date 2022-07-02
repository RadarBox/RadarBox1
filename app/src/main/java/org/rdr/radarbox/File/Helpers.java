package org.rdr.radarbox.File;

import android.os.Environment;

import java.util.Map;
import java.util.HashMap;

/**
 * Класс-хранилище констант для работы пакета File.
 */
public class Helpers {
    static final String defaultFolderPath = Environment.DIRECTORY_DOCUMENTS;
    static final Map<String,String> fileNamesMap = makeFileNamesMap();

    private static Map<String,String> makeFileNamesMap() {
        Map<String,String> map = new HashMap<String,String>();
        map.put("config", "config.xml");
        map.put("data", "radar_data.data");
        map.put("status", "status.csv");
        map.put("additional_folder", "additional_unzipped");
        map.put("additional_zip", "additional.zip");
        return map;
    }
}
