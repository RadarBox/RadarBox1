package org.rdr.radarbox.File;


import android.content.Context;

import org.rdr.radarbox.Device.DeviceStatus;
import org.rdr.radarbox.RadarBox;
import org.rdr.radarbox.Device.DeviceConfiguration;

import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.file.NotDirectoryException;
import java.nio.InvalidMarkException;
import java.nio.BufferUnderflowException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;

import androidx.annotation.NonNull;

/**
 * Класс-наследник {@link File} для работы с AoRD-файлами (Archive of Radar Data).
 * Поддерживает чтение, запись и создание.<br />
 * Важно: <b>все методы родителя ({@link File}) относятся к самому AoRD-файлу
 * (не к папке, в которую он распакован)</b>.
 * <br />Ошибки методов класса, помеченных #enable_danger, могут повлиять на работоспособность
 * ({@link AoRDFile#isEnabled()}) класса (в этот список входят все конструкторы).
 * <br /><br />
 * Подробнее о формате AoRD:
 * <br /> Сам файл - это zip-архив с конкретным списком файлов внутри:
 *  <br />- <i>Файл конфигурации</i>* (.xml) -- описание текущих настроек радара и всей необходимой
 *  информации для правильной интерпретации данных в будущем.
 *  <br />- <i>Файл данных</i> (.data) -- файл, куда будет записываться двоичный код данных,
 *  переданных радаром.
 *  <br />- Файл описания (.txt) -- краткое текстовое описание эксперимента.
 *  <br />- Папка дополнений -- папка для файлов с различной дополнительной
 *  информацией (тоже архивируется).
 *  <br />---------- ^ сделано
 *  <br />- Файл статуса устройства (.csv) -- данные с датчиков устройства и прочая
 *  информация, которая меняется в процессе сканирования.
 *  <br />*Курсивом выделены обязательные файлы.
 * @author Шишмарев Ростислав Иванович; Сапронов Данил Игоревич
 * @version v0.1.0
 */
public class AoRDFile extends File {
    private Context aordFileContext = RadarBox.getAppContext();

    private File unzipFolder = null;
    private ZipManager zipManager = null;

    public DataFileManager data = null;
    public ConfigurationFileManager config = null;
    public StatusFileManager status = null;
    public DescriptionFileManager description = null;
    public AdditionalFileManager additional = null;

    private boolean enabled = true;

    // Initialize methods
    public AoRDFile(@NonNull File file) {
        super(file.getAbsolutePath());
        try {
            Helpers.checkFileExistence(file);

            zipManager = new ZipManager(this);
            zipManager.unzipFile();

            unzipFolder = zipManager.getUnzipFolder();
            checkAoRDFileFolder(unzipFolder);

            read();
        } catch (IOException e) {
            RadarBox.logger.add(this, "ERROR: " + e.getLocalizedMessage());
            e.printStackTrace();
            close();
        }
    }

    public AoRDFile(@NonNull File file, @NonNull File folder) {
        super(file.getAbsolutePath());
        try {
            Helpers.checkFileExistence(file);
            Helpers.checkFileExistence(folder);
            checkAoRDFileFolder(folder);

            unzipFolder = folder;
            zipManager = new ZipManager(this);

            read();
        } catch (IOException e) {
            RadarBox.logger.add(this, "ERROR: " + e.getLocalizedMessage());
            e.printStackTrace();
            close();
        }
    }

    public AoRDFile(String filePath) {
        super(filePath);
        try {
            Helpers.checkFileExistence(this);

            zipManager = new ZipManager(this);
            zipManager.unzipFile();

            unzipFolder = zipManager.getUnzipFolder();
            Helpers.checkFileExistence(unzipFolder);
            checkAoRDFileFolder(unzipFolder);

            read();
        } catch (IOException e) {
            RadarBox.logger.add(this, "ERROR: " + e.getLocalizedMessage());
            e.printStackTrace();
            close();
        }
    }

    public AoRDFile(String filePath, String folderPath) {
        super(filePath);
        try {
            Helpers.checkFileExistence(this);

            unzipFolder = new File(folderPath);
            Helpers.checkFileExistence(unzipFolder);
            checkAoRDFileFolder(unzipFolder);

            zipManager = new ZipManager(this);

            read();
        } catch (IOException e) {
            RadarBox.logger.add(this, "ERROR: " + e.getLocalizedMessage());
            e.printStackTrace();
            close();
        }
    }

    /**
     * Проверка папки, в которую распакован AoRD-файл, на корректность содержимого.
     * @param aordFileUnzipFolder - объект {@link File} директории.
     * @throws NotDirectoryException - если передана не директория.
     * @throws WrongFileFormatException - если в формате AoRD допущена ошибка.
     */
    public static void checkAoRDFileFolder(File aordFileUnzipFolder)
            throws NotDirectoryException, WrongFileFormatException {
        if (!aordFileUnzipFolder.isDirectory()) {
            throw new NotDirectoryException("Файл " + aordFileUnzipFolder.getAbsolutePath() +
                    " не является директорией");
        }
        for (String fileName : new String[] {Const.CONFIG_FILE_NAME, Const.DATA_FILE_NAME}) {
            if (!new File(aordFileUnzipFolder.getAbsolutePath() + "/" +
                    fileName).exists()) {
                throw new WrongFileFormatException(
                        "Некорректный формат AoRD-файла: не хватает файла (папки) " + fileName);
            }
        }
    }

    // Get methods
    /**
     * Определяет, в рабочем ли состоянии (enabled) AoRD-файл.
     * @return true, если в работе с объектом не возникли критические ошибки,
     * false в противном случае.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return контекст AoRD-файла (по умолчанию имеет значение RadarBox.getAppContext()).
     */
    public Context getContext() {
        return aordFileContext;
    }

    /**
     * @return объект {@link File} папки, в которую был распакован AoRD-файл.
     */
    public File getUnzipFolder() {
        return unzipFolder;
    }

    // Main methods
    private void read() throws IOException {
        data = new DataFileManager();
        config = new ConfigurationFileManager();
        status = new StatusFileManager();
        description = new DescriptionFileManager();
        additional = new AdditionalFileManager();
    }

    /**
     * Создание AoRD-файла с записанной конфигурацтей и пустыми остальными файлами.
     * @param path - путь до создаваемого файла (без ".zip").
     * @return null, если в процессе создания возникла ошибка
     * (в том числе если {@link AoRDFile#isEnabled()} == false).
     */
    public static AoRDFile createNewAoRDFile(String path) {
        if (path.endsWith(".zip")) {
            return null;
        }
        File folderWrite = Helpers.createUniqueFile(path);
        try {
            if (!folderWrite.mkdir()) {
                throw new IOException("Can`t create main write folder");
            }
            for (String name : new String[] {Const.DATA_FILE_NAME, Const.CONFIG_FILE_NAME,
                    Const.STATUS_FILE_NAME, Const.DESC_FILE_NAME}) {
                File file = new File(folderWrite.getAbsolutePath() + "/" + name);
                if (!file.createNewFile()) {
                    throw new IOException("Can`t create file " + name);
                }
            }
            File file = new File(folderWrite.getAbsolutePath() + "/" +
                    Const.ADDITIONAL_FOLDER_NAME);
            if (!file.mkdir()) {
                throw new IOException("Can`t create folder " + Const.ADDITIONAL_FOLDER_NAME);
            }
            try {
                File zipFile = ZipManager.archiveFolder(folderWrite);
                AoRDFile result = new AoRDFile(zipFile, folderWrite);
                if (result.isEnabled()) {
                    result.config.write(RadarBox.device.configuration);
                    result.status.writeHeader(RadarBox.device.status);
                    RadarBox.logger.add("Successful creation of file " + result.getAbsolutePath());
                    return result;
                }
            } catch (IOException ie) {
                RadarBox.logger.add("ERROR on creation zip");
            }
        } catch (Exception e) {
            RadarBox.logger.add("ERROR: AoRDFile.createNewAoRDFile error: " +
                    e.getLocalizedMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Сохранение всех изменений.
     * <br />#enable_danger
     */
    public void commit() {
        data.endWriting();
        if (!delete()) {
            RadarBox.logger.add(this, "ERROR: Can`t commit file " + getAbsolutePath());
            return;
        }
        try {
            additional.prepareToCommit();
            File newSelf = ZipManager.archiveFolder(unzipFolder);
            if (!newSelf.getAbsolutePath().equals(getAbsolutePath())) {
                close();
                throw new IOException("Error on commit file: incorrect archive name");
            }
            additional.commit();
        } catch (IOException e) {
            RadarBox.logger.add(this, e.toString());
            e.printStackTrace();
            close();
        }
        RadarBox.logger.add(this, "INFO: Commit on file " + getName() + " is successful");
    }

    /**
     * Закрытие файла (без сохранения изменений). Его обязательно нужно вызвать после
     * окончания работы с объектом.<br />
     * После вызова этого метода объект использованию не подлежит
     * ({@link AoRDFile#isEnabled()} == false).
     */
    public void close() {
        data.endWriting();
        Helpers.removeTreeIfExists(unzipFolder);
        enabled = false;
    }

    // Classes for inner files
    /**
     * Базовый класс для классов внутренних файлов AoRD-файла.
     */
    protected class BaseInnerFileManager {
        protected File selfFile = null;

        protected BaseInnerFileManager(String name, boolean required) throws IOException {
            selfFile = new File(unzipFolder.getAbsolutePath() + "/" + name);
            if (!required && !selfFile.exists()) {
                if (!selfFile.createNewFile()) {
                    throw new IOException("Can`t create file " + selfFile.getAbsolutePath());
                }
            } else if (required && !selfFile.exists()) {
                throw new FileNotFoundException("Required file in " +
                        AoRDFile.this.getAbsolutePath() + " not found");
            }
        }

        /**
         * @return объект {@link File} файла, управляемого классом.
         */
        public File getFile() {
            return selfFile;
        }
    }

    /**
     * Класс для управления файлом данных.
     */
    public class DataFileManager extends BaseInnerFileManager {
        private MappedByteBuffer fileReadBuffer;
        private ShortBuffer fileReadShortBuffer;
        private int curReadFrame;
        private FileOutputStream dataWriteStream = null;

        DataFileManager() throws IOException {
            super(Const.DATA_FILE_NAME, true);
            readSelf();
        }

        protected void readSelf() throws IOException {
            FileInputStream dataReadStream = new FileInputStream(selfFile);
            fileReadBuffer = dataReadStream.getChannel().map(
                    FileChannel.MapMode.READ_ONLY,0, selfFile.length());
            fileReadBuffer.mark(); //отметка, с которой можно начать читать данные типа short

            // создание буфера типа short для удобного считывания данных
            fileReadShortBuffer = fileReadBuffer.order(ByteOrder.BIG_ENDIAN).asShortBuffer();

            // установка отметки в нулевую позицию для возможности в будущем перечитывать данные
            fileReadShortBuffer.mark();
            curReadFrame = 0;
            dataReadStream.close();
        }

        /**
         * Считывает следующий кадр данных в массив.
         * @param dest - массив для записи.
         */
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
                    // RadarBox.mainDataThread.extraStop();
                }
            }
        }

        /**
         * Начинает запись данных.
         * <br />#enable_danger
         */
        public void startWriting() {
            try {
                dataWriteStream = new FileOutputStream(selfFile);
            } catch (IOException e) {
                RadarBox.logger.add(this, e.toString());
                e.printStackTrace();
                endWriting();
            }

        }

        /**
         * Записывает массив двоичных данных в файл
         * (при значении {@link AoRDSettingsManager#isNeedSaveData()} == true).
         * До {@link DataFileManager#startWriting()} и после {@link DataFileManager#endWriting()}
         * игнорируется.
         * @param data - массив, который нужно записать.
         */
        public void write(short[] data) {
            if (dataWriteStream != null && AoRDSettingsManager.isNeedSaveData()) {
                ByteBuffer byteBuffer = ByteBuffer.allocate(2 * data.length);
                byteBuffer.asShortBuffer().put(data);
                try {
                    dataWriteStream.write(byteBuffer.array());
                    dataWriteStream.flush();
                } catch (IOException e) {
                    RadarBox.logger.add(this, e.toString());
                    e.printStackTrace();
                    endWriting();
                }
            }
        }

        /**
         * Завершает запись данных. Автоматически вызывается в {@link AoRDFile#commit()} и
         * {@link AoRDFile#close()}.
         * <br />#enable_danger
         */
        public void endWriting() {
            if (dataWriteStream == null) {
                return;
            }
            try {
                dataWriteStream.close();
                readSelf();
            } catch (IOException e) {
                RadarBox.logger.add(this, e.toString());
                e.printStackTrace();
                close();
            }
            dataWriteStream = null;
        }
    }

    /**
     * Класс для управления файлом конфигурации.
     */
    public class ConfigurationFileManager extends BaseInnerFileManager {
        private VirtualDeviceConfiguration virtualDeviceConfiguration = null;

        ConfigurationFileManager() throws IOException {
            super(Const.CONFIG_FILE_NAME, true);
            readSelf();
        }

        protected void readSelf() throws IOException {
            try {
                FileInputStream configReadStream = new FileInputStream(selfFile);
                virtualDeviceConfiguration = new VirtualDeviceConfiguration(aordFileContext,
                        "virtual", configReadStream);
                configReadStream.close();
            } catch (IllegalStateException e) {
                RadarBox.logger.add(this,
                        "WARNING: Reading null configuration " +
                                "(if it`s on creation of AoRD-file, it is normal)");
            }
        }

        /** Возвращает конфигурацию устройства, считанную из файла.
         * @return null, если файл с конфигурацией не открыт.
         */
        public DeviceConfiguration getVirtual() {
            return virtualDeviceConfiguration;
        }

        /**
         * Запись конфигурации в файл (с удалением предыдущего файла).
         * <br />#enable_danger
         * @param configuration - объект класса {@link DeviceConfiguration}.
         */
        public void write(DeviceConfiguration configuration) {
            try {
                if(RadarBox.device == null)
                    throw new IOException("No device");
                if (!selfFile.delete()) {
                    throw new IOException("Can`t write configuration to file");
                }
                XmlSerializer serializer = Xml.newSerializer();
                FileOutputStream fileWriteStream = new FileOutputStream(selfFile);
                serializer.setOutput(fileWriteStream, "UTF-8");
                serializer.startDocument(null, Boolean.TRUE);
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
                readSelf();
            } catch (IOException e) {
                RadarBox.logger.add(this, e.toString());
                e.printStackTrace();
                close();
            }
        }

        /** Класс-наследник {@link #DeviceConfiguration} для считывания конфигурации устройства
         * из конфигурационного файла */
        private class VirtualDeviceConfiguration extends DeviceConfiguration {

            public VirtualDeviceConfiguration(Context context, String devicePrefix,
                                              InputStream configFileStream) {
                super(context, devicePrefix);

                try {
                    parseConfigurationHeader(configFileStream);
                } catch (XmlPullParserException | IOException e) {
                    RadarBox.logger.add(deviceName + " CONFIG",
                            "ERROR on parseConfiguration: " + e.getLocalizedMessage());
                }
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

    /**
     * Класс для управления файлом статуса.
     */
    public class StatusFileManager extends BaseInnerFileManager {
        StatusFileManager() throws IOException {
            super(Const.STATUS_FILE_NAME, false);
        }

        /**
         * Записывает заголовок файла статуса;
         * @param deviceStatus - статус устройства.
         */
        public void writeHeader(DeviceStatus deviceStatus) {
            ArrayList<String> list = new ArrayList<String>();
            Collections.addAll(list, "FrNum", "Time, ms");
            deviceStatus.getStatusList().forEach(statusEntry -> {
                list.add(statusEntry.getID());
            });
            writeList(list);
        }

        /**
         * Записывает строку статуса в файл.
         * @param frameNumber - номер кадра.
         * @param time - время с начала эксперимента.
         * @param deviceStatus - статус устройства.
         */
        public void write(int frameNumber, long time, DeviceStatus deviceStatus) {
            ArrayList<String> list = new ArrayList<String>();
            Collections.addAll(list, String.valueOf(frameNumber), String.valueOf(time));
            deviceStatus.getStatusList().forEach(statusEntry -> {
                list.add(statusEntry.getValue().toString());
            });
            writeList(list);
        }

        private void writeList(ArrayList<String> list) {
            try {
                CSVWriter writer = new CSVWriter(new FileWriter(selfFile, true),
                        '\t', '"', '=', "\n");
                writer.writeNext(list.toArray(new String[] {}));
                writer.close();
            } catch (IOException e) {
                RadarBox.logger.add(this, e.toString());
                e.printStackTrace();
            }
        }
    }

    /**
     * Класс для управления файлом описания.
     */
    public class DescriptionFileManager extends BaseInnerFileManager {
        private String description = "";

        DescriptionFileManager() throws IOException {
            super(Const.DESC_FILE_NAME, false);
            readSelf();
        }

        protected void readSelf() throws IOException {
            FileReader fileReader = new FileReader(selfFile);
            Scanner reader = new Scanner(fileReader);
            String result = "";
            while (reader.hasNextLine()) {
                result += reader.nextLine() + "\n";
            }
            reader.close();
            fileReader.close();
            if (result.length() >= 1) {
                result = result.substring(0, result.length() - 1);
            }
            description = result;
        }

        /**
         * Возвращает текстовое описание AoRD-файла.
         * @return null, если файл не открыт.
         */
        public String getText() {
            return description;
        }

        /**
         * Запись в файл.
         * @param text - строка, которую нужно записать.
         */
        public void write(String text) {
            try {
                FileWriter writer = new FileWriter(selfFile, false);
                writer.write(text);
                writer.flush();
                description = text;
                RadarBox.logger.add(this, "DEBUG: Description written: " + description);
            } catch (IOException e) {
                RadarBox.logger.add(this, e.toString());
                e.printStackTrace();
            }
        }
    }

    /**
     * Класс для управления файлом и папкой дополнений.
     * Важно: <b>все методы родителя ({@link BaseInnerFileManager}) относятся к zip-файлу дополнений
     * (не к папке, в которую он распакован)</b>.
     */
    public class AdditionalFileManager extends BaseInnerFileManager {
        private File selfFolder = null;
        
        AdditionalFileManager() throws IOException {
            super(Const.ADDITIONAL_ARCH_NAME, false);
            selfFolder = new File(unzipFolder.getAbsolutePath() + "/" +
                    Const.ADDITIONAL_FOLDER_NAME);
            if (!selfFolder.exists() && !selfFolder.mkdir()) {
                throw new IOException("Can`t create dir " + selfFolder.getAbsolutePath());
            }
        }

        /**
         * @return объект {@link File} управляемой папки.
         */
        public File getFolder() {
            return selfFolder;
        }

        /**
         * @return массив имён файлов в управляемой папке.
         */
        public String[] getNamesList() {
            if (selfFolder.list() == null) {
                return new String[] {};
            }
            return selfFolder.list();
        }

        /**
         * @return массив объектов {@link File} в управляемой папке.
         */
        public File[] getFilesList() {
            if (selfFolder.listFiles() == null) {
                return new File[] {};
            }
            return selfFolder.listFiles();
        }

        /**
         * Добавляет файл (не директорию) в управляемую папку.
         * @param newFile - новый файл.
         */
        public void addFile(File newFile) {
            if (newFile.isDirectory()) {
                return;
            }
            RadarBox.logger.add(this, "DEBUG: Adding file " + newFile.getAbsolutePath() +
                    newFile.exists());
            if (!Helpers.copyFile(newFile, Helpers.createUniqueFile(
                    selfFolder.getAbsolutePath() + "/" + newFile.getName()))) {
                RadarBox.logger.add(this, "ERROR: Can`t add file " +
                        newFile.getAbsolutePath() + " to additional folder of AoRD-file " +
                        selfFolder.getAbsolutePath());
            }
        }

        /**
         * Удаляет файл из управляемой папки по имени.
         * @param name - имя файла, который нужно удалить.
         */
        public void deleteFile(String name) {
            if (Arrays.asList(getNamesList()).contains(name)) {
                File fileToDelete = new File(selfFolder.getAbsolutePath() + "/" + name);
                if (!fileToDelete.delete()) {
                    RadarBox.logger.add(this, "ERROR: Can`t delete file " +
                            fileToDelete.getAbsolutePath() +
                            " from additional folder of AoRD-file " +
                            AoRDFile.this.getAbsolutePath());
                }
            }
        }
        
        protected void prepareToCommit() throws IOException {
            if (selfFile.exists()) {
                if (!selfFile.delete()) {
                    throw new IOException("Can`t delete file " + selfFile.getAbsolutePath());
                }
            }
        }

        /**
         * Сохраняет изменения в папке дополнений.
         * <br />#enable_danger
         */
        public void commit() {
            try {
                prepareToCommit();
                selfFile = ZipManager.archiveFolder(selfFolder);
            } catch (IOException e) {
                RadarBox.logger.add(this, e.toString());
                e.printStackTrace();
                close();
            }
        }
    }

    // Help classes
    /**
     * Класс-хранилище констант для работы {@link AoRDFile}.
     */
    private static class Const {
        public static final String CONFIG_FILE_NAME = "config.xml";
        public static final String DATA_FILE_NAME = "radar_data.data";
        public static final String STATUS_FILE_NAME = "status.csv";
        public static final String DESC_FILE_NAME = "description.txt";
        public static final String ADDITIONAL_FOLDER_NAME = "additional";
        public static final String ADDITIONAL_ARCH_NAME = ADDITIONAL_FOLDER_NAME + ".zip";

    }
}
