package org.rdr.radarbox.RDR4_22;

import android.content.Context;

import org.rdr.radarbox.DataChannels.DataChannelUSB;
import org.rdr.radarbox.DataChannels.DataChannelWiFi;
import org.rdr.radarbox.Device.DataChannel;
import org.rdr.radarbox.Device.DeviceCommunication;

public class RDR4_22_Communication extends DeviceCommunication {
    protected DataChannelUSB dataChannelUSB;
    protected DataChannelWiFi dataChannelWiFi;
    public RDR4_22_Communication(Context context, String devicePrefix) {
        super();
        dataChannelUSB =new DataChannelUSB(context);
        channelSet.add(dataChannelUSB); dataChannelUSB.setPriority(1);
        dataChannelUSB.settingsFragment=new RDR4_22_DataChannelUSBFragment();

        dataChannelWiFi =new DataChannelWiFi(context, devicePrefix);
        channelSet.add(dataChannelWiFi); dataChannelWiFi.setPriority(0);

        this.selectChannel(dataChannelUSB.getName());
        setChannelSelectionBasedOnPriority();
    }
}
