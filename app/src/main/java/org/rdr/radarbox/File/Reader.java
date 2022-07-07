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
 * Класс для чтения данных из AoRD-файла (архива данных радара).
 * @author Сапронов Данил Игоревич; Шишмарев Ростислав Иванович
 * @version 0.3.0
 */
public class Reader {
    private Context context;
    private File aordFile = null;
    private ZipManager zipManager = null;
    private final File defaultDirectory;
    // Data variables
    private MappedByteBuffer fileReadBuffer;
    private ShortBuffer fileReadShortBuffer;
    private int fileReadFrameCount;
    private int curReadFrame;
    // Configuration variables
    private DeviceConfiguration virtualDeviceConfiguration = null;
    // Description variables
    private String description = null;

    // Initialize methods
    public Reader(Context context_) {
        context = context_;
        defaultDirectory = context.getExternalFilesDir(Helpers.AoRD_FILES_DEFAULT_FOLDER_PATH);
        if (Helpers.autoRunReader) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            String dataReadFilename = pref.getString("file_reader_filename","");
            RadarBox.logger.add(this, dataReadFilename);
            if(!dataReadFilename.isEmpty()) {
                setFileRead(dataReadFilename);
            }
        }
    }

    // Get methods
    /** Текущий открытый архив с файлом для чтения радиолокационных данных.
     * @return null, если открытого архива не существует.
     */
    public final File getFileRead() {return aordFile;}

    /** Возвращает конфигурацию устройства, считанную из файла, либо null.
     * @return null, если файл с конфигурацией не открыт.
     */
    public DeviceConfiguration getVirtualDeviceConfiguration() {return virtualDeviceConfiguration;}

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

    public int getFileReadFrameCount() {
        if (fileReadFrameCount > 0)
            return fileReadFrameCount;
        else
            return -1;
    }

    public String getDescription() {
        return description;
    }

    // Set methods
    /** Распаковка файла с заданным имененем и открытие его содержимого.
     * @param name - имя файла, включая расширение .zip
     * @return true, если файлы успешно открыты
     * и двоичные данные в data-файле преобразованы для дальнешего считывания.
     */
    public boolean setFileRead(String name) {
        aordFile = new File(defaultDirectory.getPath() + "/" + name);
        try {
            if (zipManager == null) {
                zipManager = new ZipManager(aordFile);
            } else {
                zipManager.setZipFile(aordFile);
            }
        } catch (WrongFileFormatException | FileNotFoundException e) {
            RadarBox.logger.add(e.toString());
            e.printStackTrace();
            return false;
        }
        try {
            readFile();
            RadarBox.logger.add(this,"INFO: File " + name + " has been read");
            return true;
        } catch (IOException e) {
            RadarBox.logger.add(e.toString());
            aordFile = null;
            e.printStackTrace();
        }
        return false;
    }

    // <Main methods>
    /** Создание виртуальной конфигурации устройства, из которой можно
     * узнать параметры устройства, с которого были записаны данные;
     * Считывание данных из data-файла.
     * @throws IOException
     */
    private void readFile() throws IOException {
        zipManager.unzipFile();
        File folder = zipManager.getUnzipFolder();
        Helpers.checkAoRDFileFolder(folder);

        readConfigFile(folder);
        readDataFile(folder);
        readDescriptionFile(folder);

        zipManager.close();
    }

    // Configuration
    private void readConfigFile(File folder) throws IOException {
        FileInputStream configReadStream = new FileInputStream(folder.getAbsolutePath() +
                "/" + Helpers.CONFIG_FILE_NAME);
        virtualDeviceConfiguration = new VirtualDeviceConfiguration(context,
                "virtual", configReadStream);
        configReadStream.close();
    }

    // Data
    private void readDataFile(File folder) throws IOException {
        File dataFile = new File(folder.getPath() + "/" +
                Helpers.DATA_FILE_NAME);
        FileInputStream dataReadStream = new FileInputStream(dataFile);
        fileReadBuffer = dataReadStream.getChannel().map(
                FileChannel.MapMode.READ_ONLY,0, dataFile.length());
        fileReadBuffer.mark(); //отметка, с которой можно начать читать данные типа short

        // создание буфера типа short для удобного считывания данных
        fileReadShortBuffer = fileReadBuffer.order(ByteOrder.BIG_ENDIAN).asShortBuffer();

        // установка отметки в нулевую позицию для возможности в будущем перечитывать данные
        fileReadShortBuffer.mark();
        curReadFrame = 0;
        dataReadStream.close();
    }

    //Description
    private void readDescriptionFile(File folder) throws IOException {
        File descFile = new File(folder.getAbsolutePath() + "/" + Helpers.DESC_FILE_NAME);
        description = Helpers.readTextFile(descFile);
    }
    // </ Main methods>

    // Help methods & classes
    /** Получение списка всех файлов в директории с расширением .zip
     * @return перечень файлов с расширением .zip, пустой список во всех остальных случаях
     */
    public String[] getFilesList() {
        String[] listOfFiles = defaultDirectory.list((d, s) -> s.toLowerCase().endsWith(".zip"));
        if (listOfFiles == null)
            listOfFiles = new String[]{};
        return listOfFiles;
    }

    /** Класс-наследник {@link #DeviceConfiguration} для считывания конфигурации устройства
     * из конфигурационного файла */
    class VirtualDeviceConfiguration extends DeviceConfiguration {

        public VirtualDeviceConfiguration(Context context, String devicePrefix, InputStream configFileStream) {
            super(context, devicePrefix);

            if(aordFile != null) {
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
            parameter.setValue(Boolean.parseBoolean(parser.getAttributeValue(
                    null,"value")));
            return parameter;
        }

        protected IntegerParameter readIntegerParameter(XmlPullParser parser) {
            IntegerParameter parameter = super.readIntegerParameter(parser);
            parameter.setValue(Integer.parseInt(
                        (parser.getAttributeValue(null,"value")),
                    parameter.getRadix()));
            return parameter;
        }
    }
}
