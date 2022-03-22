package org.rdr.radarbox.Device;

import android.content.Context;
import android.content.SharedPreferences;

import org.rdr.radarbox.DataChannels.DataChannelWiFi;
import org.rdr.radarbox.RadarBox;

import java.util.Comparator;
import java.util.Objects;
import java.util.TreeSet;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

public abstract class DeviceCommunication {

    protected DataChannel workingChannel, selectedChannel;

    /** Набор коммуникационных каналов, доступный для данного устройства. Создаётся в конструкторе
     * класса при создании класса устройства. */
    public final TreeSet<DataChannel> channelSet;
    /** LiveData интерфейс для генерирования событий при смене источников данных */
    protected final MutableLiveData<DataChannel> liveConnectedChannel = new MutableLiveData<>();
    /** Событие вызывается при смене канала предачи данных
     * @return новый канал передачи данных (USB, Wi-Fi и т.д.) либо null, если нет соединения */
    public LiveData<DataChannel> getLiveConnectedChannel() {return liveConnectedChannel;}

    public DeviceCommunication() {
        channelSet = new TreeSet<DataChannel>();
        workingChannel =null;
    }

    protected void setChannelSettingsFromSharedSettings(Context context, String devicePrefix) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        for (DataChannel channel:channelSet) {
            channel.setPriority(pref.getInt(devicePrefix+
                            channel.getName()+"priority",
                    0));
        }
    }

    /** Этот метод необходимо вызвать в конструкторе, чтобы устройство автоматически решало,
     * в каком порядке переключаться между каналами (когда возникает ситуация наличия связи
     * сразу по нескольким каналам. Например, устройство в данный момент работает по USB,
     * но параллельно возникает физическая связь по UART. Должно ли устройство переключиться?
     * Если у других каналов в наборе приоритет равен нынешнему, то переключение не производить.
     * Если у нового канала приоритет выше, произвести переключение на него.
     */
    protected void setChannelSelectionBasedOnPriority() {
        for (DataChannel channel:channelSet) {
            // для каждого канала устанавливаем слушателя событий его состояния
            channel.getLiveState().observeForever(channelState -> {
                switch (channelState) {
                    //появилось подключение по некоторому каналу
                    case CONNECTED:
                        //текущий рабочий канал не установлен
                        if(workingChannel == null || //ЛИБО приоритет текущего канала ниже нового
                                channel.getPriority() > workingChannel.getPriority())
                            //переключиться на новый канал и сообщить об этом по liveData
                            liveConnectedChannel.setValue(workingChannel = channel);
                        break;
                        //произошло программное отключение по некоторому каналу
                    case DISCONNECTED:
                        //или произошло физическое отключение от некоторого канала
                    case SHUTDOWN:
                        //если рабочий канал не был установлен
                        if(workingChannel==null)
                            break; //значит так надо, ничего не делаем
                        //если рабочий канал - это текущий канал
                        if(workingChannel.equals(channel)) {
                            liveConnectedChannel.setValue(workingChannel=
                                    channelSet.stream() //находим другие каналы в состоянии CONNECTED
                                    .filter(channel2 ->
                                            Objects.equals(channel2.getLiveState().getValue(),
                                                    DataChannel.ChannelState.CONNECTED))
                                    //среди них выбираем канал с максимальным приоритетом
                                    .max(Comparator.comparingInt(DataChannel::getPriority))
                                    //возвращаем его, либо, если таких нет, то возвращаем null
                                    .orElse(null)
                            );


                        }
                        break;
                }
            });
        }
    }

    /** <p>Выбор канала передачи данных.</p>
     * Происходит проверка, существует ли устанавливаемый канал в наборе {@link #channelSet}.
     * Для того, чтобы произошло переключение на выбранный канал данных,
     * необходимо отдельно вызвать метод {@link #connectToSelectedChannel()}
     * @param name имя устанавливаемого коммуникационного канала
     * @return false, если передаётся канал, имя которого отсутствует в списке каналов
     * для данного устройства
     */
    public boolean selectChannel(String name) {
        if(channelSet.stream().noneMatch(dataChannel -> dataChannel.getName().equals(name))) {
            RadarBox.logger.add(this,"No ("+name+") data channel in channel set");
            return false;
        }
        for (DataChannel channel:channelSet)
            if (channel.getName().equals(name)) {
                selectedChannel = channel;
                return true;
            }
        return false;
    }

    public DataChannel getSelectedChannel() {return selectedChannel;}

    /** Попытка подключения к устройству по вырабнному каналу. Если в данный момент существует
     * подключение по другому каналу {@link #workingChannel}!=null, то происходит отключение.
     * После чего производится подключение по выбранному каналу. Если оно успешно, то
     * {@link #workingChannel} присваивается {@link #selectedChannel}
     * @return true, если подключение успешно
     */
    public boolean connectToSelectedChannel(){
        if(selectedChannel ==null) {
            RadarBox.logger.add(this,
                    "connectToSelectedChannel ERROR: selectedChannel == null");
            return false;
        }
        if(!selectedChannel.connect()) {
            RadarBox.logger.add(this,"selectedChannel.connect() returned false");
            return false;
        }
        workingChannel = selectedChannel;
        liveConnectedChannel.setValue(workingChannel);
        return true;
    }

    /** Попытка отключение от устройства по текущему каналу данных. Если в данный момент существует
     * подключение по другому каналу {@link #workingChannel}!=null, то происходит отключение.
     * После чего производится подключение по выбранному каналу. Если оно успешно, то
     * {@link #workingChannel} присваивается {@link #selectedChannel}
     * @return true, если подключение успешно
     */
    public boolean disconnectFromWorkingChannel() {
        if(workingChannel ==null) {
            RadarBox.logger.add(this,"Disconnect ERROR: workingChannel == null");
            return false;
        }
        if(workingChannel.disconnect()) {
            liveConnectedChannel.setValue(workingChannel =null);
            return true;
        }
        RadarBox.logger.add(this,"connectedChannel.disconnect() returned false");
        return false;
    }

    /** Метод, вызывающий {@link DataChannel#disconnect()} для всех каналов в наборе.
     * Нужен перед сменой рабочего устройства.*/
    public void disconnectAllChannels() {
        for (DataChannel channel:channelSet) {
            channel.disconnect();
        }
    }

    /** <p>Передача данных (с таймаутом)</p>
     * Если передача не выполнена за указнный таймаут (в мс), возвращается false.
     *
     * @param data - массив данных для передачи
     * @param timeout - таймаут в мс
     * @return true в случае передачи данных без ошибок за указанное время
     */
    public boolean send(byte[] data, int timeout) {
        if(workingChannel ==null) {
            RadarBox.logger.add(this,"Sending ERROR: connectedChannel == null");
            return false;
        }
        return workingChannel.send(data,timeout);
    }

    /** <p>Приём данных (с таймаутом)</p>
     * Если передача не выполнена за указнный таймаут (в мс), возвращается false.
     *
     * @param data - массив данных для передачи
     * @param timeout - таймаут в мс
     * @return true в случае передачи данных без ошибок за указанное время
     */
    public boolean recv(byte[] data, int timeout) {
        if(workingChannel ==null) {
            RadarBox.logger.add(this,"Receive ERROR: connectedChannel == null");
            return false;
        }
        return workingChannel.recv(data,timeout);
    }
}
