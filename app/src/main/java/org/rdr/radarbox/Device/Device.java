package org.rdr.radarbox.Device;


import android.content.Context;

public abstract class Device {
    private final String devicePrefix;
    public String getDevicePrefix() {return devicePrefix;}

    public DeviceCommunication communication;
    public DeviceConfiguration configuration;
    public DeviceStatus status;
    public DeviceProtocolRDR protocolRDR;

    public Device(Context context, String devicePrefix) {
        this.devicePrefix=devicePrefix;
    }

    /** Подключение к устройству по выбранному каналу связи */
    public boolean Connect() {
        return false;
    }
    /** Отключение от устройства */
    public boolean Disconnect() {
        communication.disconnectAllChannels();
        return true;
    }
    /** Отправить на устройство заданную конфигурацию */
    public synchronized boolean setConfiguration() {
        return false;
    }
    /** Получить статус устройства */
    public synchronized boolean getStatus() {
        return false;
    }
    /** Получить новый кадр данных. В этом месте можно вызывать функцию
     * {@link DeviceRawDataAdapter#reshuffleRawData(short[], DeviceRawDataAdapter.DimensionOrder, int, int, int, boolean)}
     * , чтобы приводить данные к единому виду*/
    public synchronized boolean getNewFrame(short[] dest) {
        return false;
    }
}
