package org.rdr.radarbox.Device.DataChannels;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.Toast;

import org.rdr.radarbox.Device.DataChannel;
import org.rdr.radarbox.R;
import org.rdr.radarbox.RadarBox;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

public class DataChannelWiFiSettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.wifi_settings, rootKey);
        // класс канала WiFi передаётся извне

        DataChannelWiFi dataChannelWiFi_temp=null, dataChannelWiFi;
        for (DataChannel channel: RadarBox.device.communication.channelSet) {
            if(channel.getName().equals("WiFi"))
                dataChannelWiFi_temp = (DataChannelWiFi) channel;
        }
        if(dataChannelWiFi_temp==null)
            return;
        else
            dataChannelWiFi = dataChannelWiFi_temp;

        String devicePrefix = RadarBox.device.configuration.getDevicePrefix();
        final SwitchPreferenceCompat wifiAutoconnect = findPreference("wifi_autoconnect");
        assert wifiAutoconnect != null;
        wifiAutoconnect.setChecked(dataChannelWiFi.needAutoconnect);
        wifiAutoconnect.setKey(devicePrefix+wifiAutoconnect.getKey());
        wifiAutoconnect.setOnPreferenceChangeListener(((preference, newValue) -> {
            dataChannelWiFi.needAutoconnect =
                    Boolean.parseBoolean(newValue.toString());
            return true;
        }));

        final SwitchPreferenceCompat wifiIsHidden = findPreference("wifi_is_hidden");
        assert wifiIsHidden != null;
        wifiIsHidden.setChecked(dataChannelWiFi.networkIsHidden);
        wifiIsHidden.setKey(devicePrefix+wifiIsHidden.getKey());
        wifiIsHidden.setOnPreferenceChangeListener(((preference, newValue) -> {
            dataChannelWiFi.networkIsHidden =
                    Boolean.parseBoolean(newValue.toString());
            dataChannelWiFi.setNewNetworkParameters();
            return true;
        }));
        // SSID
        final EditTextPreference wifiSSID = findPreference("wifi_ssid");
        assert wifiSSID != null;
        wifiSSID.setText(dataChannelWiFi.networkSSID);
        wifiSSID.setKey(devicePrefix+wifiSSID.getKey());
        wifiSSID.setOnBindEditTextListener(editText -> {
            editText.setSingleLine(true);
        });
        wifiSSID.setOnPreferenceChangeListener((preference, newValue) -> {
            dataChannelWiFi.networkSSID = newValue.toString();
            dataChannelWiFi.setNewNetworkParameters();
            return true;
        });
        // BSSID
        final EditTextPreference wifiBSSID = findPreference("wifi_bssid");
        assert wifiBSSID != null;
        wifiBSSID.setText(dataChannelWiFi.networkBSSID);
        wifiBSSID.setKey(devicePrefix+wifiBSSID.getKey());
        wifiBSSID.setOnBindEditTextListener(editText -> {
            editText.setSingleLine(true);
        });
        wifiBSSID.setOnPreferenceChangeListener((preference, newValue) -> {
            dataChannelWiFi.networkBSSID = newValue.toString();
            dataChannelWiFi.setNewNetworkParameters();
            return true;
        });
        dataChannelWiFi.getLiveWiFiNetworkBSSID()
                .observe(this, wifiBSSID::setText);
        // PASSWORD
        final EditTextPreference wifiPassword = findPreference("wifi_password");
        assert wifiPassword != null;
        wifiPassword.setText(dataChannelWiFi.networkPass);
        wifiPassword.setKey(devicePrefix+wifiPassword.getKey());
        wifiPassword.setOnBindEditTextListener(editText -> {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(63)});
        });
        wifiPassword.setOnPreferenceChangeListener((preference, newValue) -> {
            dataChannelWiFi.networkPass = newValue.toString();
            dataChannelWiFi.setNewNetworkParameters();
            return true;
        });

        //wifi scan results
        final EditTextPreference wifiScanResult = findPreference("wifi_scan_result");
        assert wifiScanResult != null;
        dataChannelWiFi.getLiveWiFiScanResult()
                .observe(this, wifiScanResult::setText);

        //wifi connect button
        final Preference wifiConnect = findPreference("wifi_connect");
        assert wifiConnect != null;
        wifiConnect.setOnPreferenceClickListener(preference -> {
            if(dataChannelWiFi.connectToAP()) {
                return true;
            }
            Toast.makeText(getContext(), "WIFI CONNECT ERROR", Toast.LENGTH_SHORT).show();
            return false;
        });
        dataChannelWiFi.getLiveStatusWiFi()
                .observe(this, statusWiFi -> wifiConnect.setSummary(statusWiFi.toString()));

        //wifi disconnect button
        final Preference wifiDisconnect = findPreference("wifi_disconnect");
        assert wifiDisconnect != null;
        wifiDisconnect.setOnPreferenceClickListener(preference -> {
            dataChannelWiFi.disconnectFromAP();
            Toast.makeText(getContext(), "WIFI DISCONNECTED", Toast.LENGTH_SHORT).show();
            return true;
        });

        //tcp port
        final EditTextPreference tcpPort = findPreference("tcp_port");
        assert tcpPort != null;
        tcpPort.setKey(devicePrefix+tcpPort.getKey());
        tcpPort.setOnBindEditTextListener(editText -> {
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
        });
        tcpPort.setText(Integer.toString(dataChannelWiFi.tcpServerPort));
        tcpPort.setOnPreferenceChangeListener((preference, newValue) -> {
            dataChannelWiFi.tcpServerPort = Integer.parseInt(newValue.toString());
            return true;
        });

        //tcp address
        final EditTextPreference tcpAddress = findPreference("tcp_address");
        assert tcpAddress != null;
        tcpAddress.setKey(devicePrefix+tcpAddress.getKey());
        tcpAddress.setOnBindEditTextListener(editText -> {
            InputFilter[] filters = new InputFilter[1];
            filters[0] = (source, start, end, dest, dstart, dend) -> {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart)
                            + source.subSequence(start, end)
                            + destTxt.substring(dend);
                    if (!resultingTxt
                            .matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (String split : splits) {
                            if (Integer.parseInt(split) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            };
            editText.setFilters(filters);
        });
        tcpAddress.setText(dataChannelWiFi.tcpServerAddress);
        tcpAddress.setOnPreferenceChangeListener((preference, newValue) -> {
            dataChannelWiFi.tcpServerAddress = newValue.toString();
            return true;
        });

        //tcp start/stop button
        final Preference tcpStartStop = findPreference("tcp_start_stop");
        assert tcpStartStop != null;
        tcpStartStop.setOnPreferenceClickListener(preference -> {
            if(tcpStartStop.getTitle().equals
                    (getResources().getString(R.string.tcp_connection_title_START))) {
                if(dataChannelWiFi.createServerTCP())
                    if(dataChannelWiFi.startAcceptingClientThread())
                        tcpStartStop.setTitle(getResources().getString(R.string.tcp_connection_title_STOP));
            }
            else {
                dataChannelWiFi.closeConnectionTCP();
                tcpStartStop.setTitle(getResources().getString(R.string.tcp_connection_title_START));
            }
            return true;
        });
        dataChannelWiFi.getLiveStatusTCP()
                .observe(this, statusTCP -> {
                    if(statusTCP.equals(DataChannelWiFi.StatusTCP.WAITING) ||
                    statusTCP.equals(DataChannelWiFi.StatusTCP.CONNECTED))
                        tcpStartStop.setTitle(getResources().getString(R.string.tcp_connection_title_STOP));
                    else
                        tcpStartStop.setTitle(getResources().getString(R.string.tcp_connection_title_START));
                    tcpStartStop.setSummary(statusTCP.toString());
                });

        //----------------------------
        final Preference tcpStart = findPreference("tcp_open");
        assert tcpStart != null;
        tcpStart.setOnPreferenceClickListener(preference -> {
            return dataChannelWiFi.createServerTCP();
        });

        final Preference tcpStop = findPreference("tcp_close");
        assert tcpStop != null;
        tcpStop.setOnPreferenceClickListener(preference -> {
            dataChannelWiFi.closeServerTCP();
            return true;
        });

        final Preference tcpStartAccept = findPreference("tcp_start_accept");
        assert tcpStartAccept != null;
        tcpStartAccept.setOnPreferenceClickListener(preference -> {
            return dataChannelWiFi.startAcceptingClientThread();
        });

        final Preference tcpConnectToServer = findPreference("tcp_connect");
        assert tcpConnectToServer != null;
        tcpConnectToServer.setOnPreferenceClickListener(preference -> {
            return dataChannelWiFi.connectToServerTCP();
        });

        final Preference tcpStopConnection = findPreference("tcp_close_connection");
        assert tcpStopConnection != null;
        tcpStopConnection.setOnPreferenceClickListener(preference -> {
            dataChannelWiFi.closeConnectionTCP();
            return true;
        });
    }
}
