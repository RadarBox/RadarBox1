package org.rdr.radarbox.RDR4_20;

import android.content.Context;

import org.rdr.radarbox.Device.Device;
import org.rdr.radarbox.Device.DeviceStatus;
import org.rdr.radarbox.Device.RealDeviceConfiguration;

public class RDR4_20 extends Device {
    private final RDR4_20_ProtocolRDR protocolRDR;
    public RDR4_20(Context context, String devicePrefix) {
        super(context,devicePrefix);
        configuration = new RealDeviceConfiguration(context, devicePrefix);
        status = new DeviceStatus(context, devicePrefix);
        communication = new RDR4_20_Communication(context, devicePrefix);
        protocolRDR = new RDR4_20_ProtocolRDR(communication,configuration,status);
    }

    @Override
    public boolean Connect() {
        return super.Connect();
    }

    @Override
    public boolean Disconnect() {
        return super.Disconnect();
    }

    @Override
    public boolean setConfiguration() {
        return protocolRDR.sendCommand8() && protocolRDR.sendCommand9();
    }

    @Override
    public boolean getStatus() {
        return protocolRDR.sendCommand2() && protocolRDR.sendCommand7();
    }

    @Override
    public boolean getNewFrame(short[] dest) {
        return protocolRDR.sendCommand1(dest);
    }
}
