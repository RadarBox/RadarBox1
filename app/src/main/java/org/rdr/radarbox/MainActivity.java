package org.rdr.radarbox;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.lifecycle.MediatorLiveData;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import org.rdr.radarbox.DataChannels.DataChannelWiFi;
import org.rdr.radarbox.Device.DataChannel;
import org.rdr.radarbox.File.Sender;
import org.rdr.radarbox.Plots2D.TimeFreqGraphFragment;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.main_data_view_container, TimeFreqGraphFragment.class,null)
                    .commit();
        }
        //ImageButton btnWifi = findViewById(R.id.btn_wifi);
        /*
        Observer<DataThreadService.DataThreadState> dataThreadObserver =
                new Observer<DataThreadService.DataThreadState>() {
                    @Override
                    public void onChanged(DataThreadService.DataThreadState dataThreadState) {
                        if(dataThreadState.equals(DataThreadService.DataThreadState.STOPPED)) {
                            btnStartStop.setText(getString(R.string.str_start));
                            RadarBox.dataThreadService.getLiveDataThreadState().removeObserver(this);
                            if(RadarBox.fileWriter.isNeedSaveData())
                                Sender.createDialogToSendFile(getParent(),
                                        RadarBox.fileWriter.getFileWrite());
                        }
                    }
                };
        RadarBox.dataThreadService.getLiveDataThreadState().observe(this, dataThreadObserver);
        */
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        ImageButton btnStartStop = findViewById(R.id.btn_start);
        MediatorLiveData<Integer> liveDataMerger = new MediatorLiveData<>();
        liveDataMerger.addSource(RadarBox.dataThreadService.getLiveCurrentSource(),value -> {
            if(value.equals(DataThreadService.DataSource.NO_SOURCE))
                liveDataMerger.setValue(0);
                });
        liveDataMerger.addSource(RadarBox.dataThreadService.getLiveDataThreadState(),value ->{
            if(value.equals(DataThreadService.DataThreadState.STARTED))
                liveDataMerger.setValue(1);
            else
                liveDataMerger.setValue(2);
        });
        liveDataMerger.observe(this,value -> {
            if(value.equals(0))
                btnStartStop.setImageDrawable(AppCompatResources
                        .getDrawable(this,R.drawable.baseline_play_disabled_24));
            else if(value.equals(1))
                btnStartStop.setImageDrawable(AppCompatResources
                        .getDrawable(this,R.drawable.baseline_pause_24));
            else btnStartStop.setImageDrawable(AppCompatResources
                        .getDrawable(this,R.drawable.baseline_play_arrow_24));
        });

        ImageButton btnWifi = findViewById(R.id.btn_wifi);
        RadarBox.device.communication.channelSet.stream().filter(
                dataChannel -> dataChannel.getName().equals("WiFi"))
                .forEach(dataChannelWiFi -> {
                    dataChannelWiFi.getLiveState().observe(this,wifiState -> {
                        if(wifiState.equals(DataChannel.ChannelState.CONNECTING)) {
                            btnWifi.setBackgroundResource(R.drawable.wifi_animation);
                            ((AnimationDrawable) btnWifi.getBackground()).start();
                        }
                        else if(wifiState.equals(DataChannel.ChannelState.DISCONNECTED))
                            btnWifi.setBackground(AppCompatResources.getDrawable(this,
                                    R.drawable.baseline_network_wifi_bad_24));
                        else if(wifiState.equals(DataChannel.ChannelState.SHUTDOWN))
                            btnWifi.setBackground(AppCompatResources.getDrawable(this,
                                    R.drawable.baseline_network_wifi_off_24));
                        else {
                            btnWifi.setBackground(AppCompatResources.getDrawable(this,
                                    R.drawable.baseline_network_wifi_4_bar_24));
                            ((DataChannelWiFi)dataChannelWiFi).getLiveWiFiSignalLevel().observe(this,
                            signalLevel -> {
                                switch (signalLevel) {
                                    case 0: btnWifi.setBackground(AppCompatResources.getDrawable(this,
                                            R.drawable.baseline_network_wifi_null_24));
                                    case 1: btnWifi.setBackground(AppCompatResources.getDrawable(this,
                                            R.drawable.baseline_network_wifi_1_bar_24));
                                        break;
                                    case 2: btnWifi.setBackground(AppCompatResources.getDrawable(this,
                                            R.drawable.baseline_network_wifi_2_bar_24));
                                        break;
                                    case 3: btnWifi.setBackground(AppCompatResources.getDrawable(this,
                                            R.drawable.baseline_network_wifi_3_bar_24));
                                        break;
                                    case 4: btnWifi.setBackground(AppCompatResources.getDrawable(this,
                                            R.drawable.baseline_network_wifi_4_bar_24));
                                        break;
                                }
                            });
                        }
                    });
                });
    }

    public void onClickSettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void onClickExit(View view) {
        this.finish();
        System.exit(0);
    }

    public void onClickStartStop(View view) {
        if(Objects.equals(RadarBox.dataThreadService.getLiveCurrentSource().getValue(),
                DataThreadService.DataSource.NO_SOURCE))
            return;

        if (Objects.equals(RadarBox.dataThreadService.getLiveDataThreadState().getValue(),
                DataThreadService.DataThreadState.STOPPED)) {
            RadarBox.dataThreadService.start();
        }
        else {
            RadarBox.dataThreadService.stop();
            if(RadarBox.fileWriter.isNeedSaveData())
                Sender.createDialogToSendFile(this,
                    RadarBox.fileWriter.getFileWrite());
        }
    }

    public void onClickWifi(View view) {
        if(RadarBox.device.communication.channelSet.stream().anyMatch(
                dataChannel -> dataChannel.getName().equals("WiFi"))) {
            RadarBox.device.communication.channelSet.stream().filter(
                    dataChannel -> dataChannel.getName().equals("WiFi"))
                    .forEach(dataChannelWiFi -> {
                        dataChannelWiFi.connect();
                    });
        }
    }
}