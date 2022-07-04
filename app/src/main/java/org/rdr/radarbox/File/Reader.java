package org.rdr.radarbox.File;

import android.content.Context;
import android.content.SharedPreferences;

import org.rdr.radarbox.Device.DeviceConfiguration;
import org.rdr.radarbox.RadarBox;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.InvalidMarkException;
import java.nio.BufferUnderflowException;

import java.nio.ByteOrder;

import java.io.InputStream;
import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;

import androidx.preference.PreferenceManager;

/**
 * Класс для чтения данных из файла-архива.
 * @author Сапронов Данил Игоревич; Шишмарев Ростислав Иванович
 * @version 0.2.1
 */
public class Reader {
    private Context context;
    private File fileRead = null;
    private ZipManager zipManager = null;
    private final File defaultDirectory;
    private DeviceConfiguration virtualDeviceConfiguration = null;

    private MappedByteBuffer fileReadBuffer;
    private ShortBuffer fileReadShortBuffer;

    private int fileReadFrameCount;
    private int curReadFrame;

    // Initialize methods
    public Reader(Context context_) {
        context = context_;
        defaultDirectory = context.getExternalFilesDir(Helpers.defaultFolderPath);
//        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
//        String dataReadFilename = pref.getString("file_reader_filename","");
//        RadarBox.logger.add(this, dataReadFilename);
//        if(!dataReadFilename.isEmpty()) {
//            setFileRead(dataReadFilename);
//        }
    }

    // Get methods
    /** Текущий открытый архив с файлом для чтения радиолокационных данных.
     * @return null, если открытого архива не существует.
     */
    public final File getFileRead() {return fileRead;}

    /** Возвращает конфигурацию устройства, считанную из файла, либо null.
     * @return null, если файл с конфигурацией не открыт.
     */
    public DeviceConfiguration getVirtualDeviceConfiguration() {return virtualDeviceConfiguration;}

    // Main methods
    /** Распаковка файла с заданным имененем и открытие его содержимого.
     * @param name - имя файла, включая расширение .zip
     * @return true, если файлы успешно открыты
     * и двоичные данные в data-файле преобразованы для дальнешего считывания.
     */
    public boolean setFileRead(String name) {
        fileRead = new File(defaultDirectory.getPath() + "/" + name);
        try {
            if (zipManager == null) {
                zipManager = new ZipManager(fileRead);
            } else {
                zipManager.setZipFile(fileRead);
            }
        } catch (NoAZipFileException | FileNotFoundException e) {
            RadarBox.logger.add(e.toString());
            e.printStackTrace();
            return false;
        }
        try {
            readFile();
            RadarBox.logger.add(this,"File "+name+" is open for reading");
            return true;
        } catch (IOException e) {
            RadarBox.logger.add(e.toString());
            fileRead = null;
            e.printStackTrace();
        }
        return false;
    }

    /** Создание виртуальной конфигурации устройства, из которой можно
     * узнать параметры устройства, с которого были записаны данные;
     * Считывание данных из data-файла.
     * @throws IOException
     */
    private void readFile() throws IOException {
        zipManager.unzipFile();
        File folder = zipManager.getUnzipFolder();

        FileInputStream configReadStream = new FileInputStream(folder.getAbsolutePath() + "/" +
                Helpers.fileNamesMap.get("config"));
        virtualDeviceConfiguration = new VirtualDeviceConfiguration(context,
                "virtual", configReadStream);
        configReadStream.close();

        File dataFile = new File(folder.getPath() + "/" +
                Helpers.fileNamesMap.get("data"));
        FileInputStream dataReadStream = new FileInputStream(dataFile);
        fileReadBuffer = dataReadStream.getChannel().map(
                FileChannel.MapMode.READ_ONLY,0, dataFile.length());
        /*
        boolean isEndOfConfigReached = false;
        while(fileReadBuffer.hasRemaining() && !isEndOfConfigReached) {
            byte[] endOfConfig = ("</config>").getBytes(StandardCharsets.UTF_8);
            for(int i=0; endOfConfig[i]==fileReadBuffer.get(); i++)
                if(i==endOfConfig.length-1) { //найдено ключевое слово
                    isEndOfConfigReached = true; //достигнут конец заголовка файла
                    break;
                }
        }
        fileReadBuffer.get(); //переход на новую строку */

        fileReadBuffer.mark(); //отметка, с которой можно начать читать данные типа short
        // создание буфера типа short для удобного считывания данных
        fileReadShortBuffer = fileReadBuffer.order(ByteOrder.BIG_ENDIAN).asShortBuffer();

        // установка отметки в нулевую позицию для возможности в будущем перечитывать данные
        fileReadShortBuffer.mark();
        curReadFrame = 0;
        dataReadStream.close();
    }

    /**
     * Завершение работы и удаление распакованных файлов.
     */
    public void close() {
        if (zipManager != null) {
            zipManager.close();
        }
    }

    // Help methods
    /** Получение списка всех файлов в директории с расширением .zip
     * @return перечень файлов с расширением .zip, пустой список во всех остальных случаях
     */
    public String[] getFilesList() {
        // String[] listOfFiles = new String[]{};
        String[] listOfFiles = defaultDirectory.list((d, s) -> s.toLowerCase().endsWith(".zip"));
        if (listOfFiles == null)
            listOfFiles = new String[]{};
        return listOfFiles;
    }

    public int getFileReadFrameCount() {
        if (fileReadFrameCount > 0)
            return fileReadFrameCount;
        else
            return -1;
    }

    public void getNextFrame(short[] dest) {
        try {
            fileReadShortBuffer.get(dest,0, dest.length);
            curReadFrame++;
        }
        catch (BufferUnderflowException e) {
            RadarBox.logger.add(this,e.toString() + "\n\tCouldn't read " + curReadFrame
                    + "-th frame. \n\tFileBuffer position: " + fileReadBuffer.position()
                    + "\n\tReading elements count: " + dest.length + "(int16)"
                    + "\n\tElements remaining in buffer: " + fileReadShortBuffer.remaining()
                    + "(int16)");
            try {
                fileReadShortBuffer.reset();
                curReadFrame=0;

            } catch (InvalidMarkException e2) {
                RadarBox.logger.add(this,e2.getLocalizedMessage() +
                        "\n\tCouldn't reload file");
                //RadarBox.mainDataThread.extraStop();
            }
        }
    }

    // Classes
    // Help classes
    /** Класс-наследник {@link #DeviceConfiguration} для считывания конфигурации устройства
     * из конфигурационного файла */
    class VirtualDeviceConfiguration extends DeviceConfiguration {

        public VirtualDeviceConfiguration(Context context, String devicePrefix, InputStream configFileStream) {
            super(context, devicePrefix);

            if(fileRead != null) {
                try {
                    parseConfigurationHeader(configFileStream);
                } catch (XmlPullParserException | IOException e) {
                    RadarBox.logger.add(deviceName + " CONFIG",
                            "parseConfiguration ERROR: " + e.getLocalizedMessage());
                }
            }
            else RadarBox.logger.add(this,"FileRead is null");
        }

        private void parseConfigurationHeader(InputStream inputStream)
                throws IOException, XmlPullParserException {
            super.parseConfiguration(inputStream);
        }

        protected void readDeviceConfiguration(XmlPullParser parser) {
            super.readDeviceConfiguration(parser);
        }

        protected BooleanParameter readBooleanParameter(XmlPullParser parser) {
            BooleanParameter parameter = super.readBooleanParameter(parser);
            parameter.setValue(Boolean.parseBoolean(parser
                    .getAttributeValue(null,"value")));
            return parameter;
        }

        protected IntegerParameter readIntegerParameter(XmlPullParser parser) {
            IntegerParameter parameter = super.readIntegerParameter(parser);
            parameter.setValue(Integer.parseInt(
                        (parser.getAttributeValue(null,"value")),parameter.getRadix()));
            return parameter;
        }
    }
}
