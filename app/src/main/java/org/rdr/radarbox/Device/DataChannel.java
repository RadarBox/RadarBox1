package org.rdr.radarbox.Device;

import java.io.Serializable;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceFragmentCompat;

public abstract class DataChannel implements Serializable, Comparable<DataChannel> {

    private int priority;
    /** <p>Установить приоритет для данного канала.</p>
     * Приоритет канала нужен для того, чтобы устройство автоматически решало, в каком порядке
     * переключаться между каналами, когда возникает ситуация наличия связи сразу по нескольким
     * каналам. Например, устройство в данный момент работает по USB, но параллельно
     * возникает связь по UART. Должно ли устройство переключиться? Если у других каналов в наборе
     * приоритет равен нынешнему, то переключение не производить. Если у нового канала приоритет
     * выше, произвести переключение на него.
     * @param priority приоритет канала (0 - минимальный)
     */
    public void setPriority(int priority) { this.priority = priority; }

    /** <p>Приоритет данного канала относительно других каналов в наборе.</p>
     * Приоритет канала нужен для того, чтобы устройство автоматически решало, в каком порядке
     * переключаться между каналами, когда возникает ситуация наличия связи сразу по нескольким
     * каналам. Например, устройство в данный момент работает по USB, но параллельно
     * возникает связь по UART. Должно ли устройство переключиться? Если у других каналов в наборе
     * приоритет равен нынешнему, то переключение не производить. Если у нового канала приоритет
     * выше, произвести переключение на него.
     * @return приоритет канала (0 - минимальный)
     */
    public int getPriority() { return priority; }

    private final String name;
    public PreferenceFragmentCompat settingsFragment;

    public String getName() {
        return name;
    }

    public DataChannel(String name) {
        this.priority=0;
        this.name=name;
        liveChannelState.setValue(ChannelState.SHUTDOWN);
    }

    public enum ChannelState {
        CONNECTED,
        CONNECTING,
        DISCONNECTED,
        DISCONNECTING,
        SHUTDOWN
    }
    protected final MutableLiveData<ChannelState> liveChannelState = new MutableLiveData<>();
    /** LiveData интерфейс для получения информации о состоянии подключения по текущему каналу */
    public LiveData<ChannelState> getLiveState() {return liveChannelState;}

    /** Попытка подключения по данному каналу связи.
     * @return true, если подключение успешно
     */
    public abstract boolean connect();

    /** Попытка отключения от устройства.
     * @return true, если отключение успешно
     */
    public abstract boolean disconnect();

    /** Попытка передачи данных на устройство по данному каналу связи
     * @param data массив данных для передачи
     * @param timeout - таймаут в мс
     * @return true, если данные успешно переданы
     */
    public boolean send(byte[] data, int timeout) {
        return false;
    }

    /** Попытка приёма данных с устройства по данному каналу связи
     * @param data массив данных, в который будут записаны принятые данные
     * @param timeout - таймаут в мс
     * @return true, если данные успешно приняты
     */
    public boolean recv(byte[] data, int timeout) {
        return false;
    }

    /** Метод, необходимый для сортировки нескольких каналов в наборе TreeSet.
     * Сортировка осуществляется по полю {@link #name}
     * @param o второй объект типа DataChannel для сортировки
     * @return результат сравнения двух имён сравниваемых каналов данных в лексикографическом порядке
     */
    @Override
    public int compareTo(DataChannel o) {
        return this.name.compareTo(o.getName());
    }
}
