package org.rdr.radarbox.File;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import android.util.Xml;
import android.widget.EditText;

import org.rdr.radarbox.R;
import org.xmlpull.v1.XmlSerializer;

import org.rdr.radarbox.RadarBox;
import org.rdr.radarbox.Device.DeviceConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import java.io.IOException;

import java.sql.Timestamp;
import java.util.ArrayList;

/**
 * Класс для записи данных и создания AoRD-файла (архива данных радара).
 * @author Сапронов Данил Игоревич; Шишмарев Ростислав Иванович
 * @version 0.3.1
 */
public class Writer {
    Context context;
    private File aordFile = null;
    private File folderWrite = null;
    private File defaultDirectory;
    private FileOutputStream dataWriteStream = null;

    private String dataWriteFilenamePostfix;
    private boolean needSaveData = false;

    // Initialize methods
    public Writer(Context context_) {
        context = context_;
        defaultDirectory = context.getExternalFilesDir(Helpers.AoRD_FILES_DEFAULT_FOLDER_PATH);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        dataWriteFilenamePostfix = pref.getString("file_writer_filename","");
        // Устанавливать пункт "Сохранять данные" в false при запуске программы
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("need_save", needSaveData);
        editor.apply();
    }

    // Get methods
    /**
     * Возвращает последний записанный архив (файл {@link File}).
     * @return null, если архивы ещё не были созданы.
     */
    public File getFileWrite() {return aordFile;}

    /**
     * @return true, если данные должны записываться, false в противном случае.
     */
    public boolean isNeedSaveData() {return needSaveData;}

    public boolean isWritingToFile() {
        return dataWriteStream != null && folderWrite != null;
    }

    // Set methods
    /** Задаёт новый постфикс в имени всех последующих файлов архивов.
     * Имя архива будет представлять собой следующий формат: <дата>_<время>_<постфикс>.zip
     * @param postfix - постфикс в имени файла
     */
    public void setDataWriteFilenamePostfix (String postfix) {
        dataWriteFilenamePostfix = postfix;
    }

    /** Метод для включения/выключения записи сырых данных в файл.
     * @param value - если true, каждое нажатие на "СТАРТ" будет создавать новый файл и сохрать все
     *              данные в него.
     */
    public void setNeedSaveData(boolean value) {
        needSaveData = value;
    }

    // <Main methods>
    /** Создание папки с файлами для дальнейшей архивации:
     *   <br />- Файл конфигурации (.xml) -- описание текущих настроек радара и всей необходимой
     *   информации для правильной интерпретации данных в будущем
     *   <br />- Файл данных (.data) (пустой) -- файл, куда будут записываться двоичный код данных,
     *   переданных радаром.
     *   <br />---------- ^ сделано
     *   <br />- Файл статуса устройства (.csv) -- данные с датчиков устройства и прочая
     *   информация, которая меняется в процессе сканирования.
     *   <br />- Папка дополнительной информации (тоже будет архивирована).
     */
    public void createNewWriteFile() {
        if (!needSaveData)
            return;
        endWritingToFile();

        folderWrite = Helpers.createUniqueFile(defaultDirectory.getAbsolutePath() +
                "/" + createFileName());
        folderWrite.mkdir();

        try {
            createConfigFile();
            createDataFile();
            createStatusFile();
            createDescriptionFile();
            createAdditionalFolder();
        } catch (IOException e) {
            RadarBox.logger.add(this, "ERROR: CreateNewWriteFile error: " +
                    e.getLocalizedMessage());
            e.printStackTrace();
            endWritingToFile();
        }
    }

    // Configuration
    private void createConfigFile() throws IOException {
        createConfigFile(RadarBox.device.configuration);
    }
    private void createConfigFile(DeviceConfiguration configuration) throws IOException {
        if(RadarBox.device == null)
            throw new IOException("No device");
        XmlSerializer serializer = Xml.newSerializer();
        File configFile = new File(folderWrite.getAbsolutePath() + "/" +
                Helpers.CONFIG_FILE_NAME);
        FileOutputStream fileWriteStream = new FileOutputStream(configFile);
        serializer.setOutput(fileWriteStream,"UTF-8");
        serializer.startDocument(null,Boolean.TRUE);
        serializer.setFeature(
                "http://xmlpull.org/v1/doc/features.html#indent-output", true);
        serializer.startTag(null,"config");
        DeviceConfiguration.writeDeviceConfiguration(serializer, configuration);
        ArrayList<DeviceConfiguration.Parameter> parameters = configuration.getParameters();
        for (DeviceConfiguration.Parameter param: parameters) {
            if(param.getValue().getClass().equals(Boolean.class))
                DeviceConfiguration.writeBooleanParameter(serializer,
                        (DeviceConfiguration.BooleanParameter) param);
            else if(param.getValue().getClass().equals(Integer.class))
                DeviceConfiguration.writeIntegerParameter(serializer,
                        (DeviceConfiguration.IntegerParameter) param);
        }
        serializer.endTag(null,"config");
        serializer.endDocument();
        serializer.flush();
        fileWriteStream.close();
    }

    // Data
    private void createDataFile() throws IOException {
        File dataFile = new File(folderWrite.getAbsolutePath() + "/" +
                Helpers.DATA_FILE_NAME);
        dataWriteStream = new FileOutputStream(dataFile);
    }

    /** Записывает массив двоичных данных в файл .data. */
    public void writeToDataFile(short[] data) {
        if (isWritingToFile() && needSaveData) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(2 * data.length);
            byteBuffer.asShortBuffer().put(data);
            try {
                dataWriteStream.write(byteBuffer.array());
                dataWriteStream.flush();
            } catch (IOException e) {
                RadarBox.logger.add(e.toString());
                e.printStackTrace();
                endWritingToFile();
            }
        }
    }

    // Status
    private void createStatusFile() throws IOException {}

    // Description
    private void createDescriptionFile() throws IOException {
        File descriptionFile = new File(folderWrite.getAbsolutePath() + "/" +
                Helpers.DESC_FILE_NAME);
        if (!descriptionFile.createNewFile()) {
            throw new IOException("Error on creation description file");
        }
    }

    public void writeToDescriptionFile(String description) {
        if (isWritingToFile()) {
            try {
                File descFile = new File(folderWrite.getAbsolutePath() + "/" +
                        Helpers.DESC_FILE_NAME);
                Helpers.writeToTextFile(descFile, description, true);
                System.out.println("Description written: " + description);
            } catch (IOException e) {
                RadarBox.logger.add(this, e.toString());
                e.printStackTrace();
            }
        }
    }

    // Additional files
    private void createAdditionalFolder() throws IOException {
        File additionalFolder = new File(folderWrite.getAbsolutePath() + "/" +
                Helpers.ADDITIONAL_FOLDER_NAME);
        if (!additionalFolder.mkdir()) {
            throw new IOException("Error on creation additional directory");
        }
    }

    // Saving
    /** Закрывает файл данных для записи и создаёт архив со всеми файлами.
     * Имя архива будет представлять собой следующий формат: <дата>_<время>_<постфикс>.zip */
    public void endWritingToFile() {
        endWritingToFile(null, false);
    }
    public void endWritingToFile(Context contextForDialog, boolean sendFile) {
        if (isWritingToFile()) {
            try {
                dataWriteStream.close();
                if (contextForDialog == null) {
                    saveFile();
                } else {
                    createSavingDialog(contextForDialog, sendFile);
                }
            } catch (IOException e) {
                RadarBox.logger.add(e.toString());
                e.printStackTrace();
            }
        }
    }

    private void createSavingDialog(Context contextForDialog, boolean sendFile) {
        final EditText textEditor = new EditText(contextForDialog);

        AlertDialog.Builder builder = new AlertDialog.Builder(contextForDialog);
        builder.setTitle(context.getString(R.string.file_writer_header));
        builder.setMessage(folderWrite.getName() + ".zip\n\n" +
                context.getString(R.string.description_for_file_to_send));
        builder.setView(textEditor);
        builder.setPositiveButton(context.getString(R.string.str_save), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    writeToDescriptionFile(textEditor.getText().toString());
                    saveFile();
                    if (sendFile && aordFile != null) {
                        Sender.createDialogToSendFile(contextForDialog, aordFile);
                    }
                } catch (IOException e) {
                    RadarBox.logger.add(this, e.toString());
                    e.printStackTrace();
                }
            }
        });
        builder.setNegativeButton(context.getString(R.string.str_close), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    terminateWriting();
                } catch (IOException e) {
                    RadarBox.logger.add(this, e.toString());
                    e.printStackTrace();
                }
            }
        });
        builder.create();
        builder.show();
    }

    private void saveFile() throws IOException {
        aordFile = ZipManager.archiveFolder(folderWrite);
        Helpers.removeTree(folderWrite);
        RadarBox.logger.add(this, "INFO: Creation file " + aordFile.getName() +
                " is successful");
    }
    // </ Main methods>

    // Help methods
    private String createFileName() {
        String name = new Timestamp(System.currentTimeMillis()).toString();
        name = name.replace(' ', '_').replace('.',
                '-').replace(':', '-');
        return name + "_" + dataWriteFilenamePostfix;
    }

    public void terminateWriting() throws IOException {
        if (dataWriteStream != null) {
            dataWriteStream.close();
            dataWriteStream = null;
        }
        if (folderWrite != null) {
            Helpers.removeTreeIfExists(folderWrite);
            folderWrite = null;
        }
    }
}
