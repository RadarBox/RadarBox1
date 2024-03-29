package org.rdr.radarbox;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import org.rdr.radarbox.Device.DeviceConfiguration;
import org.rdr.radarbox.Device.DeviceConfigurationFragment;
import org.rdr.radarbox.File.AoRDSettingsManager;
import org.rdr.radarbox.File.AoRDSender;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        TextView textViewLogger = findViewById(R.id.logger_view);
        final NestedScrollView scrollView = (NestedScrollView)findViewById(R.id.logger_scroll);
        try {
            textViewLogger.setText(
                    new String(Files.readAllBytes(RadarBox.logger.getFileLog().toPath())));
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        OnLongClickFileOpener fileOpener = new OnLongClickFileOpener(this,
                RadarBox.logger.getFileLog().getPath());
        textViewLogger.setOnLongClickListener(fileOpener);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (savedInstanceState == null) {
            RadarBox.logger.getLiveLastStringWritten().observe(this,lastString-> {
                TextView textViewLogger = findViewById(R.id.logger_view);
                textViewLogger.setMovementMethod(new ScrollingMovementMethod());
                textViewLogger.append("\n"+lastString);
                NestedScrollView scrollView = findViewById(R.id.logger_scroll);
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });
            });
        }
    }

    private static class OnLongClickFileOpener implements View.OnLongClickListener {
        private Context parentObject;
        private String filePath;

        public OnLongClickFileOpener(Context parent, String path) {
            parentObject = parent;
            filePath = path;
        }

        @Override
        public boolean onLongClick(View view) {
            File file = new File(filePath);

            Uri uri = FileProvider.getUriForFile(parentObject,
                    "org.rdr.radarbox.file_provider", file);
            String mime = parentObject.getContentResolver().getType(uri);

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chosenIntent = Intent.createChooser(intent, "Открыть файл в...");
            parentObject.startActivity(chosenIntent);
            return false;
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            // список всех устройств
            final ListPreference deviceList = findPreference("last_connected_device");
            assert deviceList != null;
            RadarBox radarBox = RadarBox.getInstance();
            deviceList.setEntryValues(radarBox.getDevicePrefixList());
            deviceList.setEntries(deviceList.getEntryValues());
            /* редкий случай при первом запуске приложения,
             когда ничего не выбрано в списке, но текущее устройство в RadarBox уже установлено */
            if(RadarBox.device!=null &&
                    !RadarBox.device.getDevicePrefix().equals(deviceList.getValue()))
                deviceList.setValue(RadarBox.device.getDevicePrefix());
            deviceList.setOnPreferenceChangeListener(((preference, newValue) ->
                    radarBox.setCurrentDevice(newValue.toString())));
            Preference currentDevice = findPreference("current_device");
            assert currentDevice != null;
            if (RadarBox.device != null) {
                RadarBox.device.communication.getLiveConnectedChannel().observe(this,
                        connectedDataChannel -> {
                            if (connectedDataChannel == null)
                                currentDevice.setSummary(R.string.current_device_no_device);
                            else
                                currentDevice.setSummary(
                                        getResources().getString(R.string.current_device_channel) + ": " +
                                                connectedDataChannel.getName());
                        });
            }

            // имя файла для чтения данных
            final ListPreference readFileName = findPreference("file_reader_filename");
            assert readFileName != null;
            String[] fileListAtStart = AoRDSettingsManager.getFilesList();
            readFileName.setEntryValues(fileListAtStart);
            readFileName.setEntries(readFileName.getEntryValues());
            if(fileListAtStart.length>0) {
                /* редкий случай при первом запуске приложения,
             когда ничего не выбрано в списке, но файлы в директории присутствуют.
             Тогда в качестве значения выбирается первый файл.*/
                if(readFileName.getValue()==null)
                    readFileName.setValue(fileListAtStart[0]);
            }

            readFileName.setOnPreferenceChangeListener((preference, newValue) -> {
                RadarBox.setAoRDFile(RadarBox.FILE_READ_KEY,
                        AoRDSettingsManager.getFileByName(newValue.toString()));
                if (RadarBox.fileRead == null) {
                    RadarBox.logger.add(this,"AoRDFile " + newValue + " isn`t enabled");
                    return false;
                }
                if (RadarBox.fileRead.config.getVirtual() == null) {
                    RadarBox.logger.add(this,
                            "FileReader.getVirtualDeviceConfiguration() is NULL");
                    return false;
                }
                if(!RadarBox.freqSignals.updateSignalParameters(
                        RadarBox.fileRead.config.getVirtual())) {
                    RadarBox.logger.add(this, "FreqSignals.updateSignalParameters(" +
                            RadarBox.fileRead.config.getVirtual().getDevicePrefix() +
                            ") is FALSE");
                    return false;
                }
                int period = RadarBox.fileRead.config.getVirtual()
                        .getIntParameterValue("Trep");
                if (period == -1) {
                    RadarBox.logger.add(this.getClass(), "Parameter Trep doesn't exist");
                    return false;
                }
                RadarBox.dataThreadService.setPeriod(period);
                RadarBox.device.configuration.getParameters().stream().filter(
                        parameter -> parameter.getID().equals("Trep")
                ).findAny().ifPresent(parameter ->
                        ((DeviceConfiguration.IntegerParameter)parameter).getLiveValue()
                                .observeForever(value->RadarBox.dataThreadService.setPeriod(value)));
                // Если выбрано "Отправлять файлы", то вызвать диалог, отправляющий файл
                if (PreferenceManager.getDefaultSharedPreferences(
                                this.requireContext()).getBoolean("need_send",false))
                    AoRDSender.createDialogToSendAoRDFile(this.requireContext(),
                            RadarBox.fileRead);

                return true;
            });
            // разрешение на запись файла данных
            final SwitchPreferenceCompat needSave = findPreference("need_save");
            assert needSave != null;
            needSave.setOnPreferenceChangeListener(((preference, newValue) -> {
                if(!RadarBox.dataThreadService.getLiveCurrentSource()
                        .getValue().equals(DataThreadService.DataSource.NO_SOURCE)) // TODO заменить на DEVICE и убрать ! после тестов
                    AoRDSettingsManager.needSaveData = Boolean.parseBoolean(newValue.toString());
                return true;
            }));
            RadarBox.dataThreadService.getLiveCurrentSource().observe(this,
                    currentDataSource->{
                if(!currentDataSource.equals(DataThreadService.DataSource.NO_SOURCE)) { // TODO заменить на DEVICE и убрать ! после тестов
                    needSave.setEnabled(true);
                }
                else {
                    needSave.getOnPreferenceChangeListener().onPreferenceChange(needSave,
                            Boolean.FALSE);
                    needSave.setChecked(false);
                    needSave.setEnabled(false);
                }
            });
            // разрешение на чтение файла данных
            final SwitchPreferenceCompat needRead = findPreference("need_read");
            assert needRead != null;
            needRead.setChecked(RadarBox.dataThreadService.getLiveCurrentSource().getValue()
                    .equals(DataThreadService.DataSource.FILE));
            if(needRead.isChecked()) {
                needSave.setEnabled(false);
            }
            needRead.setOnPreferenceChangeListener(((preference, newValue) -> {
                if(Boolean.parseBoolean(newValue.toString())) {
                    if (readFileName.getEntries().length == 0) {
                        RadarBox.logger.add(this, "No files for reading in data directory");
                        return false;
                    }
                    // если в данный момент в списке файлов не выбран не один из них,
                    // то установить первый
                    if (!Arrays.asList(readFileName.getEntries()).contains(readFileName.getValue()))
                        readFileName.setValue(readFileName.getEntries()[0].toString());
                    // если в RadarBox.fileRead сейчас не находится никакой файл ЛИБО
                    // если выбранный файл из списка не соответствует файлу RadarBox.fileRead
                    if (RadarBox.fileRead == null ||
                            !RadarBox.fileRead.getName().equals(readFileName.getValue())) {
                        RadarBox.logger.add(this, "File " + readFileName.getValue() +
                                " was not opened. Opening it...");
                        // попытаться установить открыть файл для чтения
                        RadarBox.setAoRDFile(RadarBox.FILE_READ_KEY,
                                AoRDSettingsManager.getFileByName(readFileName.getValue()));
                        if (RadarBox.fileRead == null) {
                            RadarBox.logger.add(this, "ERROR on opening File " +
                                    readFileName.getValue());
                            return false;
                        }
                    }
                    if (RadarBox.dataThreadService.setDataSource(DataThreadService.DataSource.FILE))
                        return true;
                    else
                        return false;
                }
                else {
                    // перестать читать из файла
                    if(!RadarBox.dataThreadService.setDataSource(
                            DataThreadService.DataSource.DEVICE))
                        RadarBox.dataThreadService.setDataSource(
                                DataThreadService.DataSource.NO_SOURCE);
                    return true;
                }
            }));
            // показать конфигурацию устройства для текущего файла
            final Preference readFileHeader = findPreference("read_file_config");
            assert readFileHeader != null;
            readFileHeader.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if(!readFileName.getSummary().toString().isEmpty() &&
                    RadarBox.fileRead.config.getVirtual() != null) {
                        DeviceConfigurationFragment fragment = new DeviceConfigurationFragment();
                        Bundle args = new Bundle();
                        args.putString("DeviceConfiguration",
                                "Virtual");
                        fragment.setArguments(args);
                        getParentFragmentManager().beginTransaction()
                                .setReorderingAllowed(true)
                                .replace(R.id.settings_container,fragment)
                                .addToBackStack(null).commit();
                        return true;
                    }
                    return false;
                }
            });
            // ввод постфикса для имени файла при записи
            final EditTextPreference writeFileNamePrefix = findPreference(
                    "file_writer_filename");
            assert writeFileNamePrefix != null;
            writeFileNamePrefix.setOnBindEditTextListener(editText -> {
                editText.setSingleLine(true);
            });
            writeFileNamePrefix.setOnPreferenceChangeListener((preference, newValue) -> {
                AoRDSettingsManager.setFileNamePostfix(newValue.toString());
                return true;
            });
            // Отправка файлов по сети при записи
            final SwitchPreferenceCompat needSend = findPreference("need_send");
            assert needSend != null;
            needSend.setOnPreferenceChangeListener(((preference, newValue) -> true));
            RadarBox.dataThreadService.getLiveCurrentSource().observe(this,
                    currentDataSource->{
                        if(!currentDataSource.equals(DataThreadService.DataSource.NO_SOURCE)) {
                            needSend.setEnabled(true);
                        }
                        else {
                            needSend.getOnPreferenceChangeListener().onPreferenceChange(needSend,
                                    Boolean.FALSE);
                            needSend.setChecked(false);
                            needSend.setEnabled(false);
                        }
                    });
        }
    }
}
