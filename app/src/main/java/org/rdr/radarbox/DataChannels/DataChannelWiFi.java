package org.rdr.radarbox.DataChannels;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WifiNetworkSuggestion;
import android.util.Log;
import android.widget.Toast;

import org.rdr.radarbox.Device.DataChannel;
import org.rdr.radarbox.R;
import org.rdr.radarbox.RadarBox;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.IllegalBlockingModeException;
import java.util.List;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;


/**
 * Класс для приёма и передачи данных по каналу Wi-Fi
 * @author Сапронов Данил Игоревич
 * @version 1.0
 */
public class DataChannelWiFi extends DataChannel {

    WifiManager mWifiManager;
    Context context;
    IntentFilter intentFilter;
    WifiNetworkSuggestion mWifiNetworkSuggestion;
    WifiNetworkSpecifier mWifiNetworkSpecifier;
    NetworkRequest mWifiNetworkRequest;
    final ConnectivityManager mConnectivityManager;

    // TCP
    int tcpServerPort = 0;
    String tcpServerAddress = null;
    ServerSocket serverSocket = null;
    Socket communicationSocket = null;

    DataInputStream dataInputStream;
    DataOutputStream dataOutputStream;

    int currentWifiState=-1;
    boolean needAutoconnect;
    public boolean isNeedAutoconnect() { return needAutoconnect; }

    Network networkWiFi;
    String networkSSID;
    String networkPass;
    String networkBSSID;
    boolean networkIsHidden;

    public enum StatusTCP {
        OFF,
        WAITING,
        CONNECTED
    }

    public enum StatusWiFi {
        OFF,
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    /** LiveData интерфейс для получения информации о состоянии TCP */
    private final MutableLiveData<StatusTCP> liveStatusTCP = new MutableLiveData<>();
    public LiveData<StatusTCP> getLiveStatusTCP() {return liveStatusTCP;}
    /** LiveData интерфейс для получения информации о состоянии WiFi */
    private final MutableLiveData<StatusWiFi> liveStatusWiFi = new MutableLiveData<>();
    public LiveData<StatusWiFi> getLiveStatusWiFi() {return liveStatusWiFi;}
    /** LiveData интерфейс для получения информации о результатах сканирования точек доступа WiFi */
    private final MutableLiveData<String> liveWiFiScanResult = new MutableLiveData<>();
    public LiveData<String> getLiveWiFiScanResult() {return liveWiFiScanResult;}
    /** LiveData интерфейс для получения информации о MAC-адресе устройства для дальнешего
     * автоматического подключения к точке доступа */
    private final MutableLiveData<String> liveWiFiNetworkBSSID = new MutableLiveData<>();
    public LiveData<String> getLiveWiFiNetworkBSSID() {return liveWiFiNetworkBSSID;}
    /** LiveData интерфейс для получения информации об уровне сигнала Wi-Fi */
    private final MutableLiveData<Integer> liveWiFiSignalLevel = new MutableLiveData<>();
    public LiveData<Integer> getLiveWiFiSignalLevel() {return liveWiFiSignalLevel;}

    /** Обработчик событий при подключении к сети радара */
    ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            RadarBox.logger.add("WIFI", "onAvailable() " + network.toString()+"\n"
                    +mConnectivityManager.getLinkProperties(network).toString());
            if(networkBSSID.isEmpty())
                getBSSIDofCurrentAP();

            RadarBox.logger.add("WIFI", "bindProcessToNetwork(" + network.toString() + "): "
                    + mConnectivityManager.bindProcessToNetwork(network));

            liveStatusWiFi.postValue(StatusWiFi.CONNECTED);
            connectToServerTCP();
        }

        @Override
        public void onLosing(@NonNull Network network, int maxMsToLive) {
            super.onLosing(network, maxMsToLive);
            RadarBox.logger.add("WIFI", "onLosing()"+network);
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            RadarBox.logger.add("WIFI", "onLost() "+network);

//            closeServerTCP();
//            mConnectivityManager.bindProcessToNetwork(null);
            if(currentWifiState==WifiManager.WIFI_STATE_DISABLED) {
                liveStatusWiFi.postValue(StatusWiFi.OFF);
                liveChannelState.postValue(ChannelState.SHUTDOWN);
            }
            else {
                liveStatusWiFi.postValue(StatusWiFi.DISCONNECTED);
                liveChannelState.postValue(ChannelState.DISCONNECTED);
            }
            RadarBox.logger.add("WiFi","LiveChannelState: "+liveChannelState.getValue().toString());
            closeConnectionTCP();
            if(needAutoconnect) connectToAP();
        }

        @Override
        public void onUnavailable() {
            super.onUnavailable();
            RadarBox.logger.add("WIFI", "onUnavailable()");
            if(serverSocket!=null)
                if(!serverSocket.isClosed())
                    closeServerTCP();

            liveStatusWiFi.postValue(StatusWiFi.DISCONNECTED);
            liveChannelState.postValue(ChannelState.DISCONNECTED);
            if(needAutoconnect) connectToAP();
        }

//        @Override
//        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
//            super.onCapabilitiesChanged(network, networkCapabilities);
//            Log.i("WIFI", "onCapabilitiesChanged() "+network+networkCapabilities.toString());
//        }
//
//        @Override
//        public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
//            super.onLinkPropertiesChanged(network, linkProperties);
//            Log.i("WIFI", "onLinkPropertiesChanged() "+network+linkProperties.toString());
//        }
//
//        @Override
//        public void onBlockedStatusChanged(@NonNull Network network, boolean blocked) {
//            super.onBlockedStatusChanged(network, blocked);
//            Log.i("WIFI", "onBlockedStatusChanged() "+network+blocked);
//        }
    };

    BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                currentWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
                if(currentWifiState==WifiManager.WIFI_STATE_DISABLED) {
                    liveStatusWiFi.postValue(StatusWiFi.OFF);
                    liveChannelState.postValue(ChannelState.SHUTDOWN);
                }
                else if(currentWifiState==WifiManager.WIFI_STATE_ENABLED) {
                    liveStatusWiFi.postValue(StatusWiFi.DISCONNECTED);
                    liveChannelState.postValue(ChannelState.DISCONNECTED);
                }
            }
            else if(action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
                parseScanResults();
            else if(action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
                liveWiFiSignalLevel.postValue(mWifiManager
                        .calculateSignalLevel(intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -1)));
            }
        }
    };

    public DataChannelWiFi(Context context, String devicePrefix) {
        super("WiFi");
        this.context = context;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        currentWifiState = mWifiManager.getWifiState();

        mConnectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        setInitialParameters(devicePrefix);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        context.registerReceiver(wifiStateReceiver,intentFilter);
        parseScanResults();
        settingsFragment = new DataChannelWiFiSettingsFragment();
    }

    private void setInitialParameters(String devicePrefix) {
        liveStatusTCP.postValue(StatusTCP.OFF);
        liveStatusWiFi.postValue(StatusWiFi.OFF);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean needAutoconnectDef = false, networkIsHiddenDef = false;
        String networkSSIDDef = "no_default_ssid", networkBSSIDDef="no_default_bssid";
        String networkPassDef = "no_default_password";
        String tcpServerAddressDef = "0.0.0.0", tcpServerPortDef="0";
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(context.getAssets().open(devicePrefix+"/channel_config.xml"),null);
            while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() == XmlPullParser.START_TAG
                        && parser.getName().equals("wifi")) {
                    String attrValue = parser.getAttributeValue(null, "autoconnect_default");
                    if(attrValue!=null) needAutoconnectDef=Boolean.parseBoolean(attrValue);

                    attrValue = parser.getAttributeValue(null, "is_hidden_default");
                    if(attrValue!=null) networkIsHiddenDef=Boolean.parseBoolean(attrValue);

                    attrValue = parser.getAttributeValue(null, "ssid_default");
                    if(attrValue!=null) networkSSIDDef=attrValue;

                    attrValue = parser.getAttributeValue(null, "bssid_default");
                    if(attrValue!=null) networkBSSIDDef=attrValue;

                    attrValue = parser.getAttributeValue(null, "password_default");
                    if(attrValue!=null) networkPassDef=attrValue;
                }
                if (parser.getEventType() == XmlPullParser.START_TAG
                        && parser.getName().equals("tcp"))  {
                    String attrValue = parser.getAttributeValue(null, "address_default");
                    if(attrValue!=null) tcpServerAddressDef=attrValue;

                    attrValue = parser.getAttributeValue(null, "port_default");
                    if(attrValue!=null) tcpServerPortDef=attrValue;
                }
                parser.next();
            }
        } catch (IOException ioException) {
            RadarBox.logger.add("WIFI","Open Resources exception: "+ioException.getLocalizedMessage());
        } catch (XmlPullParserException parserException) {
            RadarBox.logger.add("WIFI","Parse Resources exception: "+parserException.getLocalizedMessage());
        }
        needAutoconnect = pref.getBoolean(devicePrefix+"wifi_autoconnect",needAutoconnectDef);
        networkIsHidden = pref.getBoolean(devicePrefix+"wifi_is_hidden",networkIsHiddenDef);
        networkSSID = pref.getString(devicePrefix+"wifi_ssid",networkSSIDDef);
        networkBSSID = pref.getString(devicePrefix+"wifi_bssid",networkBSSIDDef);
        networkPass = pref.getString(devicePrefix+"wifi_password",networkPassDef);
        tcpServerAddress = pref.getString(devicePrefix+"tcp_address",tcpServerAddressDef);
        tcpServerPort = Integer.parseInt(pref.getString(devicePrefix+"tcp_port",tcpServerPortDef));
        setNewNetworkParameters();
    }

    private void getBSSIDofCurrentAP() {
        String curBSSID = mWifiManager.getConnectionInfo().getBSSID();
        if(curBSSID.equals(null)) {
            Log.i("WIFI","BSSID is null");
            return;
        }
        if(curBSSID.equals("02:00:00:00:00:00")) {
            Log.i("WIFI","Insufficient permissions to access the BSSID");
            Toast.makeText(context,
                    "Insufficient permissions to access the BSSID", Toast.LENGTH_SHORT).show();
            return;
        }
        networkBSSID = curBSSID;
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putString("wifi_bssid",networkBSSID).apply();
        liveWiFiNetworkBSSID.postValue(networkBSSID);
        setNewNetworkParameters();
    }

    private void parseScanResults() {
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            liveWiFiScanResult.setValue("NO \"ACCESS_FINE_LOCATION\" PERMISSIONS");
            return;
        }
        List<ScanResult> results = null;
        try{
            String res = "NO SCAN RESULTS";
            results = mWifiManager.getScanResults();
            boolean isAPfound = false;
            int len = results.size();
            if(len>0)
                res = "SCAN RESULTS:\n";
            for (int i = 0; i < len; i++) {
                res+=results.get(i).SSID+"\t"+results.get(i).BSSID+"\t"+results.get(i).level+"dBm";
                if(results.get(i).BSSID.equals(networkBSSID)) {
                    res += "\t<-\tV";
                    res = res.replace("SCAN RESULTS:","AP FOUND");
                    isAPfound = true;
                }
                res += "\n";
            }
            liveWiFiScanResult.postValue(res);
            if(isAPfound && needAutoconnect && liveStatusWiFi.getValue()!= StatusWiFi.CONNECTED)
                connectToAP();
        }
        catch(Exception e)
        {
            Log.e("WIFI",e.getMessage());
        }
    }

    public void setNewNetworkParameters() {
        WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder();
        if(!networkSSID.isEmpty()) {
            builder.setSsid(networkSSID);
            builder.setIsHiddenSsid(networkIsHidden);
        }
        else if(networkIsHidden)
            Toast.makeText(context,
                    "Hidden AP should have not empty SSID", Toast.LENGTH_SHORT).show();

        if(!networkBSSID.isEmpty()) {
            try { builder.setBssid(MacAddress.fromString(networkBSSID)); }
            catch (IllegalArgumentException e) {
                Toast.makeText(context,
                        "BSSID has wrong format", Toast.LENGTH_SHORT).show();
            }
        }
        if(!networkPass.isEmpty())
            builder.setWpa2Passphrase(networkPass);

        mWifiNetworkSpecifier = builder.build();
        mWifiNetworkRequest =
                new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .setNetworkSpecifier(mWifiNetworkSpecifier)
                        .build();
    }

    /** Подключение к точке доступа Wi-Fi.
     * @return false, если не включен Wi-Fi, true, если уже есть подключение Wi-Fi с устройством
     * или если процесс подключения успешно запущен
     */
    public boolean connectToAP() {
        Log.i("WIFI", "connectToAP()");
        if(currentWifiState!=WifiManager.WIFI_STATE_ENABLED) {
            RadarBox.logger.add(this, "WIFI_STATE_DISABLED");
            // открыть диалог, для включения Wi-Fi
            displayWiFiDialog();
            return false;
        }
        if(liveStatusWiFi.getValue().equals(StatusWiFi.CONNECTED)) {
            RadarBox.logger.add(this, "WiFi is already connected");
            return true;
        }
//        if(serverSocket!=null)
//            if(!serverSocket.isClosed())
//                closeServerTCP();
        try {
            if(mNetworkCallback!=null)
                mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
        } catch (IllegalArgumentException e) {
            Log.i("WIFI", e.toString());
        }
        //Таймаут к попытке подключения 6 секунд
        mConnectivityManager.requestNetwork(mWifiNetworkRequest, mNetworkCallback,6000);
        liveStatusWiFi.postValue(StatusWiFi.CONNECTING);
        liveChannelState.postValue(ChannelState.CONNECTING);
        return true;
    }

    /** Метод открывает диалог, отправляющий пользователя в настройки Wi-Fi
     * для включения его в системе */
    private void displayWiFiDialog() {
//        context.startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK)
//                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
//
        AlertDialog.Builder alertDialogBuilder = new AlertDialog
                .Builder(RadarBox.getCurrentActivity());
        alertDialogBuilder.setTitle(context.getString(R.string.wifi_dialog_title));
        alertDialogBuilder
                .setMessage(context.getString(R.string.wifi_dialog_message))
                .setCancelable(false)
                .setPositiveButton(context.getString(R.string.str_yes),
                        (dialog, which) -> RadarBox.getCurrentActivity()
                        .startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK)))
                .setNegativeButton(context.getString(R.string.str_no),
                        (dialog, which) -> dialog.dismiss());
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    /** Создать TCP сервер.
     * @return true, если сервер успешно запущен
     */
    public boolean createServerTCP() {
        if(serverSocket!=null)
            if(!serverSocket.isClosed()) {
                RadarBox.logger.add("WIFI","createServerTCP: serverSocket is already created");
                return true;
            }
            try {
                serverSocket = new ServerSocket(tcpServerPort);
                RadarBox.logger.add("WIFI","1) ServerSocket: SO_REUSEADDR: "+serverSocket.getReuseAddress()
                        +"\tAddr: "+serverSocket.getInetAddress()
                        +"\tLockAddr: "+serverSocket.getLocalSocketAddress()
                        +"\tLockPort: "+serverSocket.getLocalPort()
                        +"\tChannel: "+serverSocket.getChannel()
                        +"\tisBound: "+serverSocket.isBound()
                        +"\tSO_TIMEOUT: "+serverSocket.getSoTimeout()
                );
            }
            catch (IOException | SecurityException |
                    IllegalBlockingModeException exception) {
                liveStatusTCP.postValue(StatusTCP.OFF);
                Log.i("WIFI", "createServerTCP: "+exception.getLocalizedMessage());
            }
        return true;
    }

    /** <p>Начать ожидание подключения клиента</p>
     * Запускает блокуирующую функцию accept() в отдельном потоке
     *
     * @return false, если эта функция была запущена при выключенном Wi-Fi или при не созданном
     * серверном сокете, true, если слушающий поток уже существует или, если он был создан
     */
    public boolean startAcceptingClientThread() {
        if(liveStatusWiFi.getValue()!= StatusWiFi.CONNECTED) {
            RadarBox.logger.add("WIFI TCP", "TCP ACCEPT: Wi-Fi is NOT CONNECTED");
            return false;
        }

        if(serverSocket==null) {
            RadarBox.logger.add("WIFI TCP", "TCP ACCEPT: ServerSocket is NULL");
            return false;
        }

        if (serverSocket.isClosed()) {
            RadarBox.logger.add("WIFI TCP", "TCP ACCEPT: ServerSocket is CLOSED");
            //return false;
        }

        if(communicationSocket!=null) {
            if(!communicationSocket.isClosed()) {
                RadarBox.logger.add("WIFI TCP", "TCP ACCEPT: communicationSocket is already OPEN");
                return true;
            }
            return false;
        }
        // запуск функции serverSocket.accept() в отдельном потоке
        new Thread(() -> {
            try {
                liveStatusTCP.postValue(StatusTCP.WAITING);
                liveChannelState.postValue(ChannelState.CONNECTING);
                communicationSocket = serverSocket.accept();
                dataInputStream = new DataInputStream(communicationSocket.getInputStream());
                dataOutputStream = new DataOutputStream(communicationSocket.getOutputStream());
                if (serverSocket.isClosed()) RadarBox.logger.add("WIFI TCP", "1) TCP ACCEPT: ServerSocket is CLOSED");
                else RadarBox.logger.add("WIFI TCP", "1) TCP ACCEPT: ServerSocket is OPENED");
                RadarBox.logger.add("WIFI TCP","2) CommunSocket:"
                        +"\tSO_REUSEADDR: "+communicationSocket.getReuseAddress()
                        +"\tAddr:"+communicationSocket.getInetAddress()
                        +"\tLockAddr:"+communicationSocket.getLocalSocketAddress()
                        +"\tLockPort:"+communicationSocket.getLocalPort()
                        +"\tChannel:"+communicationSocket.getChannel()
                        +"\tSO_OOBINLINE:"+communicationSocket.getOOBInline()
                        +"\tSO_LINGER:"+communicationSocket.getSoLinger()
                        +"\tSO_KEEPALIVE:"+communicationSocket.getKeepAlive()
                        +"\tSO_TIMEOUT:"+communicationSocket.getSoTimeout()
                        +"\tTCP_NODELAY:"+communicationSocket.getTcpNoDelay()
                );
                liveStatusTCP.postValue(StatusTCP.CONNECTED);
                liveChannelState.postValue(ChannelState.CONNECTED);
            } catch (IOException | SecurityException |
                    IllegalBlockingModeException exception) {
                liveStatusTCP.postValue(StatusTCP.OFF);
                liveChannelState.postValue(ChannelState.DISCONNECTED);
                RadarBox.logger.add("WIFI TCP", "TCP ACCEPT: "+exception.getLocalizedMessage());
            }
        }).start();
        return true;
    }

    /** <p>Начать ожидание подключения клиента</p>
     * Запускает блокуирующую функцию accept() в отдельном потоке
     *
     * @return false, если эта функция была запущена при выключенном Wi-Fi или при не созданном
     * серверном сокете, true, если слушающий поток уже существует или, если он был создан
     */
    public boolean connectToServerTCP() {
//        if(liveDataStatusWiFi.getValue()!= StatusWiFi.CONNECTED) {
//            RadarBox.logger.add("WIFI TCP", "CONNECT TO SERVER: Wi-Fi is NOT CONNECTED");
//            return false;
//        }

        if(communicationSocket!=null) {
            if(!communicationSocket.isClosed()) {
                RadarBox.logger.add("WIFI TCP", "CONNECT TO SERVER: communicationSocket is already OPEN");
                return true;
            }
            return false;
        }
        // запуск функции clientSocket.connect() в отдельном потоке
        new Thread(() -> {
            try {
                liveStatusTCP.postValue(StatusTCP.WAITING);
                liveChannelState.postValue(ChannelState.CONNECTING);
                InetAddress serverAddress = InetAddress.getByName(tcpServerAddress);

                communicationSocket = new Socket(serverAddress, tcpServerPort);
                dataInputStream = new DataInputStream(communicationSocket.getInputStream());
                dataOutputStream = new DataOutputStream(communicationSocket.getOutputStream());
                RadarBox.logger.add("WIFI TCP", "CommunSocket:"
                        + "\tSO_REUSEADDR: " + communicationSocket.getReuseAddress()
                        + "\tAddr:" + communicationSocket.getInetAddress()
                        + "\tLockAddr:" + communicationSocket.getLocalSocketAddress()
                        + "\tLockPort:" + communicationSocket.getLocalPort()
                        + "\tChannel:" + communicationSocket.getChannel()
                        + "\tSO_OOBINLINE:" + communicationSocket.getOOBInline()
                        + "\tSO_LINGER:" + communicationSocket.getSoLinger()
                        + "\tSO_KEEPALIVE:" + communicationSocket.getKeepAlive()
                        + "\tSO_TIMEOUT:" + communicationSocket.getSoTimeout()
                        + "\tTCP_NODELAY:" + communicationSocket.getTcpNoDelay()
                );
                liveStatusTCP.postValue(StatusTCP.CONNECTED);
                liveChannelState.postValue(ChannelState.CONNECTED);
            } catch (IOException | SecurityException |
                    IllegalBlockingModeException exception) {
                liveStatusTCP.postValue(StatusTCP.OFF);
                liveChannelState.postValue(ChannelState.DISCONNECTED);
                RadarBox.logger.add("WIFI TCP", "CONNECT TO SERVER: " + exception.getLocalizedMessage());
            }
        }).start();
        return true;
    }

    /** Закрыть TCP соединение.
     */
    public void closeConnectionTCP() {
        if(communicationSocket == null)
            return;
        try {
            dataOutputStream.flush();
            communicationSocket.close();
            communicationSocket = null;
            liveStatusTCP.postValue(StatusTCP.OFF);
            liveChannelState.postValue(ChannelState.DISCONNECTED);
        }
        catch (IOException | SecurityException |
                IllegalBlockingModeException exception) {
            RadarBox.logger.add("WIFI TCP", exception.getLocalizedMessage());
        }
    }

    public void closeServerTCP() {
        if(serverSocket!=null) {
            if(!serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                    serverSocket=null;
                } catch (IOException ioException) {
                    RadarBox.logger.add("WIFI TCP", ioException.getLocalizedMessage());
                }
            }
        }
    }

    /** Отключение от точки доступа Wi-Fi. */
    public void disconnectFromAP() {
        if(serverSocket!=null)
            if(!serverSocket.isClosed())
                closeServerTCP();
        try {
            if(mNetworkCallback!=null)
                mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
            liveStatusWiFi.postValue(StatusWiFi.DISCONNECTED);
        } catch (IllegalArgumentException e) {
            RadarBox.logger.add("WIFI", e.toString());
        }
    }

    @Override
    public boolean connect() {
        return connectToAP();
    }

    @Override
    public boolean disconnect() {
        closeConnectionTCP();
        closeServerTCP();
        disconnectFromAP();
        return true;
    }

    /** <p>Передача данных по Wi-Fi.</p>
     * Перед отправкой данных, проверяется приёмный буфер. Если в нём есть данные, он очищается.
     * Это делается на случай если при прошлом приёме данных, данные не успели принятся
     * за отведённый таймаут.
     * @param data массив данных
     * @return false, если ошибка передачи данных или сокет для передачи данных == null
     */
    @Override
    public boolean send(byte[] data, int timeout) {
        if(communicationSocket==null) {
            return false;
        }
        /* //Проверка отправляемых данных
        RadarBox.logger.add("WIFI TCP","sending data length:("+data.length+
                ")\n\t\tdata:\t"+Logger.toHexString(data));*/
        try {
            if(dataInputStream.available()!=0) {
                RadarBox.logger.add("WIFI TCP", "dataInputStream.available()=" + dataInputStream.available());
                dataInputStream.skip(dataInputStream.available());
            }
            dataOutputStream.write(data);
            dataOutputStream.flush();
        } catch (IOException ioException) {
            ioException.printStackTrace();
            return false;
        }
        return true;
    }

    /** <p>Передача данных по Wi-Fi.</p>
     *
     * @param data массив данных
     * @return false, если ошибка передачи данных, если получено не столько данных, сколько
     * ожидалось или, если сокет для передачи данных == null
     */
    @Override
    public boolean recv(byte[] data, int timeout) {
        if(communicationSocket==null)
            return false;

        try {
            communicationSocket.setSoTimeout(timeout);

            int iByteCount = dataInputStream.read(data);
            while(iByteCount<data.length) {
                iByteCount+=dataInputStream.read(data,iByteCount,data.length-iByteCount);
            }
            /* //Проверка принимаемых данных
            RadarBox.logger.add("WIFI TCP","receiving data length:("+data.length+
                    ") received ("+iByteCount+") bytes\n\t\tdata:\t"+Logger.toHexString(data)); */
            if(iByteCount!=data.length) {
                RadarBox.logger.add("WIFI TCP", "Recieved ("+iByteCount+
                        ") bytes, while expected ("+data.length+") bytes");
                return false;
            }
        } catch (SocketTimeoutException socketTimeoutException) {
            RadarBox.logger.add("WIFI TCP", "TimeOut ("+timeout+")ms on read()"
                    +socketTimeoutException.getLocalizedMessage());
            return false;
        } catch (SocketException e) {
            RadarBox.logger.add("WIFI TCP", "SocketException on read()");
            e.printStackTrace();
            return false;
        } catch (IOException ioException) {
            RadarBox.logger.add("WIFI TCP", "IOException on read()");
            ioException.printStackTrace();
            return false;
        }
        return true;
    }
}
