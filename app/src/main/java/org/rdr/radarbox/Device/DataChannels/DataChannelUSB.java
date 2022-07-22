package org.rdr.radarbox.Device.DataChannels;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.XmlResourceParser;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import org.rdr.radarbox.Device.DataChannel;
import org.rdr.radarbox.Logger;
import org.rdr.radarbox.R;
import org.rdr.radarbox.RadarBox;
import org.xmlpull.v1.XmlPullParser;

import java.util.HashMap;
import java.util.Iterator;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Класс для приёма и передачи данных по каналу USB
 * @author Сапронов Данил Игоревич
 * @version 1.0
 */
public class DataChannelUSB extends DataChannel {

    private boolean connected;
    final private UsbManager manager;
    private UsbDevice device=null;
    private UsbInterface intf=null;
    private UsbEndpoint endpointIn=null, endpointOut=null;
    private UsbDeviceConnection connection=null;
    private Logger log = RadarBox.logger;
    private PendingIntent permissionIntent;

    private Context context;

    private String ACTION_USB_PERMISSION = "USB_PERMISSION";

    /** LiveData интерфейс для получения информации о состоянии подключения по USB */
    private final MutableLiveData<Boolean> liveDataStatusUSB = new MutableLiveData<>();
    public LiveData<Boolean> getStatusUSB() {return liveDataStatusUSB;}

    public DataChannelUSB(Context context) {
        super("USB");
        this.context = context;
        ACTION_USB_PERMISSION = context.getPackageName()+'.'+ACTION_USB_PERMISSION;
        permissionIntent = PendingIntent.getBroadcast(context,0,
                new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();

        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_USB_PERMISSION);

        manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        BroadcastReceiver usbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                log.add("USB","action: " + action);
                // при подключении устройства
                if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                    //if(setDevice())
                    device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    connect();
                    return;
                }
                // при отключении устройства
                if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    disconnect();
                    liveChannelState.postValue(ChannelState.SHUTDOWN);
                    liveDataStatusUSB.postValue(false);
                    return;
                }
                // при предоставлении разрешений
                if(ACTION_USB_PERMISSION.equals(action)) {
                    synchronized (this) {
                        device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if(intent.getBooleanExtra(
                                UsbManager.EXTRA_PERMISSION_GRANTED,false)) {
                            log.add("USB","Permission granted");
                            if(openUSBDevice()) {
                                liveChannelState.setValue(ChannelState.CONNECTED);
                                liveDataStatusUSB.postValue(true);
                            }
                            else
                                log.add("USB","USB device not connected");
                        }
                        else {
                            log.add("USB","Permission denied for device (" +
                                    device.getProductName()+")");
                        }
                    }
                    return;
                }
            }
        };
        context.registerReceiver(usbReceiver,filter);
        checkConnectedDevicesOnStart();
        settingsFragment = null;
    }

    private void checkConnectedDevicesOnStart() {
        if(setDevice())
            connect();
    }

    public boolean setDevice() {
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        if(deviceList.isEmpty()) {
            log.add("USB", "setDevice(): deviceList is empty");
            return false;
        }

        Iterator<UsbDevice> deviceIterator  = deviceList.values().iterator();
        if(deviceList.size()==1) {
            device = deviceIterator.next();
            log.add("USB","setDevice(): SINGLE USB DEVICE\t"+device.getProductName()+
                    " VendorID: "+device.getVendorId()+" ProductID: "+device.getProductId());
        }
        else {
            log.add("USB","setDevice(): SEVERAL USB DEVICES");
            while (deviceIterator.hasNext()) {
                device = deviceIterator.next();
            }
        }
        return true;
    }

    @Override
    public boolean connect() {
        if (device==null) {
            log.add("USB","connect(): UsbDevice==null");
            liveChannelState.setValue(ChannelState.DISCONNECTED);
            liveDataStatusUSB.postValue(false);
            return false;
        }
        if (connected) {
            liveChannelState.setValue(ChannelState.CONNECTED);
            liveDataStatusUSB.postValue(true);
            return true;
        }
        // Проверить, удовлетворяет ли подключённое устройство фильтру USB устройств в .xml файле
        boolean isUSBDeviceInFilterList = false;
        try {
            XmlResourceParser parser = context.getResources().getXml(R.xml.usb_device_filter);
            while(parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() == XmlPullParser.START_TAG) {
                    if(parser.getName().equals("usb-device")) {
                        int VID = parser.getAttributeIntValue(null,"vendor-id",-1);
                        int PID = parser.getAttributeIntValue(null,"product-id",-1);
                        if (device.getVendorId()==VID && device.getProductId()==PID)
                            isUSBDeviceInFilterList = true;
                    }
                }
                parser.next();
            }
        } catch (Exception e) {
            log.add("USB XML PARSER", "Exception: "+e.getLocalizedMessage());
        }
        if(!isUSBDeviceInFilterList) {
            liveChannelState.setValue(ChannelState.DISCONNECTED);
            liveDataStatusUSB.postValue(false);
            return false;
        }

        // Запросить разрешения на подключение перед общением, иначе не работает openDevice()
        if(!manager.hasPermission(device)) {
            log.add("USB","Application hasn't got permissions. Trying to request them...");
            manager.requestPermission(device, permissionIntent);
            return false;
        }
        if(openUSBDevice()) {
            liveChannelState.setValue(ChannelState.CONNECTED);
            liveDataStatusUSB.postValue(true);
            return true;
        }
        liveChannelState.setValue(ChannelState.DISCONNECTED);
        liveDataStatusUSB.postValue(false);
        return false;
    }

    private boolean openUSBDevice() {
        intf = device.getInterface(0);
        // назначение выходных и входных endpoint
        for(int i=0; i<intf.getEndpointCount(); i++) {
            if(intf.getEndpoint(i).getType()!= UsbConstants.USB_ENDPOINT_XFER_BULK)
                continue;
            if((intf.getEndpoint(i).getDirection()==UsbConstants.USB_DIR_OUT)&&
                    (endpointOut==null))
                endpointOut=intf.getEndpoint(i);
            if((intf.getEndpoint(i).getDirection()==UsbConstants.USB_DIR_IN)&&
                    (endpointIn==null))
                endpointIn=intf.getEndpoint(i);
        }
        // если по заданному интерфейсу не найдены входные или выходные endpoint, вернуть false
        if((endpointIn==null)||(endpointOut==null)) {
            log.add("USB","Endpoint in OR out == null");
            return false;
        }
        // попытка получить доступ к выбранному интерфейсу
        connection = manager.openDevice(device);
        if(connection!=null) {
            connected = connection.claimInterface(intf,true);
            if(connected) {
                log.add("USB", "USB device connected");
            }
            else
                log.add("USB","claimInterface()==false");
            return connected;
        }
        else {
            log.add("USB","openDevice()==null");
            return false;
        }
    }

    public boolean isConnected() {
        return connected;
    }

    @Override
    public boolean disconnect() {
        if(device==null) {
            log.add("USB","Device has already been disconnected (device==null)");
            connected = false;
            return true;
        }
        if(connection==null) {
            log.add("USB","Device has already been disconnected (connection==null)");
            connected = false;
            return true;
        }
        connection.releaseInterface(intf);
        connection.close();
        log.add("USB","Device was disconnected");
        connected = false;
        context.getSharedPreferences("data_channel",Context.MODE_PRIVATE).edit().apply();
        return true;
    }

    /** <p>Передача данных по USB с заданным таймаутом (в мс).</p>
     * Проверяется, соответствует ли количетво байт, возвращаемое функцией bulkTransfer()
     * количеству данных, переданных в функцию. Если нет, то возвращается false и записывается
     * сообщение об ошибке в log.txt
     * @param data массив данных
     * @param timeout таймаут (в мс)
     * @return false, если ошибка передачи данных или таймаут
     */
    @Override
    public boolean send(byte[] data, int timeout) {
        if(connected) {
            /* //Проверка отправляемых даных
            log.add("USB","sending data length:("+data.length+
                    ")\n\t\tdata:\t"+Logger.toHexString(data)); */
            int iByteCount = connection.bulkTransfer(endpointOut,data,data.length,timeout);
            if(iByteCount!=data.length) {
                log.add("USB","Send ERROR: bulkTransfer() return ("+iByteCount+
                        ") bytes. Expected ("+data.length+") bytes.");
                return false;
            }
            return true;
        }
        return false;
    }

    /** <p>Приём данных по USB с заданным таймаутом (в мс).</p>
     * Проверяется, соответствует ли количетво байт, возвращаемое функцией bulkTransfer(),
     * количеству данных, переданных в функцию. Если нет, то возвращается false и записывается
     * сообщение об ошибке в log.txt
     * @param data массив данных
     * @param timeout таймаут (в мс)
     * @return false, если ошибка передачи данных или таймаут
     */
    @Override
    public boolean recv(byte[] data, int timeout) {
        if(connected) {
            int iByteCount = connection.bulkTransfer(endpointIn, data, data.length, timeout);
            if (iByteCount != data.length) {
                log.add("USB","Recv ERROR: bulkTransfer() return ("+iByteCount+
                        ") bytes. Expected ("+data.length+") bytes.");
                return false;
            }
            /* //Проверка принимаемых даных
            log.add("USB","receiving data length:("+data.length+
                    ")\n\t\tdata:\t"+Logger.toHexString(data)); */
            return true;
        }
        log.add("USB","Recv ERROR: Try to recv data, but device is not connected");
        return false;
    }
}
