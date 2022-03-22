package org.rdr.radarbox.Device;

import org.rdr.radarbox.Logger;
import org.rdr.radarbox.RadarBox;

import java.util.Arrays;

/**
 * Базовый класс протокола обмена с радаром
 * @author Сапронов Данил Игоревич
 * @version 0.1
 */
public class DeviceProtocolRDR {
    protected final DeviceCommunication channel;
    protected final DeviceConfiguration configuration;
    protected final DeviceStatus status;
    protected final Logger log = RadarBox.logger;

    public DeviceProtocolRDR(DeviceCommunication channel, DeviceConfiguration configuration,
                             DeviceStatus status) {
        this.channel = channel;
        this.configuration = configuration;
        this.status = status;
    }

    /** Отправка длинной команды.
     *
     * @param cmd номер команды
     * @param mod модификатор команды
     * @param parameters массив параметров
     * @return true, если отправка каманды прошла успешно
     */
    public boolean sendLongCommand(int cmd, int mod, byte[] parameters, int timeout){
        if(channel==null) {
            log.add("RDR", "ERROR for sendLongCommand(): command ("+cmd+
                    ") DeviceCommunication channel == null");
            return false;
        }
        byte[] data = new byte[parameters.length+3];
        /* байт Cmd: Cmd.7..4=Fun – номер команды, Cmd.3..0=~Cmd.7..4 */
        data[0] = (byte) ((cmd<<4) | (~cmd & 0x0f));
        /* байт CPAR, 4 младших бита которого CPAR.3..0=n=0..15 задают размер блока,
        а 4 старших могут использоваться как модификатор команды MOD */
        data[1] = (byte)((mod << 4) | ((byte)(parameters.length / 2 - 1) & 0x0f));
        /* массив параметров размером N=2*(n+1)=2..32 байт */
        System.arraycopy(parameters,0,data,2,parameters.length);
        if(channel.getLiveConnectedChannel().getValue()==null) {
            log.add("RDR", "ERROR for sendLongCommand() command ("+
                    cmd+") liveConnectedChannel == null");
            return false;
        }
        if(channel.getLiveConnectedChannel().getValue().getName().equals("USB"))
            return channel.send(Arrays.copyOfRange(data,0,data.length-1),timeout);

        /* байт контрольной суммы CSUM, (применяется, когда не USB)
        дополняющий до 0 сумму по модулю 256 всех предыдущих байтов блока, включая Cmd */
        data[parameters.length+2]=0;
        for(int i=0; i<(2+parameters.length); i++)
            data[parameters.length+2]=(byte)(data[parameters.length + 2] + data[i]);
        data[parameters.length+2]= (byte) -data[parameters.length+2];
        return channel.send(data, timeout);
    }

    /** Отправка короткой команды
     *
     * @param cmd номер команды
     * @param timeout таймаут (в мс)
     * @return true, если отправка каманды прошла успешно
     */
    public boolean sendShortCommand(int cmd, int timeout){
        if(channel==null) {
            log.add("RDR", "ERROR for sendShortCommand(): command ("+
                    cmd+") DeviceCommunication channel == null");
            return false;
        }
        /* байт Cmd: Cmd.7..4=Fun – номер команды, Cmd.3..0=~Cmd.7..4 */
        byte[] Cmd= { (byte) ((cmd<<4) | (~cmd & 0x0f)) };
        return channel.send(Cmd,timeout);
    }

    /** Получение короткого ответа в виде повтора кода команды.
     *
     * @param cmd номер команды
     * @param timeout таймаут (в мс)
     * @return true, если полученное сообщение соответствует коду команды
     */
    public boolean recvShortResponse(int cmd, int timeout) {
        if(channel==null) {
            log.add("RDR", "ERROR for recvShortResponse(): command ("+
                    cmd+") DeviceCommunication channel == null");
            return false;
        }
        byte expectedResp = (byte) ((cmd<<4) | (~cmd & 0x0f));
        byte[] recvResp = new byte[1];
        if(channel.recv(recvResp, timeout)) {
            if (recvResp[0]==expectedResp) {
                return true;
            }
            if (recvResp[0]==((byte)(0xf0|(~cmd & 0x0f)))) {
                log.add("RDR", "NACK for command ("+
                        Integer.toHexString(cmd).toUpperCase()+"). But command is ok");
                return false;
            }
            log.add("RDR","recvShortResponse() returns unknown response:("+
                    Logger.toHexString(recvResp)+") instead of ("+
                    Logger.toHexString(new byte[]{expectedResp})+")");
        }
        log.add("RDR", "recvShortResponse() ERROR while recv() for CMD("+cmd+")");
        return false;
    }

    /** Команда Cmd0=0x0F служит для проверки канала.
     * Не выполняет никаких действий, а просто ретранслируется обратно.
     */
    public boolean sendCommand0() {
        if(sendShortCommand(0,500))
            return recvShortResponse(0, 500);
        else {
            log.add("RDR","cmd0 execution: ERROR on sending command");
            return false;
        }
    }
}
