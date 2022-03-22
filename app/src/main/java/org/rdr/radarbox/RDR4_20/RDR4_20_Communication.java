package org.rdr.radarbox.RDR4_20;

import android.content.Context;

import org.rdr.radarbox.DataChannels.DataChannelUSB;
import org.rdr.radarbox.DataChannels.DataChannelWiFi;
import org.rdr.radarbox.Device.DataChannel;
import org.rdr.radarbox.Device.DeviceCommunication;

public class RDR4_20_Communication extends DeviceCommunication {
    protected DataChannelUSB dataChannelUSB;
    protected DataChannelWiFi dataChannelWiFi;
    public RDR4_20_Communication(Context context, String devicePrefix) {
        super();

        dataChannelUSB =new DataChannelUSB(context);
        channelSet.add(dataChannelUSB);

        dataChannelWiFi =new DataChannelWiFi(context, devicePrefix);
        channelSet.add(dataChannelWiFi);

        setChannelSettingsFromSharedSettings(context,devicePrefix);
        setChannelSelectionBasedOnPriority();
        this.selectChannel(dataChannelWiFi.getName());
        //setConnectedChannelSelector();
    }

    private void setConnectedChannelSelector() {
        dataChannelUSB.getLiveState().observeForever(usbState->{
            //произошло подключение по USB
            if(usbState.equals(DataChannel.ChannelState.CONNECTED)) {
                //если не было подключения ни по одному каналу
                if(workingChannel ==null) {
                    //работаем по USB
                    liveConnectedChannel.setValue(workingChannel = dataChannelUSB);
                    return;
                }
                //если мы и так работаем по USB
                if(workingChannel.equals(dataChannelUSB)) {
                    return; //ничего не делаем
                }
                //если мы были подключены по Wi-Fi
                if (workingChannel.equals(dataChannelWiFi))
                    dataChannelWiFi.closeConnectionTCP(); //только закрываем ТСP сокет
                // переключаемся на работу по USB
                liveConnectedChannel.setValue(workingChannel =dataChannelUSB);
            }
            // произошло программное отключение
            if(usbState.equals(DataChannel.ChannelState.DISCONNECTED)) {
                //рабочий канал не был выбран или мы работали по Wi-Fi
                if (workingChannel ==null || workingChannel.equals(dataChannelWiFi))
                    return; //ничего не делаем

                //если при программном отключении мы работали по USB
                if(workingChannel.equals(dataChannelUSB))
                    //установить отсуствие подключенного канала
                    liveConnectedChannel.setValue(workingChannel =null);
            }
            //произошло физическое отключение (выдирание шнурка) по USB
            if(usbState.equals(DataChannel.ChannelState.SHUTDOWN)) {
                // если при выдирании шнурка мы не работали ни по одному каналу или работали по WiFi
                if(workingChannel ==null || workingChannel.equals(dataChannelWiFi))
                    return; // не производить действий

                //если при отключении мы работали по USB
                if(workingChannel.equals(dataChannelUSB)) {
                    //редкий случай, если идёт работа по USB, но Wi-Fi подключен
                    if (dataChannelWiFi.getLiveState().getValue()
                            .equals(DataChannel.ChannelState.CONNECTED))
                        if(dataChannelWiFi.isNeedAutoconnect()) {
                            liveConnectedChannel.setValue(workingChannel = dataChannelWiFi);
                            return;
                        }

                    // при этом в настройках Wi-Fi выбрано автоподключение
                    if(dataChannelWiFi.isNeedAutoconnect()) {
                        //но, Wi-Fi на устройстве выключен
                        if (dataChannelWiFi.getLiveStatusWiFi().getValue()
                                .equals(DataChannelWiFi.StatusWiFi.OFF)) {
                            //попросить пользователя включить его
                        }
                        else //иначе, если Wi-Fi включен
                            //попытаться подключиться по Wi-Fi
                            dataChannelWiFi.connect();
                    }
                    // в любюом случае установить отсутствие подключенного канала
                    liveConnectedChannel.setValue(workingChannel =null);
                }
            }
        });

        dataChannelWiFi.getLiveState().observeForever(wifiState->{
            if(wifiState.equals(DataChannel.ChannelState.CONNECTED)) {
                // если до этого мы не работали ни по одному каналу
                if(workingChannel ==null) {
                    //работаем по Wi-Fi
                    liveConnectedChannel.setValue(workingChannel =dataChannelWiFi);
                    return;
                }
                //если мы работаем по USB
                if(workingChannel.equals(dataChannelUSB))
                    return; //не производить действий
            }
            if(wifiState.equals(DataChannel.ChannelState.DISCONNECTED) ||
                wifiState.equals(DataChannel.ChannelState.SHUTDOWN)) {
                // если мы не работали ни по одному каналу или работали по USB
                if(workingChannel ==null || workingChannel.equals(dataChannelUSB))
                    return; //ничего не делаем
                // иначе устанавливаем текущий канал в ноль
                liveConnectedChannel.setValue(workingChannel =null);
            }
        });
    }
}
