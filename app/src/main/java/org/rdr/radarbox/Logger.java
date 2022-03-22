package org.rdr.radarbox;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Класс-логгер, создающий файл log.txt в директории Documents и записывающий события приложения
 * @author Сапронов Данил Игоревич
 * @version 0.1
 */
public class Logger {
    BufferedWriter logFileWriter;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    private File fileLog;
    private MutableLiveData<String> liveLastStringWritten = new MutableLiveData<>();
    public LiveData<String> getLiveLastStringWritten() {return liveLastStringWritten;}

    public Logger(Context context) {
        try {
            fileLog = new File(context
                    .getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).toString()+"/log.txt");
            logFileWriter = new BufferedWriter(new FileWriter(fileLog));
            logFileWriter.write("\t---\t"
                    +new Timestamp(System.currentTimeMillis()).toString()
                    +"\t---\t\n");
            logFileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public File getFileLog() {
        return fileLog;
    }

    public void add(String str) {
        String strToWrite =  timeFormat.format(new Date(System.currentTimeMillis())) + ": " + str;
        try {
            Log.i("LOGGER",strToWrite); // дублирование сообщений в logcat
            logFileWriter.append(strToWrite);
            logFileWriter.newLine();
            logFileWriter.flush();
            liveLastStringWritten.postValue(strToWrite);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void add(String tag, String str){

        String strToWrite = timeFormat.format(new Date(System.currentTimeMillis())) + ": " + tag
                + ": " + str;
        try {
            Log.i("LOGGER",strToWrite); // дублирование сообщений в logcat
            logFileWriter.append(strToWrite);
            logFileWriter.newLine();
            logFileWriter.flush();
            liveLastStringWritten.postValue(strToWrite);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void add(Object obj, String str) {
        String strToWrite = timeFormat.format(new Date(System.currentTimeMillis())) + ": " +
                obj.getClass().getSimpleName() + ": " + str;
        try {
            Log.i("LOGGER",strToWrite); // дублирование сообщений в logcat
            logFileWriter.append(strToWrite);
            logFileWriter.newLine();
            logFileWriter.flush();
            liveLastStringWritten.postValue(strToWrite);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        if (bytes != null)
            for (byte b:bytes) {
                final String hexString = Integer.toHexString(b & 0xff);
                if(hexString.length()==1)
                    sb.append('0');
                sb.append(hexString).append(' ');//.append(' ');
            }
        return sb.toString().toUpperCase();
    }

}
