package org.rdr.radarbox.File;

import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Helpers {
    static final String defaultFolderPath = Environment.DIRECTORY_DOCUMENTS;
    static final String defaultFolderAbsPath =
            "storage/emulated/0/Android/data/org.rdr.radarbox/files/Documents";
}
