package org.rdr.radarbox.DSP;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import org.rdr.radarbox.R;

/** Акативность, которая вызывается при нажатии на иконку с ползунками.
 * В ней задаётся конфигурация для блока цифровой обработки сигналов. В том числе
 */
public class SettingsDSP extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_dsp);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_dsp_container, new SettingsDSP.SettingsDspFragment())
                    .commit();
        }
    }


    public static class SettingsDspFragment extends PreferenceFragmentCompat{
        static int select_signal;
        ListPreference pref;

        Preference.OnPreferenceChangeListener listener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (preference instanceof CheckBoxPreference) {
                    boolean checkValue = Boolean.parseBoolean(newValue.toString());
                    ((CheckBoxPreference) preference).setChecked(checkValue);
                }
                else if (preference instanceof ListPreference){
                    pref = findPreference("select_signal");
                    assert pref != null;
                    select_signal = pref.findIndexOfValue(newValue.toString());
                    pref.setDefaultValue(newValue.toString());
                    String[] strings = getResources().getStringArray(R.array.select_freq_chart);
                    pref.setSummary(strings[select_signal]);
                    pref.setValueIndex(select_signal);
                }
                return false;
            }
        };

        void bindSummaryValue(Preference preference){
            preference.setOnPreferenceChangeListener(listener);
            listener.onPreferenceChange(preference,
                    PreferenceManager.getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(),""));
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_settings_dsp,rootKey);

            pref = findPreference("select_signal");
            assert pref != null;
            bindSummaryValue(pref);
            pref.setValueIndex(select_signal);
            String[] array = getResources().getStringArray(R.array.select_freq_chart);
            pref.setSummary(array[select_signal]);
        }

        public static void restorePreferences(Context context) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            select_signal = Integer.parseInt(prefs.getString("select_signal","0"));
        }
    }

}
