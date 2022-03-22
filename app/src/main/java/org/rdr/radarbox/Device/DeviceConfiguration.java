package org.rdr.radarbox.Device;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

/** Класс с гибкими настройками радара, которые изменяются с помощью соответствующих
 * комманд управления. Данный класс является полем общего класса изделия, наследуемого от Device.
 * @author Сапронов Данил Игоревич
 * @version 1.0
 */
public abstract class DeviceConfiguration {
    protected String deviceName, devicePrefix;
    public String getDeviceName() {return deviceName;}
    public String getDevicePrefix() {return devicePrefix;}

    // Неизменяемые параметры устройства
    protected int rxN; // общее количество приёмников для данного устройства
    protected int txN; // общее количество передатчиков для данного устройства
    protected boolean isComplex; //является ли один отсчёт комплексным или нет
    DeviceRawDataAdapter.DimensionOrder dimensionOrder;

    /** Общее количество приёмников изделия
     *  @return количество приёмников изделия
     */
    public int getRxN() {return rxN;}

    /** Общее количество передатчиков изделия
     *  @return количество передатчиков изделия
     */
    public int getTxN() {return txN;}
    /** Являются ли цифровые отсчёты комплексными (состоящими из двух компонент) или нет */
    public boolean getIsComplex() {return isComplex;}

    /** <p>Порядок сбора данных в текущем устройстве.</p>
     * Например, сначала собираются отсчёты по всем примникам, потом для всех частотных/временных
     * точек, потом по всем передатчикам (RX_TF_TX).
     * @return элемент enum {@link DeviceRawDataAdapter.DimensionOrder},
     * определяющий порядок сбора данных
     */
    public DeviceRawDataAdapter.DimensionOrder getDimensionOrder() {return dimensionOrder;}

    protected final MutableLiveData<Integer> liveTfCount = new MutableLiveData<>();
    /** @return текущее количество отсчётов по времени/частоте для одного канала */
    public LiveData<Integer> getLiveTfCount() {return liveTfCount;}
    public boolean setTfCount(int value) {
        if(value<0) return false;
        liveTfCount.postValue(value);
        return true;
    }

    protected final MutableLiveData<Integer> liveRxEnabled = new MutableLiveData<>();
    /** @return текущее количество работающих приёмников */
    public LiveData<Integer> getLiveRxEnabled() {return liveRxEnabled;}
    public boolean setRxEnabled(int value) {
        if(value<0 || value>this.rxN) return false;
        liveRxEnabled.postValue(value);
        return true;
    }

    protected final MutableLiveData<Integer> liveTxEnabled = new MutableLiveData<>();
    /** @return текущее количество работающих передатчиков */
    public LiveData<Integer> getLiveTxEnabled() {return liveTxEnabled;}
    public boolean setTxEnabled(int value) {
        if(value<0 || value>this.txN) return false;
        liveTxEnabled.postValue(value);
        return true;
    }

    protected int[] rxtxOrder;
    protected final MutableLiveData<int[]> liveRxtxOrder = new MutableLiveData<>();
    /** <p>Матрица переключений каналов приёма-передачи.</p>
     * <p>1 измерение - приёмники, 2 - передатчики.</p>
     * <p>Пример:</p>
     * <p>0 2 1 3</p>
     * Означает, что сначала собираются данные для 2-передатчика и 1-приёмника, затем 1-прд и 2-прм,
     * затем 2-прд, 2-прм, при этом, отсутствуют данные для 1-прд, 1-прм.
     * Таким образом, 0 в некоторой позиции означает, что данные для такой конфигурации отстутсвуют.
     *  @return матрица переключений каналов приёма-передачи
     */
    public LiveData<int[]> getLiveRxtxOrder () {return liveRxtxOrder;}
    public int[] getRxtxOrder() {return rxtxOrder;}

    /** Установка нового порядка сбора данных по каналам приёма-передачи
     * @param order массив размера {@link #rxN}*{@link #txN}
     * @return false, если размер {@param order} не соответствует размеру {@link #rxtxOrder},
     * либо, если числа в {@param order} некорректные
     */
    public boolean setRxtxOrder(int[] order) {
        if (order.length != rxtxOrder.length)
            return false;
        for (int j : order) {
            if (j < 0 || j > rxN * txN)
                return false;
        }
        rxtxOrder=order;
        liveRxtxOrder.postValue(rxtxOrder);
        return true;
    }

    protected ArrayList<Parameter> parameters; // список параметров, получаемый из .xml файла
    public ArrayList<Parameter> getParameters() {return parameters;}
    SharedPreferences pref; /* настройки приложения для чтения и записи актуальных значений
    парамеров (ключевое слово настройки каждого параметра представляет собой devicePrefix+id) */

    public DeviceConfiguration (Context context, String devicePrefix){
        this.devicePrefix = devicePrefix;
        pref = PreferenceManager.getDefaultSharedPreferences(context);
        liveTfCount.setValue(1);
        liveTxEnabled.setValue(1);
        liveRxEnabled.setValue(1);
    }

    public class Parameter <T> {
        protected final String id;        public String getID() {return id;}
        protected final String name;      public String getName() {return name;}
        protected final String summary;   public String getSummary() {return summary;}
        protected final T def;            public T getDefault() {return def;}
        public T getValue() {return liveValue.getValue();}
        public boolean setValue(T value) {liveValue.postValue(value); return true;}
        protected MutableLiveData<T> liveValue = new MutableLiveData<>();
        public LiveData<T> getLiveValue() {return liveValue;}
        public Parameter(String id, String name, String summary, T def) {
            this.id = id; this.name=name; this.summary=summary; this.def=def;
            liveValue.setValue(def);
        }
    }

    public class BooleanParameter extends Parameter<Boolean> {
        public BooleanParameter(String id, String name, String summary, Boolean def) {
            super(id, name, summary, def);
            liveValue.setValue(pref.getBoolean(devicePrefix+this.id,def));
        }

        @Override
        public boolean setValue(Boolean value) {
            liveValue.postValue(value);
            return pref.edit().putBoolean(devicePrefix+this.id,value).commit();
        }
    }

    public class IntegerParameter extends Parameter<Integer> {
        private final int min, step, max, radix;
        public int getMin() {return min;}
        public int getStep() {return step;}
        public int getMax() {return max;}
        public int getRadix() {return radix;}

        @Override
        public boolean setValue(Integer value) {
            if(value<min || value>max || ((value-min)%step!=0))
                return false;
            liveValue.postValue(value);
            return pref.edit().putInt(devicePrefix+this.id,value).commit();
        }

        public IntegerParameter(String id, String name, String summary, Integer def,
                                int min, int step, int max, int radix) {
            super(id, name, summary, def);
            this.min=min; this.max=max; this.step=step; this.radix=radix;
            liveValue.setValue(pref.getInt(devicePrefix+this.id,def));
        }
    }

    /** Прочесть конфигурацию устройства из входного потока
     *
     * @param in поток данных
     * @return количество прочитанных строк конфигурации
     * @throws XmlPullParserException
     * @throws IOException
     */
    protected int parseConfiguration(InputStream in) throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL,false);
        parser.setInput(in, null);
        parser.nextTag();

        parameters = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "device":
                    readDeviceConfiguration(parser);
                    break;
                case "integer_parameter":
                    parameters.add(readIntegerParameter(parser));
                    break;
                case "boolean_parameter":
                    parameters.add(readBooleanParameter(parser));
                    break;
            }
            parser.next();
        }
        return parser.getLineNumber();
    }

    protected void readDeviceConfiguration(XmlPullParser parser) {
        this.deviceName = parser.getAttributeValue(null,"name");
        this.rxN = Integer.parseInt(parser.getAttributeValue(null,"rxN"));
        this.txN = Integer.parseInt(parser.getAttributeValue(null,"txN"));
        String rxtxOrderString = parser.getAttributeValue(null, "rxtxOrder");
        String[] rxtxOrderStringArray = rxtxOrderString.split(",");
        int[] rxtxOrder = new int[rxtxOrderStringArray.length];
        for(int i=0; i<rxtxOrder.length; i++)
            rxtxOrder[i]=Integer.parseInt(rxtxOrderStringArray[i].trim());
        this.liveRxtxOrder.setValue(rxtxOrder);
        String dimensionOrderString = parser.getAttributeValue(null, "dimensionOrder");
        if(dimensionOrderString==null)
            this.dimensionOrder = DeviceRawDataAdapter.DimensionOrder.TF_RX_TX;
        else
            this.dimensionOrder = DeviceRawDataAdapter.DimensionOrder.valueOf(dimensionOrderString);
        this.isComplex = Boolean.parseBoolean(parser.getAttributeValue(null,"isComplex"));
    }

    public static void writeDeviceConfiguration(XmlSerializer serializer, DeviceConfiguration config) throws IOException {
        serializer.startTag(null,"device");
        serializer.attribute(null,"name",config.getDeviceName());
        serializer.attribute(null,"rxN",Integer.toString(config.getRxN()));
        serializer.attribute(null,"txN",Integer.toString(config.getTxN()));
        serializer.attribute(null,"rxtxOrder",
                Arrays.toString(config.getRxtxOrder()).replaceAll("[\\[\\]]",""));
        serializer.attribute(null,"dimensionOrder",config.getDimensionOrder().toString());
        serializer.attribute(null,"isComplex",Boolean.toString(config.getIsComplex()));
        serializer.endTag(null,"device");
    }

    protected BooleanParameter readBooleanParameter(XmlPullParser parser) {
        String id = parser.getAttributeValue(null,"id");
        String name = parser.getAttributeValue(null,"name");
        String summary = parser.getAttributeValue(null,"summary");
        if(summary==null) summary="";
        boolean def = Boolean.parseBoolean(parser.getAttributeValue(null,"def"));
        return new BooleanParameter(id,name,summary,def);
    }

    public static void writeBooleanParameter(XmlSerializer serializer, BooleanParameter parameter) throws IOException {
        serializer.startTag(null,"boolean_parameter");
        serializer.attribute(null,"id",parameter.getID());
        serializer.attribute(null,"name",parameter.getName());
        serializer.attribute(null,"value",parameter.getValue().toString());
        if(parameter.getSummary()!=null)
            serializer.attribute(null,"summary",parameter.getSummary());
        serializer.attribute(null,"def",parameter.getDefault().toString());
        serializer.endTag(null,"boolean_parameter");
    }

    protected IntegerParameter readIntegerParameter(XmlPullParser parser) {
        String id = parser.getAttributeValue(null,"id");
        String name = parser.getAttributeValue(null,"name");
        String summary = parser.getAttributeValue(null,"summary");
        if(summary==null) summary="";
        int radix = 10;
        String radixStr = parser.getAttributeValue(null, "radix");
        if(radixStr!=null) radix=Integer.parseInt(radixStr);
        int def = Integer.parseInt(parser.getAttributeValue(null,"def"),radix);
        int min = Integer.parseInt(parser.getAttributeValue(null,"min"),radix);
        int step = Integer.parseInt(parser.getAttributeValue(null,"step"),radix);
        int max = Integer.parseInt(parser.getAttributeValue(null,"max"),radix);
        return new IntegerParameter(id,name,summary,def,min,step,max,radix);
    }

    public static void writeIntegerParameter(XmlSerializer serializer, IntegerParameter parameter) throws IOException {
        serializer.startTag(null,"integer_parameter");
        serializer.attribute(null,"id",parameter.getID());
        serializer.attribute(null,"name",parameter.getName());
        if(parameter.getRadix()!=10)
            serializer.attribute(null,"radix",Integer.toString(parameter.getRadix()));
        serializer.attribute(null,"value",Integer.toString(parameter.getValue(), parameter.getRadix()));
        if(parameter.getSummary()!=null)
            serializer.attribute(null,"summary",parameter.getSummary());
        serializer.attribute(null,"def",Integer.toString(parameter.getDefault(), parameter.getRadix()));
        serializer.attribute(null,"min",Integer.toString(parameter.getMin(), parameter.getRadix()));
        serializer.attribute(null,"step",Integer.toString(parameter.getStep(), parameter.getRadix()));
        serializer.attribute(null,"max",Integer.toString(parameter.getMax(), parameter.getRadix()));
        serializer.endTag(null,"integer_parameter");
    }

    /** Метод ищет параметр с заданным ID в списке целочисленных параметров
     * и возвращает его текущее значение.
     *
     * @param parameterID - ID необходимого параметра
     * @return значение параметра с заданным ID.
     * В случае отстутствия такого параметра возвращает -1.
     */
    public int getIntParameterValue(String parameterID) {
        final int[] i = {-1};
        this.getParameters().stream().filter(parameter -> parameter.getID().equals(parameterID))
                .findAny().ifPresent(param -> {
            i[0] = ((DeviceConfiguration.IntegerParameter) param).getValue();
        });
        return i[0];
    }

    /** Метод задаёт значение целочисленного параметра с заданным ID.
     * Если такого параметра нет, то ничего не происходит.
     *
     * @param parameterID - ID необходимого параметра
     * @param value - новое значение параметра
     */
    public void setIntParameterValue(String parameterID, int value) {
        this.getParameters().stream().filter(parameter -> parameter.getID().equals(parameterID))
                .findAny().ifPresent(param -> {
            ((DeviceConfiguration.IntegerParameter) param).setValue(value);
        });
    }

    /** Метод ищет параметр с заданным ID в списке булевых параметров
     * и возвращает его текущее значение. ! НЕТ ПРОВЕРКИ НА ОШИБКУ В ID !
     *
     * @param parameterID - ID необходимого параметра
     * @return значение параметра с заданным ID.
     * В случае отстутствия такого параметра возвращает -1.
     */
    public boolean getBoolParameterValue(String parameterID) {
        final boolean[] i = {false};
        this.getParameters().stream().filter(parameter -> parameter.getID().equals(parameterID))
                .findAny().ifPresent(param -> {
            i[0] = ((DeviceConfiguration.BooleanParameter) param).getValue();
        });
        return i[0];
    }

    /** Метод задаёт значение булевого параметра с заданным ID.
     * Если такого параметра нет, то ничего не происходит.
     *
     * @param parameterID - ID необходимого параметра
     * @param value - новое значение параметра
     */
    public void setBoolParameterValue(String parameterID, boolean value) {
        this.getParameters().stream().filter(parameter -> parameter.getID().equals(parameterID))
                .findAny().ifPresent(param -> {
            ((DeviceConfiguration.BooleanParameter) param).setValue(value);
        });
    }

}
