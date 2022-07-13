package org.rdr.radarbox;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.MediatorLiveData;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import org.rdr.radarbox.DSP.SettingsDSP;
import org.rdr.radarbox.DataChannels.DataChannelWiFi;
import org.rdr.radarbox.Device.DataChannel;
import org.rdr.radarbox.File.AoRDFolderManager;
import org.rdr.radarbox.File.AoRD_DialogManager;
import org.rdr.radarbox.Plots2D.TimeFreqGraphFragment;

import java.util.Objects;

/** Главная активность приложения для отображения элементов управления и графиков сигналов
 */
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
        createLocationPermissionRequest();
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
        RadarBox.device.communication.getSelectedChannel().getName();
        liveDataMerger.addSource(RadarBox.dataThreadService.getLiveCurrentSource(),value -> {
            if(value.equals(DataThreadService.DataSource.NO_SOURCE))
                liveDataMerger.setValue(0);
            else
                liveDataMerger.setValue(2);
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
                            btnWifi.setImageResource(R.drawable.wifi_animation);
                            ((AnimationDrawable) btnWifi.getDrawable()).start();
                        }
                        else if(wifiState.equals(DataChannel.ChannelState.DISCONNECTED))
                            btnWifi.setImageResource(R.drawable.baseline_network_wifi_bad_24);
                        else if(wifiState.equals(DataChannel.ChannelState.SHUTDOWN))
                            btnWifi.setImageResource(R.drawable.baseline_network_wifi_off_24);
                        else {
                            btnWifi.setImageResource(R.drawable.baseline_network_wifi_full_24);
                            ((DataChannelWiFi)dataChannelWiFi).getLiveWiFiSignalLevel().observe(this,
                            signalLevel -> {
                                switch (signalLevel) {
                                    case 0:
                                        btnWifi.setImageResource(R.drawable.baseline_network_wifi_null_24);
                                        break;
                                    case 1:
                                        btnWifi.setImageResource(R.drawable.baseline_network_wifi_1_bar_24);
                                        break;
                                    case 2:
                                        btnWifi.setImageResource(R.drawable.baseline_network_wifi_2_bar_24);
                                        break;
                                    case 3:
                                        btnWifi.setImageResource(R.drawable.baseline_network_wifi_3_bar_24);
                                        break;
                                    case 4:
                                        btnWifi.setImageResource(R.drawable.baseline_network_wifi_full_24);
                                        break;
                                }
                            });
                        }
                    });
                    // при длинном нажатии происходит отключение от устройства по Wi-Fi
                    btnWifi.setOnLongClickListener(v -> {
                        if(dataChannelWiFi.getLiveState().getValue().equals(DataChannel.ChannelState.CONNECTED) ||
                                dataChannelWiFi.getLiveState().getValue().equals(DataChannel.ChannelState.CONNECTING))
                            return dataChannelWiFi.disconnect();
                        else return false;
                    });
                });
    }

    public void onClickSettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void onClickSettingsDsp(View view){
        Intent intent = new Intent(this, SettingsDSP.class);
        startActivity(intent);
    }

    public void onClickExit(View view) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog
                .Builder(this);
        alertDialogBuilder.setTitle(getString(R.string.str_exit));
        alertDialogBuilder
                .setMessage(getString(R.string.str_exit_msg))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.str_yes),
                        (dialog, which) -> {
                            this.finish();
                            System.exit(0);
                        }).setNegativeButton(getString(R.string.str_no),
                        (dialog,which) -> dialog.dismiss()).create().show();
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
            // Если выбрано "Сохранять файлы" и "Отправлять файлы", то вызвать диалог, отправляющий файл
            // if (RadarBox.fileWriter.isNeedSaveData()) {
            if (AoRDFolderManager.needSaveData) {
                // Сохранение файла
                boolean sendFile = PreferenceManager.getDefaultSharedPreferences(
                        this).getBoolean("need_send", false);
                // RadarBox.fileWriter.endWritingToFile(this, sendFile);
                if (RadarBox.fileWrite == null) {
                    RadarBox.logger.add("ERROR: file to write is null");
                    return;
                }
                AoRD_DialogManager saver = new AoRD_DialogManager(RadarBox.fileWrite);
                saver.createSavingDialog(this, sendFile);
            }
        }
    }

    public void onClickWifi(View view) {
        if (RadarBox.device.communication.channelSet.stream().anyMatch(
                dataChannel -> dataChannel.getName().equals("WiFi"))) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                RadarBox.device.communication.channelSet.stream().filter(
                                dataChannel -> dataChannel.getName().equals("WiFi"))
                        .forEach(dataChannelWiFi -> {
                            dataChannelWiFi.connect();
                        });
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog
                        .Builder(RadarBox.getCurrentActivity());
                alertDialogBuilder.setTitle(getString(R.string.wifi_ask_access_location_title));
                alertDialogBuilder
                        .setMessage(getString(R.string.wifi_no_access_location_message) + "\n" +
                                getString(R.string.wifi_ask_access_location_message))
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.str_yes),
                                (dialog, which) -> {
                                    locationPermissionRequest.launch(new String[]{
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                    });
                                })
                        .setNegativeButton(getString(R.string.str_no),
                                (dialog, which) -> {
                                    RadarBox.device.communication.channelSet.stream().filter(
                                                    dataChannel -> dataChannel.getName().equals("WiFi"))
                                            .forEach(dataChannelWiFi -> {
                                                dataChannelWiFi.connect();
                                            });
                                    dialog.dismiss();
                                });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            } else {
                locationPermissionRequest.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
        }
    }

    private ActivityResultLauncher<String[]> locationPermissionRequest;
    /** Метод создаёт запрос на получение доступа к местоположению.
     * Это необходимо для автоматического подключения к точке доступа (для считывания её MAC адреса)
     * без открытия диалога с прогрессом подключения.
     */
    private void createLocationPermissionRequest() {
        locationPermissionRequest =
                registerForActivityResult(new ActivityResultContracts
                                .RequestMultiplePermissions(), result -> {
                            Boolean fineLocationGranted = result.getOrDefault(
                                    Manifest.permission.ACCESS_FINE_LOCATION, false);
                            Boolean coarseLocationGranted = result.getOrDefault(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,false);
                            if (fineLocationGranted != null && fineLocationGranted) {
                                // Precise location access granted.
                                RadarBox.logger.add("PRECISE LOCATION ACCESS GRANTED");
                                RadarBox.device.communication.channelSet.stream().filter(
                                                dataChannel -> dataChannel.getName().equals("WiFi"))
                                        .forEach(dataChannelWiFi -> {
                                            dataChannelWiFi.connect();
                                        });
                            } else if (coarseLocationGranted != null && coarseLocationGranted) {
                                // Only approximate location access granted.
                                RadarBox.logger.add("ONLY APPROXIMATE LOCATION ACCESS GRANTED");
                            } else {
                                AlertDialog.Builder alertDialogBuilder = new AlertDialog
                                        .Builder(this);
                                alertDialogBuilder.setTitle(getString(R.string.wifi_no_access_location_title));
                                alertDialogBuilder
                                        .setMessage(getString(R.string.wifi_no_access_location_message))
                                        .setCancelable(true)
                                        .setNeutralButton(getString(R.string.str_close),
                                                (dialog, which) -> {
                                                    dialog.dismiss();
                                                    RadarBox.device.communication.channelSet.stream().filter(
                                                                    dataChannel -> dataChannel.getName().equals("WiFi"))
                                                            .forEach(dataChannelWiFi -> {
                                                                dataChannelWiFi.connect();
                                                            });
                                                }).setPositiveButton(getString(R.string.wifi_open_app_settings),
                                                (dialog,which) -> {
                                                    startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                            Uri.fromParts("package", getPackageName(), null)));
                                                    dialog.dismiss();
                                                });
                                AlertDialog alertDialog = alertDialogBuilder.create();
                                alertDialog.show();
                            }
                        }
                );
    }
}