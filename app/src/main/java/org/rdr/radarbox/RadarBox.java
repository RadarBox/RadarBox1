package org.rdr.radarbox;

import android.os.Bundle;
import android.app.Activity;
import android.app.Application;
import android.content.Context;

import org.rdr.radarbox.DSP.FreqSignals;
import org.rdr.radarbox.Device.Device;
import org.rdr.radarbox.File.AoRDFile;
import org.rdr.radarbox.File.AoRDSettingsManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.preference.PreferenceManager;

/**
 * Основной СИНГЛТОН класс приложения. Наследуется от Application,
 * чтобы в методе {@link #onCreate()} создавать единственный экземпляр класса.
 * Остальные главные классы приложения доступны через {@link #getInstance()}
 * @author Сапронов Данил Игоревич
 * @version 0.3
 */
public class RadarBox extends Application implements Application.ActivityLifecycleCallbacks {
    private static RadarBox radarBox;
    private static Context appContext;
    private static Activity currentActivity;

    private String[] devicePrefixList;
    public static Logger logger;
    public static Device device;
    public static AoRDFile fileRead;
    public static AoRDFile fileWrite;
    public static FreqSignals freqSignals;
    public static DataThreadService dataThreadService;

    // Init methods
    @Override
    public void onCreate() {
        appContext = getApplicationContext();
        super.onCreate();
        if (radarBox == null) {
            radarBox = new RadarBox();
            radarBox.initializeSharedObjects(appContext);
        }
        registerActivityLifecycleCallbacks(this);
    }

    private void initializeSharedObjects(@NonNull Context appContext) {
        logger = new Logger(appContext);
        setDeviceArrayListOnStart();
        freqSignals = new FreqSignals();
        dataThreadService = new DataThreadService();

        // Обязательное закрытие AoRD-файлов
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                closeAoRDFile(fileRead);
                closeAoRDFile(fileWrite);
                // На случай прошлых обрушений приложения
                AoRDSettingsManager.cleanDefaultDir();
            }
        });
    }

    // Get methods
    public static RadarBox getInstance() {
        return radarBox;
    }

    public static Activity getCurrentActivity() {
        return currentActivity;
    }

    public static Context getAppContext() {
        return appContext;
    }

    public String[] getDevicePrefixList() {
        return devicePrefixList;
    }

    // Set methods
    private void setDeviceArrayListOnStart() {
        devicePrefixList = appContext.getResources().getStringArray(R.array.device_prefix_list);
        String currentDevice = PreferenceManager.getDefaultSharedPreferences(appContext).getString(
                "last_connected_device",
                devicePrefixList[0]);
        setCurrentDevice(currentDevice);
    }

    /** <p>Установка (создание) текущего устройства</p>
     * Установка осуществляется по "префиксу" устройства.
     * Префиксом называется символьное обозначение устройства, по которому хранятся настройки
     * данного устройства. Кроме того, у устройства существует пакет специфичных классов и
     * конструктор. Имена пакета и конструктора повторяют префикс в верхнем регистре.
     * Например, если префикс устройства - "rdr123", то его специфичные классы хранятся в пакете
     * "RDR123" внутри данного пакета. В пакете "RDR123" обязательно есть конструктор устройства
     * c именем "RDR123".
     * Если текущее подключенное устройство не удалось отключить, возвращает false.
     *
     * @param devicePrefix префикс устройства, которое необходимо установить
     * @return true, если переключение на новое устройство осуществилось успешно,
     * false в остальых случаях. Исключения при создании устройства выводятся в лог приложения.
     */
    public boolean setCurrentDevice(String devicePrefix) {
        if(device != null && !device.getDevicePrefix().equals(devicePrefix)) {
            if(!device.Disconnect())
                return false;
        }
        if(device == null || !devicePrefix.equals(device.getDevicePrefix())) {
            String className = appContext.getPackageName()
                    + "." + devicePrefix.toUpperCase() + "." + devicePrefix.toUpperCase();
            try {
                Class<?> clazz = Class.forName(className);
                Constructor<?> ctor = clazz.getConstructor(Context.class, String.class);
                device = (Device) ctor.newInstance(new Object[] {appContext, devicePrefix});
                return true;
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                    InstantiationException | InvocationTargetException e) {
                logger.add(this, "Class (" + className + ") creation error in package (" +
                        appContext.getPackageName() + "): " + e.getLocalizedMessage());
                return false;
            }
        }
        else
            return false;
    }

    /**
     * Задаёт атрибуту AoRD-файла новое значение.
     * @param attribute - один из атрибутов: {@link RadarBox#fileRead} или
     * {@link RadarBox#fileWrite}.
     * @param newFile - новое значение атрибута (в том числе null).
     */
    public static void setAoRDFile(AoRDFile attribute, AoRDFile newFile) {
        if (attribute == fileRead) {
            closeAoRDFile(attribute);
            fileRead = newFile;
        } else if (attribute == fileWrite) {
            closeAoRDFile(attribute);
            fileWrite = newFile;
        }
    }

    private static void closeAoRDFile(AoRDFile attribute) {
        if (attribute == fileRead) {
            if (fileRead != null) {
                fileRead.close();
                fileRead = null;
            }
        } else if (attribute == fileWrite) {
            if (fileWrite != null) {
                fileWrite.close();
                fileWrite = null;
            }
        }
    }

    // Life cycle methods
    @Override public void onActivityCreated(@NonNull Activity activity,
                                            @Nullable Bundle savedInstanceState) {}

    @Override public void onActivityStarted(@NonNull Activity activity) {}

    @Override public void onActivityResumed(@NonNull Activity activity) {currentActivity = activity;}

    @Override public void onActivityPaused(@NonNull Activity activity) {}

    @Override public void onActivityStopped(@NonNull Activity activity) {}

    @Override public void onActivitySaveInstanceState(@NonNull Activity activity,
                                                      @NonNull Bundle outState) {}

    @Override public void onActivityDestroyed(@NonNull Activity activity) {}
}
