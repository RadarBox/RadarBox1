package org.rdr.radarbox.File;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Xml;

import org.rdr.radarbox.Device.Device;
import org.rdr.radarbox.Device.DeviceConfiguration;
import org.rdr.radarbox.RadarBox;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;

import androidx.preference.PreferenceManager;

public class Writer {
    Context context;
    private boolean needSaveData;
    private File directoryDocuments;
    private File fileWrite;
    private FileOutputStream fileWriteStream;
    private String dataWriteFilenamePostfix;

    public Writer(Context context) {
        needSaveData = false;
        this.context = context;
        directoryDocuments = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        dataWriteFilenamePostfix = pref.getString("file_writer_filename","");
        // Устанавливать пункт "Сохранять данные" в false при запуске программы
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("need_save",false);
        editor.apply();
    }

    /** Метод задаёт новый постфикс в имени всех последующих файлов для записи сырых данных.
     * Имена файлов будут представлять собой следующий формат: ДАТА ВРЕМЯ ПОСТФИКС.data
     * @param postfix постфикс в имени файла
     */
    public void setDataWriteFilenamePostfix (String postfix) {
        dataWriteFilenamePostfix = postfix;
    }

    /** Метод для включения/выключения записи сырых данных в файл
     * @param value true - каждое нажатие на "СТАРТ" будет создавать новый файл и сохрать все данные в него
     */
    public void setNeedSaveData(boolean value) {
        needSaveData = value;
    }

    /** Метод создаёт новый файл для записи данных, каждый раз когда запускается новый сбор данных
     * Имя файла будет представлять собой следующий формат: ДАТА ВРЕМЯ ПОСТФИКС.data
     * В начале файла будет шапка из 1024 байт, включающая описание текущих настроек радара
     * и всей необходимой информации для правильной интерпретации данных в будущем.
     */
    public void createNewWriteFile() {
        if (!needSaveData)
            return;
        String name = new Timestamp(System.currentTimeMillis()).toString()+
                dataWriteFilenamePostfix+".data";
        fileWrite = new File(directoryDocuments.getPath()+"/"+name);
        try {
            fileWriteStream = new FileOutputStream(fileWrite);
            if(!createHeaderToDataFile()) closeWriteFile();
        } catch (IOException e) {
            RadarBox.logger.add(e.toString());
            e.printStackTrace();
        }
    }

    /** Записывает массив двоичных данных в файл */
    public void writeDataToFile(short[]data) {
        if(fileWrite.exists() && needSaveData) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(2*data.length);
            byteBuffer.asShortBuffer().put(data);
            try {
                fileWriteStream.write(byteBuffer.array());
                fileWriteStream.flush();
            } catch (IOException e) {
                RadarBox.logger.add(e.toString());
                e.printStackTrace();
            }
        }
    }

    public File getFileWrite() {return fileWrite;}

    /** Метод закрывает файл для записи */
    public void closeWriteFile() {
        try {
            fileWriteStream.close();
        } catch (IOException e) {
            RadarBox.logger.add(e.toString());
            e.printStackTrace();
        }
    }

    public boolean isNeedSaveData() {return needSaveData;}

    /** Создаётся заголовок к файлу исходных данных, размером 1024 байта, включающий в себя
     * описание настроек подключенного радара для корректного описания в будущем.
     * @return возвращает заголовок длиной 1024 байта.
     */
    boolean createHeaderToDataFile() {
        if(!fileWrite.exists() || !needSaveData || RadarBox.device==null)
            return false;
        XmlSerializer serializer = Xml.newSerializer();
        try {
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
            serializer.text("\n");
            serializer.endDocument();
            serializer.flush();
        } catch (IOException ioException) {
            RadarBox.logger.add(this,"CreateHeaderToDataFile error: "
                    +ioException.getLocalizedMessage());
            return false;
        }
        return true;
    }
}
