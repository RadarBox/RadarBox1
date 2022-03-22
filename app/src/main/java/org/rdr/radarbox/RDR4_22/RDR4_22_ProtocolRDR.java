package org.rdr.radarbox.RDR4_22;

import org.rdr.radarbox.Device.DeviceCommunication;
import org.rdr.radarbox.Device.DeviceConfiguration;
import org.rdr.radarbox.Device.DeviceProtocolRDR;
import org.rdr.radarbox.Device.DeviceStatus;
import org.rdr.radarbox.Logger;
import org.rdr.radarbox.RadarBox;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.stream.IntStream;

public class RDR4_22_ProtocolRDR extends DeviceProtocolRDR {
    public RDR4_22_ProtocolRDR(DeviceCommunication channel, DeviceConfiguration config,
                               DeviceStatus status) {
        super(channel, config, status);
    }

    /** <p> Команда Cmd9=0x96 установка параметров управления частотой.</p>
     * <p>– байт Cmd9: 0x96;</p>
     * <p>– байт Cpar: 0x01;</p>
     * <p>– 4 байта параметров Prm0..Prm3:</p>
     * <p>Prm1:Prm0.10..0 = M–1, M – число точек (размер) частотной выборки, начальное значение для Rdr4_22 – 350;</p>
     * <p>Prm1.7..3 = delta=0..31 – шаг приращения коэффициента деления, начальное значение равно 2; для точки частотной выборки с номером i=0..M–1 коэффициент деления D = Dmin + i*delta, при этом фактическая частота синтезатора F = Fp*D, где Fp=2 МГц – частота фазового детектора;</p>
     * <p>Prm3:Prm2.11..0 = Dmin – минимальный коэффициент деления синтезатора (приводящий к частоте фазового детектора Fp=2 МГц), начальное значение для Rdr4_22 – 700 (1400 МГц);</p>
     * <p>Prm3.4 – зарезервирован;</p>
     * <p>Prm3.5 – зарезервирован;</p>
     * <p>Prm3.7,6 = log2(nss) – показатель коэффициента накопления данных, начальное значение равно 0; коэффициент накопления nss = 1, 2, 4 или 8 – количество суммирований при усреднении данных 16-битовых АЦП в каждом канале, причем запуск АЦП производится с очень малыми паузами, поэтому подавляются только некоррелированные помехи, например, ошибки квантования;</p>
     * <p>– байт контрольной суммы (кроме USB).</p>
     * <p>Значения зарезервированных битов здесь игнорируются, но в целях совместимости их следует устанавливать в 0.</p>
     * <p>При режиме с разрешенной внешней синхронизацией команда отвергается с выдачей NACK; в противном случае при правильном формате устройство возвращает код подтверждения Cmd9, параметры запоминаются и используются в дальнейшем для всех сеансов сбора данных.</p>
     * @return true, если полученное в ответ сообщение соответствует коду команды
     */
    public boolean sendCommand9() {
        short M = (short)(configuration.getIntParameterValue("FN")-1);
        byte delta = (byte)(configuration.getIntParameterValue("dF")/2);
        short Dmin = (short)(configuration.getIntParameterValue("F0")/2);
        byte acc = (byte)(configuration.getIntParameterValue("acc_coef"));

        byte[] pars = new byte[4];
        pars[0] = (byte) (M & 0xff);
        pars[1] = (byte) (((M>>8)& 0x07) | (delta<<3));
        pars[2] = (byte) (Dmin);
        pars[3] = (byte) ((Dmin>>8 & 0x0f) | (acc<<6));

        if(!sendLongCommand(9, 0, pars, 500)) {
            log.add("RDR","cmd9 execution: ERROR on sending command");
            return false;
        }
        if(!recvShortResponse(9,500)) return false;
        return configuration.setTfCount(M+1);
    }

    /**<p> Команда Cmd8=0x87 установка конфигурационных параметров.</p>
     * <p>– байт Cmd8: 0x87;</p>
     * <p>– байт Cpar: 0x01;</p>
     * <p>– 4 байта параметров Prm0..Prm3:</p>
     * <p>Prm0.1,0 – режим излучения:</p>
     * <p>00 – используется только излучатель Tr0,</p>
     * <p>01 – используется только излучатель Tr1,</p>
     * <p>1x – используются оба излучателя, сначала Tr0, затем Tr1.</p>
     * <p>Начальное значение Prm0.1,0=2;</p>
     * <p>Prm0.3,2 – режим приема:</p>
     * <p>00 – используется только один канал: Rd0 при Tr0, Rd1 при Tr1,</p>
     * <p>01 – используется только один канал: Rd1 при Tr0, Rd0 при Tr1,</p>
     * <p>1x – используются оба канала: Rd0+Rd1.</p>
     * <p>Начальное значение Prm0.3,2=2;</p>
     * <p>Prm0.7..4 – зарезервировано;</p>
     * <p>Prm1.7 – зарезервировано;</p>
     * <p>Prm1.6..0=t_paus [мксек] – пауза после установки частоты синтезатора, начальное значение равно 20; рекомендуется увеличить минимум до 40;</p>
     * <p>Prm2.2..0 – не используется (зарезервировано), следует устанавливать в 0;</p>
     * <p>Prm2.3 – запрет изменения выходного тока фазового детектора CurSet1; при Prm2.3=0 с ростом частоты CurSet1 постепенно увеличивается (до 5 ступеней) от установленного значения (см. ниже), но не более максимального CurSet1=7, что позволяет скомпенсировать уменьшение крутизны регулировочной характеристики и улучшить форму спектра, но чревато потерей устойчивости системы ФАП синтезатора;</p>
     * <p>Prm2.6..4 – код управления MUX синтезатора частоты ADF4106, начальное значение – 0b001 (Lock detect); параметр используется для отладки, при сборе данных устанавливается 0b001;</p>
     * <p>Prm2.7 – значение входа ISEL смесителя LTC5551: значение 1 уменьшает потребление на четверть, но сужает диапазон; начальное значение – 0;</p>
     * <p>Prm3 – параметры синтезатора ADF4106:</p>
     * <p>     Prm3.7,6 – разрешение и выбор режима FastLock, Prm3.5..3=CurSet2, Prm3.2..0=CurSet1;</p>
     * <p>     начальное значение Prm3=0xDA; рекомендуемое значение Prm3:Prm2=0xDA10;</p>
     * <p>– байт контрольной суммы (кроме USB).</p>
     * <p>Значения зарезервированных битов здесь игнорируются, но в целях совместимости их следует устанавливать в 0.</p>
     * <p>При режиме с разрешенной внешней синхронизацией команда отвергается с выдачей NACK;</p>
     * <p>в противном случае при правильном формате устройство возвращает 4-байтовый блок:</p>
     * <p>– байт Cmd8: 0x87;</p>
     * <p>– байт напряжения питания (аккумулятора) EP = 0..255, Up [В] = KPE*EP, где	KPE =6.6/256.0;</p>
     * <p>– байт температуры микроконтроллера TC [°C] = –128..+127;</p>
     * <p>– байт контрольной суммы (кроме USB).</p>
     * @return true, если полученное в ответ сообщение соответствует коду команды
     */
    public boolean sendCommand8() {
        byte[] pars = new byte[4];
        int tx_mode = configuration.getIntParameterValue("tx_mode")&3;
        int rx_mode = configuration.getIntParameterValue("rx_mode")&3;
        pars[0] = (byte) (tx_mode | (rx_mode << 2));
        pars[1] = (byte)(configuration.getIntParameterValue("t_paus") & 0x7f);
        int curSet1 = 0; if(configuration.getBoolParameterValue("CurSet1")) curSet1=1;
        int ISEL = 0; if(configuration.getBoolParameterValue("ISEL")) ISEL=1;
        pars[2] = (byte)(curSet1<<3 | configuration.getIntParameterValue("MUX")<<4 | ISEL<<7);
        pars[3] = (byte)(configuration.getIntParameterValue("ADF4106"));
        if(!sendLongCommand(8, 0, pars, 500)) {
            log.add("RDR","cmd8 execution: ERROR on sending command");
            return false;
        }
        if(!recvShortResponse(8,500)) return false;

        byte[] ans;
        if(channel.getLiveConnectedChannel().getValue().getName().equals("USB"))
            ans = new byte[2];
        else
            ans = new byte[3];
        if(!channel.recv(ans,500)) return false;
        log.add("RDR","cmd8 resp:("+ Logger.toHexString(ans)+
                ")\tVoltage:("+String.format("%3.2f",0.1418*(ans[0]+128))+")V\tMCUtemp:("+(int)ans[1]+")°C");
        status.setFloatStatusValueByCode("EP",ans[0]);
        status.setIntStatusValue("Tmcu",ans[1]);

        if(!channel.getLiveConnectedChannel().getValue().getName().equals("USB")) {
            log.add("RDR", "CSUM=(" + (byte)((byte) 0x87 + ans[0] + ans[1] + ans[2]) + ")");
        }
        int[] RxTxOrder = {1,2,3,4};
        if(tx_mode==0) {
            configuration.setTxEnabled(1);
            if(rx_mode==0) {RxTxOrder = IntStream.of(1,0,0,0).toArray(); configuration.setRxEnabled(1);}
            if(rx_mode==1) {RxTxOrder = IntStream.of(0,1,0,0).toArray(); configuration.setRxEnabled(1);}
            if(rx_mode>=2) {RxTxOrder = IntStream.of(1,2,0,0).toArray(); configuration.setRxEnabled(2);}
        } else if(tx_mode==1) {
            configuration.setTxEnabled(1);
            if(rx_mode==0) {RxTxOrder = IntStream.of(0,0,1,0).toArray(); configuration.setRxEnabled(1);}
            if(rx_mode==1) {RxTxOrder = IntStream.of(0,0,0,1).toArray(); configuration.setRxEnabled(1);}
            if(rx_mode>=2) {RxTxOrder = IntStream.of(0,0,1,2).toArray(); configuration.setRxEnabled(2);}
        } else {
            configuration.setTxEnabled(2);
            if(rx_mode==0) {RxTxOrder = IntStream.of(1,0,0,2).toArray(); configuration.setRxEnabled(1);}
            if(rx_mode==1) {RxTxOrder = IntStream.of(0,1,2,0).toArray(); configuration.setRxEnabled(1);}
            if(rx_mode>=2) configuration.setRxEnabled(2);
        }
        configuration.setRxtxOrder(RxTxOrder);
        return true;
    }

    /**
     * Cmd11 – конфигурация канала WiFi.
     * – байт Cmd11: 0xb4;
     * – байт Cpar: 0x01;
     * – 4 байта параметров Prm0..Prm3;
     * – байт контрольной суммы (кроме USB).
     * Служебная команда, задающая параметры WiFi: имя сети и пароль, сетевые адреса, протокол, скорость обмена и др. Не может быть выполнена с помощью канала WiFi – отвергается с возвратом NACK.
     * Параметр Prm0 содержит код конфигурационной операции, Prm1..Prm3 зарезервированы и должны устанавливаться в 0. Пользователь не должен устанавливать значения Prm0, отличные от 0 (выключение питания модуля WiFi) или 1 (включение питания модуля WiFi) во избежание нарушения установленной конфигурации.
     * При задании не поддерживаемого значения Prm0 или ошибке формата команды устройство возвращает NACK, иначе – ACK.
     * @param turnOnWiFi true, если нужно включить Wi-Fi модуль
     * @return true, если команда выполнена удачно
     */
    public boolean sendCommand11(boolean turnOnWiFi) {
        byte[] pars = new byte[4];
        if(turnOnWiFi) pars[0]=(byte)1;

        if (sendLongCommand(11,0, pars, 500)) {
            if(RadarBox.device.protocolRDR.recvShortResponse(11, 500)){
                log.add("RDR","CMD11 SUCCESS: WiFi adapter state is "+turnOnWiFi);
                return true;
            }
            else {
                log.add("RDR","CMD11 ERROR: WiFi adapter state is "+!turnOnWiFi);
                return false;
            }
        }
        log.add("RDR","cmd11 execution: ERROR on sending command");
        return false;
    }

    /** Команда запускает процесс передачи, если есть хотя бы один готовый блок данных,
     * сформированный по сигналу внешней синхронизации. В конфигурации без внешней синхронизации
     * процедура сбора данных запускается данной командой, а по ее завершении блок данных
     * немедленно передается Host. Если по какой-либо причине блок данных не будет сформирован
     * в течение 0.8 сек с момента приема Cmd1, возвращается NACK=0xFE,
     * обработка команды заканчивается. Объем и содержание блока радиолокационных данных
     * задается параметрами, устанавливаемыми специальными длинными командами (см. Cmd8,9).
     * @param dest массив, в который будут записаны данные
     * @return true, если очередной кадр данных успешно принят
     */
    public boolean sendCommand1(short[] dest) {
        // размер одного фрейма в байтах
        int frameSizeShorts = RadarBox.freqSignals.getFrameSize();
        int frameSizeBytes = frameSizeShorts*2;

        if(sendShortCommand(1,500)) {
            if (!recvShortResponse(1, 800)) return false;
            byte[] tempArray; // размер принимаемого массива данных зависит от канала
            // Количество передающих каналов определяет количество 8-байтовых заголовков
            int tx_count = 1;
            if((configuration.getIntParameterValue("tx_mode")>>1)==1)
                tx_count = 2;

            if (channel.getLiveConnectedChannel().getValue().getName().equals("USB"))
                tempArray = new byte[frameSizeBytes +8*tx_count];
            else // + байт контрольной суммы (если не USB)
                tempArray = new byte[frameSizeBytes +8*tx_count+1];
            if (!channel.recv(tempArray, 500)) return false;
            // чтение заголовков перед каждым передатчиком
            for(int tx=0; tx<tx_count; tx++) {
                if (!parseStatusBytes(Arrays.copyOfRange(tempArray,
                        tx*(frameSizeBytes/tx_count+8),
                        tx*(frameSizeBytes/tx_count+8)+8))) {
                    log.add(this,"Error on parsingStatusBytes() for ("+tx+") transmitter");
                    return false;
                }
            }

            if (!channel.getLiveConnectedChannel().getValue().getName().equals("USB")) {
                // подсчёт контрольной суммы (если Wi-Fi)
                byte CSUM = 0x1E; // байт с кодом команды, принятый в условии if, также учитывается
                for (int i = 0; i < tempArray.length; i++) CSUM += tempArray[i];
                if (CSUM!=0) {
                    log.add(this, "cmd1 execution: ERROR on CSUM (CSUM!=0): "+
                            Integer.toString(CSUM,16));
                    return false;
                }
            }
            // радиолокационные данные копируются во входной массив
            try {
                ShortBuffer shortBuffer = ByteBuffer.wrap(tempArray)
                        .order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
                for(int tx=0; tx<tx_count; tx++) {
                    shortBuffer.position(shortBuffer.position()+4); // пропуск заголовка
                    shortBuffer.get(dest,tx*frameSizeShorts/tx_count,frameSizeShorts/tx_count);
                }
            }
            catch (Exception e) {
                log.add(this, "CMD1 byte to short conversion error: "+e.toString());
            }
            return true;
        }
        else {
            log.add(this,"cmd1 execution: ERROR on sending command");
            return false;
        }
    }

    /** Cmd7=0x78 – запрос состояния (8-байтовый фрейм).
     * В ответ устройство rdr4_20 передает:
     * – байт Cmd=0x78;
     * – 8-байтовый фрейм состояния, включающий
     * – маркер 0x55,
     * – номер фрейма (циклически повторяющийся 0..255),
     * – байт состояния STS,
     * – код входного напряжения питания (аккумулятора) EP: Up [В] = 6.6*EP/256;
     * – температура устройства [°C] (–128..+127);
     * – количество пропущенных синхроимпульсов СTSA (используется только при внешней синхронизации);
     * – 2-байтовое слово – счетчик ошибок захвата частоты системы ФАП синтезатора;
     * – байт контрольной суммы CSUM (кроме USB).
     * Формат байта STS.
     * STS.1,0=Chc – длина записи данных Step в частотной выборке в зависимости от числа используемых приемных каналов: 0 – одиночные, Step=2*2=4 (Re+Im по 2 байта); 1 – двойные, Step=8; 2 – строенные, Step=12, 3 – счетверенные, Step=16. Здесь возможны только одиночные и сдвоенные записи, т. е. STS.1=0;
     * STS.2 – индикатор пропуска синхроимпульсов: STS.2=1, если СTSA>0;
     * STS.3=0/1 – текущий номер излучателя передатчика;
     * STS.4 – индикатор канала WiFi: при STS.4=1 текущая команда получена по каналу WiFi;
     * STS.5=0 –здесь не используется;
     * STS.6 – индикатор отсутствия захвата ФАП LOCKerr в синтезаторе хотя бы на одной частоте во время последнего зондирования (в норме LOCKerr=0);
     * STS.7 – индикатор зарядки аккумулятора.
     * @return true, если команда успешно отправлена, принят короткий ответ, контрольная сумма совпала
     * и статусные байты успечно считанны. false — в осальных случаях. */
    public boolean sendCommand7() {
        if(sendShortCommand(7,500)) {
            if(!recvShortResponse(7,500)) return false;
            byte[] ans;
            if (channel.getLiveConnectedChannel().getValue().getName().equals("USB"))
                ans = new byte[8];
            else // + байт контрольной суммы (если Wi-Fi)
                ans = new byte[9];
            if (!channel.recv(ans, 500)) return false;

            if (!channel.getLiveConnectedChannel().getValue().getName().equals("USB")) {
                // подсчёт контрольной суммы (если Wi-Fi)
                byte CSUM = 0x78; // байт с кодом команды, принятый в условии if, также учитывается
                for (int i = 0; i < ans.length; i++) CSUM += ans[i];
                if (CSUM!=0) {
                    log.add("RDR", "cmd7 execution: ERROR on CSUM (CSUM!=0)");
                    return false;
                }
            }
            if (!parseStatusBytes(ans)) return false;
            return true;
        }
        log.add("RDR","cmd7 execution: ERROR on sending command");
        return false;
    }

    /** Cmd2=0x2D – запрос состояния.
     * В ответ устройство rdr4_20 передает:
     * – байт Cmd=0x2d;
     * – 16-битовое слово состояния STAT:
     * STAT.0 – индикатор недостаточного напряжения аккумулятора;
     * STAT.1 – режим канала WiFi: 0 – прозрачный (ретрансляция), 1 – командный;
     * STAT.2 – индикатор канала WiFi: 1 означает, что последняя команда UART получена по каналу WiFi (ответ, соответственно, по тому же каналу);
     * STAT.3 – индикатор состояния канала WiFi (1 – включен);
     * STAT.4=1 (постоянное значение) – индикатор диапазона частот LF;
     * STAT.5 – индикатор переполнения кольцевого буфера (возможно только в режиме внешней синхронизации);
     * STAT.6 – индикатор отсутствия захвата частоты синтезатора при последнем свиппировании;
     * STAT.7 – служебный бит, здесь всегда 0;
     * STAT.8 – индикатор подключения WiFi модуля к сети;
     * STAT.9 – индикатор готовности TCP-подключения WiFi к серверу;
     * STAT.10 – индикатор зарядки аккумулятора;
     * STAT.11 – индикатор ошибки АЦП микроконтроллера (при STAT.11=1 данные о напряжении и температуре недостоверны);
     * STAT.15..12 – служебные биты, здесь всегда нули.
     * – байт %Uacc = 0..100 – процент заряда аккумулятора; при работе от внешнего источника питания (в режиме зарядки или при отсутствии батареи) показания немного завышены;
     * – напряжение силового питания V5.6: Upwr [В] = 3.3*2.5*V5.6/256 (1 байт);
     * – входное напряжения питания платы (аккумулятора) EP: Up [В] = 6.6*EP/256 (1 байт, тот же, что в Cmd7);
     * – температура устройства [°C] –128..+127 (1 байт, тот же, что в Cmd7);
     * – байт контрольной суммы CSUM (кроме USB).
     * @return true, если команда успешно отправлена, принят короткий ответ, контрольная сумма совпала
     * и статусные байты успечно считанны. false — в осальных случаях.*/
    public boolean sendCommand2() {
        if(sendShortCommand(2,500)) {
            if(!recvShortResponse(2,500)) return false;
            byte[] ans;
            if (channel.getLiveConnectedChannel().getValue().getName().equals("USB"))
                ans = new byte[6];
            else // + байт контрольной суммы (если Wi-Fi)
                ans = new byte[7];
            if (!channel.recv(ans, 500)) return false;
            if (!channel.getLiveConnectedChannel().getValue().getName().equals("USB")) {
                // подсчёт контрольной суммы (если не Wi-Fi)
                byte CSUM = 0x2D; // байт с кодом команды, принятый в условии if, также учитывается
                for (int i = 0; i < ans.length; i++) CSUM += ans[i];
                if (CSUM!=0) {
                    log.add("RDR", "cmd2 execution: ERROR on CSUM (CSUM!=0)");
                    return false;
                }
            }
            status.setIntStatusValue("STAT",
                    (int) ByteBuffer.wrap(new byte[]{ans[1], ans[0]}).getShort());
            status.setIntStatusValue("Uacc",(int)ans[2]);
            status.setFloatStatusValueByCode("V56",(int)ans[3]+128);
            status.setFloatStatusValueByCode("EP",(int)ans[4]+128);
            status.setIntStatusValue("Tmcu",(int)ans[5]);
            return true;
        }
        log.add("RDR","cmd2 execution: ERROR on sending command");
        return false;
    }

    /** Анализируются байты состояния, после чего информация об устройстве в классе
     * {@link DeviceStatus} обновляется.
     * @param ans весь массив принятых данных. Из него проанализируются только первые 8 байтов
     * @return true, если данные в порядке, false в противном случае
     */
    private boolean parseStatusBytes(byte[] ans) {
        if(ans[0]!=0x55){
            log.add("RDR","cmd1 execution: ERROR on 0x55 label (first byte is "+
                    Integer.toHexString((int)ans[0]).toUpperCase()+")");
            return false;
        }
        status.setIntStatusValue("frN",(int)ans[1]);
        status.setIntStatusValue("STS",(int)ans[2]+128);
        status.setFloatStatusValueByCode("EP",(int)ans[3]+128);
        status.setIntStatusValue("Tmcu",(int)ans[4]);
        status.setIntStatusValue("CTSA",(int)ans[5]);
        status.setIntStatusValue("PLLerr",
                (int) ByteBuffer.wrap(new byte[]{ans[6], ans[7]}).getShort());
        return true;
    }
}
