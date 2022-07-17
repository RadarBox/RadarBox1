package org.rdr.radarbox.RDR4_22;

import android.os.Bundle;

import org.rdr.radarbox.Device.DataChannels.DataChannelUSB;
import org.rdr.radarbox.Device.DataChannel;
import org.rdr.radarbox.Device.DeviceStatus;
import org.rdr.radarbox.RadarBox;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

public class RDR4_22_DataChannelUSBFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // класс канала WiFi передаётся извне
        DataChannelUSB dataChannelUSB=null;
        for (DataChannel channel: RadarBox.device.communication.channelSet) {
            if(channel.getName().equals("USB"))
                dataChannelUSB = (DataChannelUSB) channel;
        }
        if(dataChannelUSB==null)
            return;

        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this.getContext());
        SwitchPreferenceCompat switchWifi = new SwitchPreferenceCompat(screen.getContext());
        switchWifi.setTitle("Turn on Wi-Fi");
        for (DeviceStatus.StatusEntry<?> statusEntry : RadarBox.device.status.getStatusList()) {
            if(statusEntry instanceof DeviceStatus.ComplexStatusEntry) {
                DeviceStatus.ComplexStatusEntry complexStatusEntry =
                        (DeviceStatus.ComplexStatusEntry) statusEntry;
                for(DeviceStatus.ComplexStatusEntry.Bit bit:complexStatusEntry.getBits()) {
                    if(bit.bitID().equals("WIFI3")) {
                        if (bit.getBitVal() != 0)
                            switchWifi.setChecked(true);
                        break;
                    }
                }
            }
        }
        switchWifi.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean previousState = ((SwitchPreferenceCompat)preference).isChecked();
                new Thread(() -> {
                    if(((RDR4_22_ProtocolRDR)(RadarBox.device.protocolRDR))
                            .sendCommand11(!previousState)) {
                        ((SwitchPreferenceCompat)preference).setChecked(!previousState);
                    }
                }).start();
                return false;
            }
        });
        screen.addPreference(switchWifi);
        dataChannelUSB.getLiveState().observe(
                this,
                channelState ->  {
                    switchWifi.setEnabled(channelState
                            .equals(DataChannel.ChannelState.CONNECTED));
                }
        );
        setPreferenceScreen(screen);
    }
}
