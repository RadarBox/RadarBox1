package org.rdr.radarbox.Device;

import android.os.Bundle;
import android.text.InputType;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import org.rdr.radarbox.R;
import org.rdr.radarbox.RadarBox;

import java.util.Iterator;
import java.util.Objects;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

public class DeviceCommunicationFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this.getContext());
        Preference setConfiguration = new Preference(screen.getContext());
        setConfiguration.setTitle(R.string.set_configuration_title);
        setConfiguration.setSummary(R.string.set_configuration_summary);
        setConfiguration.setOnPreferenceClickListener(preference -> {
            Thread thread = new Thread(() -> {
                if(RadarBox.device.setConfiguration()) {
                    requireActivity().runOnUiThread(() -> {
                        Snackbar.make(requireView(), "SET CONFIG OK", Snackbar.LENGTH_SHORT)
                                .show();
                    });
                } else {
                    requireActivity().runOnUiThread(() -> {
                        Snackbar.make(requireView(), "SET CONFIG ERROR", Snackbar.LENGTH_SHORT)
                                .show();
                    });
                }
            });
            thread.start();
            return true;
        });
        Preference getStatus = new Preference(screen.getContext());
        getStatus.setTitle(R.string.get_status_title);
        getStatus.setSummary(R.string.get_status_summary);
        getStatus.setOnPreferenceClickListener(preference -> {
            Thread thread = new Thread(() -> {
                if(RadarBox.device.getStatus()) {
                    requireActivity().runOnUiThread(() -> {
                        Snackbar.make(requireView(), "GET STATUS OK", Snackbar.LENGTH_SHORT)
                                .show();
                    });
                } else {
                    requireActivity().runOnUiThread(() -> {
                        Snackbar.make(requireView(), "GET STATUS ERROR", Snackbar.LENGTH_SHORT)
                                .show();
                    });
                }
            });
            thread.start();
            return true;
        });

        Preference connectedChannelPreference = new Preference(screen.getContext());
        DataChannel connectedChannelOnStart = RadarBox.device.communication.getLiveConnectedChannel().getValue();
        if(connectedChannelOnStart==null) {
            connectedChannelPreference.setTitle(R.string.data_channel_no_connection);
            connectedChannelPreference.setSummary("");
            connectedChannelPreference.setEnabled(false);
            setConfiguration.setEnabled(false);
            getStatus.setEnabled(false);
        }
        else {
            connectedChannelPreference.setTitle(
                    getResources().getString(R.string.data_channel_current_title) + " " +
                            connectedChannelOnStart.getName());
            connectedChannelPreference.setSummary(R.string.data_channel_current_disconnect);
            connectedChannelPreference.setEnabled(true);
            setConfiguration.setEnabled(true);
            getStatus.setEnabled(true);
        }
        RadarBox.device.communication.getLiveConnectedChannel().observe(this,
                connectedChannel->{
            if(connectedChannel==null) {
                connectedChannelPreference.setTitle(R.string.data_channel_no_connection);
                connectedChannelPreference.setSummary("");
                connectedChannelPreference.setEnabled(false);
                setConfiguration.setEnabled(false);
                getStatus.setEnabled(false);
                return;
            }
            connectedChannelPreference.setTitle(
                    getResources().getString(R.string.data_channel_current_title)+" "+
                    connectedChannel.getName());
            connectedChannelPreference.setSummary(R.string.data_channel_current_disconnect);
            connectedChannelPreference.setEnabled(true);
            setConfiguration.setEnabled(true);
            getStatus.setEnabled(true);
        });
        connectedChannelPreference.setOnPreferenceClickListener(preference -> {
            RadarBox.device.communication.disconnectFromWorkingChannel();
            return false;
        });
        screen.addPreference(connectedChannelPreference);
        screen.addPreference(setConfiguration);
        screen.addPreference(getStatus);

        DataChannel selectedChannelOnStart = RadarBox.device.communication.getSelectedChannel();
        ListPreference dataChannelList = new ListPreference(screen.getContext());
        dataChannelList.setKey(RadarBox.device.configuration.getDevicePrefix()+"data_channel");
        dataChannelList.setTitle(R.string.data_channel_list_title);
        dataChannelList.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        String[] names=new String[RadarBox.device.communication.channelSet.size()];
        Iterator<DataChannel>channelIterator=
                RadarBox.device.communication.channelSet.iterator();
        int i=0;
        while(channelIterator.hasNext() && i<names.length) {
            DataChannel channel = channelIterator.next();
            names[i]=channel.getName();
            i++;
        }
        dataChannelList.setEntryValues(names);
        dataChannelList.setEntries(dataChannelList.getEntryValues());
        screen.addPreference(dataChannelList);
        dataChannelList.setValue(selectedChannelOnStart.getName());

        Preference selectedChannelPreferences = new Preference(screen.getContext());
        selectedChannelPreferences.setTitle(R.string.data_channel_selected_settings);
        selectedChannelPreferences.setEnabled(selectedChannelOnStart.settingsFragment != null);
        screen.addPreference(selectedChannelPreferences);
        selectedChannelPreferences.setOnPreferenceClickListener(preference -> {
            Bundle args = new Bundle();
            DataChannel selectedChannel = RadarBox.device.communication.getSelectedChannel();
            getParentFragmentManager().beginTransaction().setReorderingAllowed(true)
                    .replace(R.id.device_navigation_container,
                            selectedChannel.settingsFragment).commit();
            return true;
        });

        Preference selectedChannelButton = new Preference(screen.getContext());
        screen.addPreference(selectedChannelButton);
        selectedChannelButton.setTitle(
                getResources().getString(R.string.data_channel_selected_state)
                        + " " + selectedChannelOnStart.getLiveState().getValue().toString()
        );
        selectedChannelButton.setOnPreferenceClickListener(preference12 ->
                RadarBox.device.communication.connectToSelectedChannel());
        if(!selectedChannelOnStart.getLiveState().getValue()
                .equals(DataChannel.ChannelState.SHUTDOWN)
                && !RadarBox.device.communication.selectedChannel
                .equals(RadarBox.device.communication.workingChannel)) {
            selectedChannelButton.setSummary(R.string.data_channel_selected_connect);
            selectedChannelButton.setEnabled(true);
        }
        else {
            selectedChannelButton.setSummary("");
            selectedChannelButton.setEnabled(false);
        }

        EditTextPreference channelPriority = new EditTextPreference(screen.getContext());
        channelPriority.setTitle(R.string.data_channel_selected_priority_title);
        channelPriority.setKey(RadarBox.device.configuration.getDevicePrefix()+
                selectedChannelOnStart.getName()+"priority");
        channelPriority.setText(Integer.toString(selectedChannelOnStart.getPriority()));
        channelPriority.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
        channelPriority.setOnPreferenceChangeListener((preference, newValue) -> {
            int priority = Integer.parseInt(newValue.toString());
            selectedChannelOnStart.setPriority(priority);
            return true;
        });
        channelPriority.setSummaryProvider((EditTextPreference.SimpleSummaryProvider.getInstance()));

        dataChannelList.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!RadarBox.device.communication.selectChannel(newValue.toString()))
                return false;
            DataChannel selectedChannel = RadarBox.device.communication.getSelectedChannel();
            selectedChannelPreferences.setEnabled(selectedChannel.settingsFragment != null);
            selectedChannel.getLiveState().observe(this, channelState -> {
                selectedChannelButton.setTitle(
                        getResources().getString(R.string.data_channel_selected_state)
                                + " " + channelState.toString()
                );
                if(!channelState.equals(DataChannel.ChannelState.SHUTDOWN) &&
                        !selectedChannel.equals(RadarBox.device.communication.workingChannel)) {
                    selectedChannelButton.setSummary(R.string.data_channel_selected_connect);
                    selectedChannelButton.setEnabled(true);
                }
                else {
                    selectedChannelButton.setSummary("");
                    selectedChannelButton.setEnabled(false);
                }
            });
            channelPriority.setKey(RadarBox.device.configuration.getDevicePrefix()+
                    selectedChannel.getName()+"priority");
            channelPriority.setText(Integer.toString(selectedChannel.getPriority()));
            channelPriority.setOnPreferenceChangeListener((preference2, newValue2) -> {
                int priority = Integer.parseInt(newValue2.toString());
                selectedChannel.setPriority(priority);
                return true;
            });
            return true;
        });

        screen.addPreference(channelPriority);
        setPreferenceScreen(screen);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    }

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        LinearLayoutCompat linearLayout = new LinearLayoutCompat(container.getContext());
//        linearLayout.setLayoutParams(new LinearLayoutCompat.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
//        linearLayout.setOrientation(LinearLayoutCompat.VERTICAL);
//        NestedScrollView scrollView = new NestedScrollView(container.getContext());
//        scrollView.addView(linearLayout);
//
//        return scrollView;
//    }
}
