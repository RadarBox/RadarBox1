package org.rdr.radarbox.DSP;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import org.rdr.radarbox.R;
import org.rdr.radarbox.RadarBox;

/**
 * Класс для оценки отношения сигнал-шум. Внутри него можно получать актульную информацию об
 * актульаных средних значениях ОСШ {@link #getAvgSNR()},
 * о максимальном ОСШ {@link #getMaxSNR()}
 * @author Козлов Роман Юрьевич
 * @version 0.1
 */

public class SNR extends PreferenceFragmentCompat {
    static int nAccumulated = 10;
    static int nSnrAccumulated = 10;
    double[][] accumulatedData;
    double[] arrayAvgSNR;
    static double maxSNR = 0;
    static double avgSNR = 0;
    static double[][] arraySNR;
    private int iFrame = 0;

    public SNR(int length){
        reinitSNR(length);
    }

    EditTextPreference pref;
    Preference.OnPreferenceChangeListener listener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String stringValue = newValue.toString();
            if (preference instanceof EditTextPreference) {
                preference.setSummary(stringValue);
                ((EditTextPreference) preference).setText(stringValue);
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
        setPreferencesFromResource(R.xml.settings_dsp_snr,rootKey);

            pref = findPreference("nAccumulated");
            assert pref != null;
            bindSummaryValue(pref);
            pref.setSummary(pref.getText());
//        pref.setText(Integer.toString(nAccumulated));
            pref.setDefaultValue(Integer.toString(nAccumulated));

        pref = findPreference("nSnrAccumulated");
        assert pref != null;
        bindSummaryValue(pref);
        pref.setSummary(pref.getText());
//        pref.setText(Integer.toString(nSnrAccumulated));
        pref.setDefaultValue(Integer.toString(nSnrAccumulated));

    }

    public void reinitSNR(int nF){
        accumulatedData = new double[nAccumulated][nF];
        arrayAvgSNR = new double[nF];
    }

    public void calculateSNR(double[] rawFreqFrame) {
        int nF = rawFreqFrame.length;
        if (nF!=arrayAvgSNR.length)
            reinitSNR(nF);
        // Raw Data Accumulation
        for (int f = 0; f < nF; f++)
            accumulatedData[iFrame][f] = rawFreqFrame[f];
        iFrame++;
        if (iFrame >= nAccumulated)
            iFrame = 0;


        // Null SNRs
        for (int i = 0; i < nSnrAccumulated; i++)
            arrayAvgSNR[i] = 0;

        arraySNR = new double[nSnrAccumulated][nF];
        // Variance Calculation
        float mu;
        for (int f = 0; f < nF; f++) {
            // Mu
            mu = 0;
            for (int n = 0; n < nAccumulated; n++)
                mu += accumulatedData[n][f];
            mu /= nAccumulated;
            // Variance
//            for (int n = 0; n < nAccumulated; n++)
//                arraySNR[iSNR][f] = (float) Math.pow((accumulatedData[n][f] - mu), 2);
//            arraySNR[iSNR][f] /= nAccumulated - 1;
//             Average SNR
            for (int n=0; n<nAccumulated; n++)
                arrayAvgSNR[f] += (float) Math.pow((accumulatedData[n][f] - mu), 2);
            arrayAvgSNR[f] /= nAccumulated-1;
            arrayAvgSNR[f] *= 5;
//            iSNR++;
//            if (iSNR >= nSnrAccumulated)
//                iSNR = 0;
        }
    }

    public double getAvgSNR() {
        return avgSNR;
    }
    public double getMaxSNR() {
        return maxSNR;
    }
    public double[] getArrayAvgSNR() {
        return arrayAvgSNR;
    }
}
