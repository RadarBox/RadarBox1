package org.rdr.radarbox.Device;

import android.os.Bundle;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.rdr.radarbox.R;
import org.rdr.radarbox.RadarBox;

import java.util.ArrayList;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.widget.NestedScrollView;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;

public class DeviceConfigurationFragment extends Fragment {
    DeviceConfiguration deviceConfiguration;
    public DeviceConfigurationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(getArguments()!=null) {
            if(getArguments().getString("DeviceConfiguration").equals("Real"))
                deviceConfiguration = RadarBox.device.configuration;
            else if(getArguments().getString("DeviceConfiguration").equals("Virtual"))
                deviceConfiguration = RadarBox.fileReader.getVirtualDeviceConfiguration();
        }
        else {
            deviceConfiguration = RadarBox.device.configuration;
        }
        NestedScrollView scrollView = new NestedScrollView(container.getContext());
        LinearLayoutCompat linearLayout = new LinearLayoutCompat(scrollView.getContext());
        linearLayout.setOrientation(LinearLayoutCompat.VERTICAL);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scrollView.addView(linearLayout);
        TextView deviceInfoTextView = new TextView(linearLayout.getContext());
        String deviceInfoString = deviceConfiguration.getDevicePrefix()+"\t"+
                deviceConfiguration.getDeviceName()+"\t"+
                "Rx:"+deviceConfiguration.getRxN()+" "+
                "Tx:"+deviceConfiguration.getTxN();
        deviceInfoTextView.setText(deviceInfoString); deviceInfoTextView.setSingleLine();
        linearLayout.addView(deviceInfoTextView);
        ArrayList<DeviceConfiguration.Parameter>parameters=deviceConfiguration.getParameters();
        for (DeviceConfiguration.Parameter parameter:parameters) {
            if(parameter.getDefault().getClass().equals(Integer.class)) {
                DeviceConfiguration.IntegerParameter iParam =
                        (DeviceConfiguration.IntegerParameter) parameter;

                TextInputLayout iTextLayout = new TextInputLayout(this.requireContext());
                iTextLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                iTextLayout.setHintEnabled(true); iTextLayout.setHint(iParam.getName()+
                        " ["+Integer.toString(iParam.getMin(),iParam.getRadix())+
                        ":"+Integer.toString(iParam.getStep(),iParam.getRadix())+
                        ":"+Integer.toString(iParam.getMax(),iParam.getRadix())+"]");
                iTextLayout.setHelperTextEnabled(true); iTextLayout.setHelperText(iParam.getSummary());
                TextInputEditText iEditText = new TextInputEditText(iTextLayout.getContext());
                if(iParam.getRadix()==10) {
                    iEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
                } else if(iParam.getRadix()==16) {
                    iEditText.setInputType(InputType.TYPE_CLASS_TEXT);
                    iEditText.setKeyListener(DigitsKeyListener.getInstance("0123456789AaBbCcDdEeFf"));
                }
                iEditText.setText(Integer.toString(iParam.getValue(),iParam.getRadix()));

                final String[] oldText = new String[1];
                iEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if(hasFocus)
                            oldText[0] =iEditText.getText().toString();
                        else if(iEditText.getText().toString().isEmpty() ||
                                !iParam.setValue(Integer.parseInt(
                                        iEditText.getText().toString(),iParam.getRadix())))
                            iEditText.setText(oldText[0]);
                    }
                });
                ((LinearLayoutCompat) linearLayout).addView(iTextLayout);
                iTextLayout.addView(iEditText);
            }
            if(parameter.getDefault().getClass().equals(Boolean.class)) {
                DeviceConfiguration.BooleanParameter bParam = (DeviceConfiguration.BooleanParameter) parameter;
                SwitchMaterial bSwitch = new SwitchMaterial(this.requireContext());
                bSwitch.setUseMaterialThemeColors(true);
                bSwitch.setText(bParam.getName()+"\n("+bParam.getSummary()+")");
                bSwitch.setTextSize(12);
                bSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> bParam.setValue(isChecked));
                ((LinearLayoutCompat) linearLayout).addView(bSwitch);
            }
        }
        return scrollView;
    }
}