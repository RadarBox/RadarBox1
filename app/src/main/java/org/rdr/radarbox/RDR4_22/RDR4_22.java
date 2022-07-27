package org.rdr.radarbox.RDR4_22;

import android.content.Context;

import org.rdr.radarbox.Device.Device;
import org.rdr.radarbox.Device.DeviceRawDataAdapter;
import org.rdr.radarbox.Device.DeviceStatus;
import org.rdr.radarbox.Device.RealDeviceConfiguration;
import org.rdr.radarbox.RadarBox;

public class RDR4_22 extends Device {
    int TFcount, RXenabled, TXenabled;
//    public final RDR4_22_ProtocolRDR protocolRDR;
    public RDR4_22(Context context, String devicePrefix) {
        super(context,devicePrefix);
        configuration = new RealDeviceConfiguration(context, devicePrefix);
        TFcount = 1; RXenabled = configuration.getRxN(); TXenabled = configuration.getTxN();
        configuration.getLiveRxEnabled().observeForever(value -> RXenabled=value);
        configuration.getLiveTfCount().observeForever(value -> TFcount=value);
        configuration.getLiveTxEnabled().observeForever(value -> TXenabled =value);
        status = new DeviceStatus(context, devicePrefix);
        communication = new RDR4_22_Communication(context, devicePrefix);
        protocolRDR = new RDR4_22_ProtocolRDR(communication,configuration,status);
    }

    @Override
    public boolean setConfiguration() {
        protocolRDR.sendCommand0();
        return ((RDR4_22_ProtocolRDR)protocolRDR).sendCommand8() &&
                ((RDR4_22_ProtocolRDR)protocolRDR).sendCommand9();
    }

    @Override
    public boolean getStatus() {
        return ((RDR4_22_ProtocolRDR)protocolRDR).sendCommand2() &&
                ((RDR4_22_ProtocolRDR)protocolRDR).sendCommand7();
    }

    @Override
    public boolean getNewFrame(short[] dest) {
        //получаем данные по 1 каналу
        if(!((RDR4_22_ProtocolRDR)protocolRDR).sendCommand1(dest)) return false;
        return DeviceRawDataAdapter.reshuffleRawData(dest,
                configuration.getDimensionOrder(),
                TFcount, RXenabled, TXenabled, configuration.getIsComplex());
    }
}
