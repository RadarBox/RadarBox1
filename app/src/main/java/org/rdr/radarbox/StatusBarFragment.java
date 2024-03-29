package org.rdr.radarbox;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

/**
 * {@link Fragment} добавляющий статустную строку с отображением актуальной информации о
 * подключении устройства, уровне заряда и некоторой отладочной информации в режиме отладки.
 */
public class StatusBarFragment extends Fragment {

    public StatusBarFragment() {
        // Required empty public constructor
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.status_bar_fragment, container, false);

        // вывод текущего источника данных (файл/устройство) + (префикс устройства)
        setCurrentDataSource(view);

        // вывод количества кадров и периода сбора данных
        setTimingAnimation(view);

        // анимация статуса подключения
        setConnectionChannel(view);

        return view;
    }

    private void setCurrentDataSource(View view) {
        if(RadarBox.dataThreadService.getLiveCurrentSource().getValue()!=null) {
            if(RadarBox.dataThreadService.getLiveCurrentSource().getValue()
                    .equals(DataThreadService.DataSource.DEVICE)) {
                ((TextView)view.findViewById(R.id.status_left_message))
                        .append(" "+RadarBox.device.configuration.getDeviceName());
            }
            else if(RadarBox.dataThreadService.getLiveCurrentSource().getValue()
                    .equals(DataThreadService.DataSource.FILE)) {
                ((TextView)view.findViewById(R.id.status_left_message))
                        .append(" " + RadarBox.fileRead.config.getVirtual().getDeviceName());
                // .append(" "+RadarBox.fileReader.getVirtualDeviceConfiguration().getDeviceName());
            }
        }
        RadarBox.dataThreadService.getLiveCurrentSource().observe(getViewLifecycleOwner(),dataSource -> {
            if(dataSource!=null) {
                ((TextView) view.findViewById(R.id.status_left_message)).setText(dataSource.toString());
                if (dataSource.equals(DataThreadService.DataSource.DEVICE)) {
                    ((TextView) view.findViewById(R.id.status_left_message))
                            .append(" " + RadarBox.device.configuration.getDeviceName());
                    RadarBox.dataThreadService.getLiveStatusCounter().observe(getViewLifecycleOwner(),statusCounter -> {
                        int currentCharge = RadarBox.device.status.getIntStatusValue("Uacc");
                        ((TextView) view.findViewById(R.id.status_left_message))
                                .setText(dataSource + " "
                                        +RadarBox.device.configuration.getDeviceName() + " "
                                        +currentCharge+"%");
                    });
                } else if (dataSource.equals(DataThreadService.DataSource.FILE)) {
                    ((TextView) view.findViewById(R.id.status_left_message))
                            .append(" " + RadarBox.fileRead.config.getVirtual().getDeviceName());
                    // .append(" " + RadarBox.fileReader.getVirtualDeviceConfiguration().getDeviceName());
                }
            }
        });
    }

    private void setTimingAnimation(View view) {
        RadarBox.dataThreadService.getLiveFrameCounter().observe(getViewLifecycleOwner(), frameNumber -> {
            int tFrame = (int) RadarBox.dataThreadService.getLastFrameTimeInterval();
            long divisor = frameNumber; if(frameNumber==0) divisor = 1;
            int tFrameAvg = (int) (RadarBox.dataThreadService.getFullScanningTime() / divisor) + 1;
            String timeString = String.format("%03d %03d %03d", tFrame, tFrameAvg, frameNumber);
            ((TextView) view.findViewById(R.id.status_center_message)).setText(timeString);
        });
    }

    private void setConnectionChannel(View view) {
        if(RadarBox.device!=null) {
            RadarBox.device.communication.getLiveConnectedChannel().observe(getViewLifecycleOwner(),
                    connectedChannel -> {
                        if (connectedChannel == null)
                            ((TextView) view.findViewById(R.id.status_right_message)).setText("");
                        else
                            ((TextView) view.findViewById(R.id.status_right_message)).setText(
                                    connectedChannel.getName());
                    });
            /*
                .getStatusWiFi().observe(getViewLifecycleOwner(),statusWiFi -> {
            ((TextView)view.findViewById(R.id.status_right_message)).setText(statusWiFi.toString());
            if(statusWiFi.equals(DataChannelWiFi.StatusWiFi.CONNECTING))
                view.findViewById(R.id.wifi_connection_animation).setVisibility(View.VISIBLE);
            else
                view.findViewById(R.id.wifi_connection_animation).setVisibility(View.GONE);
            if(statusWiFi.equals(DataChannelWiFi.StatusWiFi.CONNECTED))
                (view.findViewById(R.id.wifi_signal_level)).setVisibility(View.VISIBLE);
            else
                (view.findViewById(R.id.wifi_signal_level)).setVisibility(View.GONE);
        });
        RadarBox.dataIOAdapter.dataChannelWiFi.getWiFiSignalRSSI().observe(getViewLifecycleOwner(),signalRSSI->{
            ((TextView)view.findViewById(R.id.wifi_signal_level)).setText(signalRSSI.toString()+"dB");
        });
        */
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        View view = this.getView();
        // вывод текущего источника данных (файл/устройство) + (префикс устройства)
        setCurrentDataSource(view);

        // вывод количества кадров и периода сбора данных
        setTimingAnimation(view);

        // анимация статуса подключения
        setConnectionChannel(view);
        //this.getView();
    }
}