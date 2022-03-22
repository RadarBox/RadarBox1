package org.rdr.radarbox.Device;

import android.content.Context;
import android.util.Xml;

import org.rdr.radarbox.RadarBox;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/** Класс для получения информации о текущем состоянии устойства:
 * температуре, уровне заряда аккумулятора, ориентации и т.д.
 */
public class DeviceStatus {
    /** LiveData статус радара в общем виде представляется в виде списка из строковых пар
     * <имя, значение> для отображения в меню и обработки в других классах
     */
    InputStream fileStatus;
    protected ArrayList<StatusEntry> statusList;

    public ArrayList<StatusEntry> getStatusList() {
        return statusList;
    }

    public DeviceStatus(Context context, String devicePrefix) {
        try {
            parseStatusFile(context.getAssets()
                    .open(devicePrefix+"/status.xml"));
        } catch (XmlPullParserException | IOException e) {
            RadarBox.logger.add(devicePrefix+" STATUS",
                    "parseStatusFile ERROR: "+e.getLocalizedMessage());
        }
    }

    public class StatusEntry <T> {
        protected final String id;  public String getID() {return id;}
        protected final String name; public String getName() {return name;}
        protected T value;
        public void setValue(T value) {this.value = value;}
        public T getValue() {return this.value;}
        public StatusEntry(String id, String name) {
            this.id=id; this.name=name;
        }
    }

    public class SimpleStatusEntry <T> extends StatusEntry<T> {
        protected final String summary;   public String getSummary() {return summary;}

        public SimpleStatusEntry(String id, String name, String summary) {
            super(id, name);
            this.summary=summary;
        }
    }

    public class IntegerStatusEntry extends SimpleStatusEntry<Integer> {
        public IntegerStatusEntry(String id, String name, String summary) {
            super(id, name, summary);
            value=0;
        }
    }

    public class FloatStatusEntry extends SimpleStatusEntry<Float> {
        private final float coef;        public float getCoef() {return coef;}
        public FloatStatusEntry(String id, String name, String summary, float coef) {
            super(id, name, summary);
            this.coef=coef;
            value=0F;
        }
    }

    /** Класс для сложного статусного слова, который состоит из нескольких битовых параметров.
     * В объекте класса создаётся список битовых параметров, к которым можно обратиться по
     * уникальным {@link Bit#id}, а значения битовых параметров возвращаются с помощью функции
     * {@link Bit#getBitVal()}.
     */
    public class ComplexStatusEntry extends StatusEntry<Integer> {
        public ComplexStatusEntry(String id, String name, ArrayList<Bit> bits) {
            super(id,name);
            this.bits=bits;
            this.value=0;
        }
        public class Bit {
            private final String id;        public String bitID() {return id;}
            private final String name;      public String bitName() {return name;}
            private final String summary;   public String bitSummary() {return summary;}
            private final String mask;
            private final int shift;

            /** Функция возвращает значение битового параметра в сложном статусе в соответствии
             * с заданной битовой маской
             * @return значение битового параметра
             */
            public int getBitVal() {
                return (value & Integer.parseInt(mask, 2))>>shift;
            }
            public Bit(String id, String name, String summary, String mask) {
                this.id=id; this.name=name; this.summary=summary; this.mask=mask;
                int numberOfZeros = 0;
                for(int i=0; mask.charAt(mask.length()-i-1)=='0' && i<mask.length(); i++)
                    numberOfZeros++;
                shift=numberOfZeros;
            }
        }
        protected final ArrayList<Bit> bits; public ArrayList<Bit> getBits() {return bits;}
    }

    protected void parseStatusFile(InputStream in) throws XmlPullParserException, IOException {
        fileStatus = in;
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(in, null);
        parser.nextTag();

        statusList = new ArrayList();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "simple":
                    statusList.add(readSimpleStatus(parser));
                    break;
                case "complex":
                    statusList.add(readComplexStatus(parser));
                    break;
            }
            parser.next();
        }
    }

    private SimpleStatusEntry readSimpleStatus(XmlPullParser parser) {
        String id = parser.getAttributeValue(null,"id");
        String name = parser.getAttributeValue(null,"name");
        String summary = parser.getAttributeValue(null,"summary");
        if(summary==null) summary="";
        if(parser.getAttributeValue(null,"coef")!=null) {
            float coef = Float.parseFloat(parser.getAttributeValue(null,"coef"));
            return new FloatStatusEntry(id,name,summary,coef);
        }
        return new IntegerStatusEntry(id,name,summary);
    }

    private ComplexStatusEntry readComplexStatus(XmlPullParser parser) throws IOException, XmlPullParserException {
        String id = parser.getAttributeValue(null,"id");
        String name = parser.getAttributeValue(null,"name");
        ArrayList<ComplexStatusEntry.Bit> bits = new ArrayList<>();
        ComplexStatusEntry complexStatusEntry = new ComplexStatusEntry(id,name,bits);

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String bitId = parser.getAttributeValue(null,"id");
            String bitName = parser.getAttributeValue(null,"name");
            if(bitName==null) bitName=bitId;
            String bitSummary = parser.getAttributeValue(null,"summary");
            if(bitSummary==null) bitSummary="";
            String bitMask = parser.getAttributeValue(null,"mask");
            bits.add(complexStatusEntry. new Bit(bitId,bitName,bitSummary,bitMask));
            parser.next();
        }
        return complexStatusEntry;
    }

    /** Метод ищет простой статус с заданным ID в статусном списке
     * и возвращает его текущее значение. ! НЕТ ПРОВЕРКИ НА ОШИБКУ В ID !
     *
     * @param statusID - ID необходимого показателя состояния (статуса)
     * @return значение статуса с заданным ID.
     * В случае отстутствия такого ID возвращает -1.
     */
    public int getIntStatusValue(String statusID) {
        int []ret = {-1};
        this.getStatusList().stream().filter(status -> status.getID().equals(statusID))
                .findAny().ifPresent(statusEntry -> {
            ret[0] = ((DeviceStatus.IntegerStatusEntry) statusEntry).getValue();
        });
        return ret[0];
    }

    /** Метод задаёт значение целочисленного показателя состояния с заданным ID.
     * Если такого ID нет, то ничего не происходит.
     *
     * @param statusID - ID необходимого показателя состояния (статуса)
     * @param value - новое значение показателя состояния
     */
    public void setIntStatusValue(String statusID, int value) {
        this.getStatusList().stream().filter(status -> status.getID().equals(statusID))
                .findAny().ifPresent(status -> {
            ((DeviceStatus.StatusEntry<Integer>) status).setValue(value);
        });
    }

    /** Метод ищет показатель статуса с плавающей запятой с заданным ID в статусном списке
     * и возвращает его текущее значение. ! НЕТ ПРОВЕРКИ НА ОШИБКУ В ID !
     *
     * @param statusID - ID необходимого показателя состояния (статуса)
     * @return значение статуса с заданным ID.
     * В случае отстутствия такого ID возвращает -1.
     */
    public float getFloatStatusValue(String statusID) {
        float []ret = {-1};
        this.getStatusList().stream().filter(status -> status.getID().equals(statusID))
                .findAny().ifPresent(statusEntry -> {
            ret[0] = ((DeviceStatus.FloatStatusEntry) statusEntry).getValue();
        });
        return ret[0];
    }

    /** Метод задаёт значение показателя состояния с плавающей запятой с заданным ID.
     * Если такого ID нет, то ничего не происходит. Для показателей с плавающей запятой характерно
     * наличие коэффициента пересчёта из целочисленного кода, получаемого по протоколу связи
     * с устройством. Поэтому, здесь второй аргумент типа int умножается на коэффициент.
     * Для присвоения значения напрямую необходимо использовать функцию
     * {@link #setFloatStatusValue(String, float)} }
     *
     * @param statusID - ID необходимого показателя состояния (статуса)
     * @param code - новое значение показателя состояния
     */
    public void setFloatStatusValueByCode(String statusID, int code) {
        this.getStatusList().stream().filter(status -> status.getID().equals(statusID))
                .findAny().ifPresent(status -> {
            ((DeviceStatus.FloatStatusEntry) status).setValue(code*
                    ((DeviceStatus.FloatStatusEntry) status).getCoef());
        });
    }

    /** Метод задаёт значение показателя состояния с плавающей запятой с заданным ID.
     * Если такого ID нет, то ничего не происходит.
     *
     * @param statusID - ID необходимого показателя состояния (статуса)
     * @param value - новое значение показателя состояния
     */
    public void setFloatStatusValue(String statusID, float value) {
        this.getStatusList().stream().filter(status -> status.getID().equals(statusID))
                .findAny().ifPresent(status -> {
            ((DeviceStatus.FloatStatusEntry) status).setValue(value);
        });
    }

}
