package org.rdr.radarbox.File;

import android.content.Context;
import android.content.SharedPreferences;

import android.util.Xml;
import org.xmlpull.v1.XmlSerializer;

import org.rdr.radarbox.RadarBox;
import org.rdr.radarbox.Device.DeviceConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import java.io.IOException;

import java.sql.Timestamp;
import java.util.ArrayList;

import androidx.preference.PreferenceManager;

/**
 * Класс для записи данных и создания файла-архива.
 * @author Сапронов Данил Игоревич; Шишмарев Ростислав Иванович
 * @version 0.2.2
 */
public class Writer {
    Context context;
    private File zipFile = null;
    private File folderWrite = null;
    private File defaultDirectory;
    private FileOutputStream dataWriteStream = null;

    private String dataWriteFilenamePostfix;
    private boolean needSaveData = false;

    // Initialize methods
    public Writer(Context context_) {
        context = context_;
        defaultDirectory = context.getExternalFilesDir(Helpers.defaultUserFilesFolderPath);
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
    public File getFileWrite() {return zipFile;}

    /**
     * @return true, если данные должны записываться, false в противном случае.
     */
    public boolean isNeedSaveData() {return needSaveData;}

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

    // Main methods
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
        folderWrite = Helpers.createUniqueFile(defaultDirectory.getAbsolutePath() +
                "/" + createFileName());
        folderWrite.mkdir();

        try {
            createConfigFile();
            createDataFile();
            createStatusFile();
            createAdditionalFolder();
        } catch (IOException e) {
            RadarBox.logger.add(this, "ERROR: CreateNewWriteFile error: " +
                    e.getLocalizedMessage());
            e.printStackTrace();
            endWritingToFile();
        }
    }

    private String createFileName() {
        String name = new Timestamp(System.currentTimeMillis()).toString();
        name = name.replace(' ', '_').replace('.', ':');
        return name + "_" + dataWriteFilenamePostfix;
    }

    private void createConfigFile() throws IOException {
        if(RadarBox.device == null)
            throw new IOException("No device");
        XmlSerializer serializer = Xml.newSerializer();
        File configFile = new File(folderWrite.getAbsolutePath() + "/" +
                Helpers.fileNamesMap.get("config"));
        FileOutputStream fileWriteStream = new FileOutputStream(configFile);
        serializer.setOutput(fileWriteStream,"UTF-8");
        serializer.startDocument(null,Boolean.TRUE);
        serializer.setFeature(
                "http://xmlpull.org/v1/doc/features.html#indent-output", true);
        serializer.startTag(null,"config");
        DeviceConfiguration.writeDeviceConfiguration(serializer,RadarBox.device.configuration);
        ArrayList<DeviceConfiguration.Parameter> parameters
                = RadarBox.device.configuration.getParameters();
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

    private void createDataFile() throws IOException {
        File dataFile = new File(folderWrite.getAbsolutePath() + "/" +
                Helpers.fileNamesMap.get("data"));
        dataWriteStream = new FileOutputStream(dataFile);
    }

    private void createStatusFile() throws IOException {}

    private void createAdditionalFolder() throws IOException {}

    /** Записывает массив двоичных данных в файл .data. */
    public void writeDataToFile(short[] data) {
        if(folderWrite != null && dataWriteStream != null && needSaveData) {
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

    /** Закрывает файл данных для записи и создаёт архив со всеми файлами.
     * Имя архива будет представлять собой следующий формат: <дата>_<время>_<постфикс>.zip */
    public void endWritingToFile() {
        if (dataWriteStream == null || folderWrite == null) {
            RadarBox.logger.add(this,
                    "WARNING: end of writing file when file is not being written");
            return;
        }
        try {
            dataWriteStream.close();
            zipFile = ZipManager.archiveFolder(folderWrite);
            Helpers.removeTree(folderWrite);
            RadarBox.logger.add(this, "INFO: End of creation file " + zipFile.getName());
        } catch (IOException e) {
            RadarBox.logger.add(e.toString());
            e.printStackTrace();
        }
    }
}
