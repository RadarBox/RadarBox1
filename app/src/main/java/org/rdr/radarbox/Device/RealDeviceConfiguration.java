package org.rdr.radarbox.Device;

import android.content.Context;

import org.rdr.radarbox.RadarBox;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import androidx.preference.PreferenceManager;

/** Класс наследник {@link #DeviceConfiguration} для считывания конфигурации устройства
 * из конфигурационного xml файла в директории с названием {@link #devicePrefix} в assets
 */
public class RealDeviceConfiguration extends DeviceConfiguration {
    public RealDeviceConfiguration(Context context, String devicePrefix) {
        super(context, devicePrefix);
        try {
            parseConfiguration(context.getAssets()
                    .open(devicePrefix + "/configuration.xml"));
        } catch (XmlPullParserException | IOException e) {
            RadarBox.logger.add(deviceName + " CONFIG",
                    "parseConfiguration ERROR: " + e.getLocalizedMessage());
        }
        if(rxN==1&&txN==1) {
            rxtxOrder = new int[1];
            rxtxOrder[0]=1;
        }
        else if(rxN>1 || txN>1) {
            rxtxOrder=new int[rxN*txN];
            for (int r = 0; r < rxN; r++)
                for (int t = 0; t < txN; t++)
                    rxtxOrder[t*rxN+r] = t * rxN + r + 1;
        }
    }
}
