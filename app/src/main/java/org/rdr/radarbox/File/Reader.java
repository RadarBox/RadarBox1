package org.rdr.radarbox.File;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Parcel;

import org.rdr.radarbox.Device.DeviceConfiguration;
import org.rdr.radarbox.RadarBox;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.InvalidMarkException;
import java.nio.MappedByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

import androidx.preference.PreferenceManager;

/**
 * Класс для чтения данных из файла
 * @author Сапронов Данил Игоревич; Шишмарев Ростислав Иванович
 * @version 0.1
 */
public class Reader {
    private Context context;
    private int fileReadFrameCount;
    private int curReadFrame;
    private final File directoryDocuments;
    private File fileRead = null;
    private MappedByteBuffer fileReadBuffer;
    private ShortBuffer fileReadShortBuffer;
    private DeviceConfiguration virtualDeviceConfiguration = null;

    public Reader(Context context_) {
        context = context_;
        directoryDocuments = context.getExternalFilesDir(Helpers.defaultFolderPath);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String dataReadFilename = pref.getString("file_reader_filename","");
        RadarBox.logger.add(this, dataReadFilename);
        if(!dataReadFilename.isEmpty()) {
            setFileRead(dataReadFilename);
        }
    }

    /** Возвращает конфигурацию устройства, считанную из заголовка файла либо null.
     * @return null, если файл с данными не открыт или в открытом файле отстутствует заголовок с
     * конфигурацией устройства
     */
    public DeviceConfiguration getVirtualDeviceConfiguration() {return virtualDeviceConfiguration;}

    /** Получение списка всех файлов в директории с расширением .data
     * @return перечень файлов с расширением .data, пустой список во всех остальных случаях
     */
    public String[] getFilesList() {
        String[] listOfFiles = new String[]{};
        listOfFiles = directoryDocuments.list((d, s) -> s.toLowerCase().endsWith(".data"));
        if (listOfFiles==null)
            listOfFiles = new String[]{};
        return listOfFiles;
    }

    /** Открытие файла с заданным имененем и парсинг шапки файла
     * @param name - имя файла, включая расширение *.data
     * @return true, если файл успешно открыт
     * и двоичные данные в нём преобразованы для дальнешего считывания.
     */
    public boolean setFileRead(String name) {
        fileRead = new File(directoryDocuments.getPath()+"/"+name);
        if (fileRead.exists()) {
            try {
                readFile();
            } catch (IOException e) {
                RadarBox.logger.add(e.toString());
                fileRead = null;
                e.printStackTrace();
                return false;
            }
            return true;
        }
        else return false;
    }

    /** Анализ заголовка файла и создание виртуальной конфигурации устройства, из которой можно
     * узнать параметры устройства, с которого были записаны данные
     * @throws IOException
     */
    private void readFile() throws IOException {
        try {
            ZipManager zipManager = new ZipManager(new File(Helpers.defaultFolderAbsPath + "/Arch.zip"));
            zipManager.unzipFile();
        } catch (Exception e) {
            RadarBox.logger.add(this,e.getLocalizedMessage()+"\n\tCould not unzip file");
        }
        FileInputStream fileReadStream = new FileInputStream(fileRead);
        // формирование конфигурации устройства из заголовка файла
        virtualDeviceConfiguration = new VirtualDeviceConfiguration(context,
                "virtual", fileReadStream);

        fileReadBuffer = fileReadStream.getChannel()
                .map(FileChannel.MapMode.READ_ONLY,0,fileRead.length());
        boolean isEndOfConfigReached = false;
        while(fileReadBuffer.hasRemaining() && !isEndOfConfigReached) {
            byte[] endOfConfig = ("</config>").getBytes(StandardCharsets.UTF_8);
            for(int i=0; endOfConfig[i]==fileReadBuffer.get(); i++)
                if(i==endOfConfig.length-1) { //найдено ключевое слово
                    isEndOfConfigReached = true; //достигнут конец заголовка файла
                    break;
                }
        }
        fileReadBuffer.get(); //переход на новую строку
        fileReadBuffer.mark(); //отметка, с которой можно начать читать данные типа short
        // создание буфера типа short для удобного считывания данных
        fileReadShortBuffer = fileReadBuffer.order(ByteOrder.BIG_ENDIAN).asShortBuffer();

        // установка отметки в нулевую позицию для возможности в будущем перечитывать данные
        fileReadShortBuffer.mark();
        curReadFrame=0;
    }

    public int getFileReadFrameCount() {
        if (fileReadFrameCount>0)
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
            RadarBox.logger.add(this,e.toString()+"\n\tCouldn't read "+curReadFrame
                    +"-th frame. \n\tFileBuffer position: "+fileReadBuffer.position()
                    +"\n\tReading elements count: "+dest.length+"(int16)"
                    +"\n\tElements remaining in buffer: "+fileReadShortBuffer.remaining()+"(int16)");
            try {
                fileReadShortBuffer.reset();
                curReadFrame=0;

            } catch (InvalidMarkException e2) {
                RadarBox.logger.add(this,e2.getLocalizedMessage()+"\n\tCouldn't reload file");
                //RadarBox.mainDataThread.extraStop();
            }
        }
    }

    /** Текущий открытый файл для чтения радиолокационных данных.
     * @return null, если открытого для чтения данных файла не существует.
     */
    public final File getFileRead() {return fileRead;}

    /** Класс наследник {@link #DeviceConfiguration} для считывания конфигурации устройства
     * из заголовка файла c данными вместо конфигурационного файла устройства */
    class VirtualDeviceConfiguration extends DeviceConfiguration {

        public VirtualDeviceConfiguration(Context context, String devicePrefix, InputStream configFileStream) {
            super(context, devicePrefix);

            if(fileRead!=null) {
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
