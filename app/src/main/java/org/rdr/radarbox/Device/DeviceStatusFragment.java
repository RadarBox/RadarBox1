package org.rdr.radarbox.Device;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import org.rdr.radarbox.R;
import org.rdr.radarbox.RadarBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

public class DeviceStatusFragment extends Fragment {
    public DeviceStatusFragment() {

    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
//        View view = inflater.inflate(R.id.device_container, container, false);
        LinearLayoutCompat linearLayout = new LinearLayoutCompat(container.getContext());
        linearLayout.setLayoutParams(new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayout.setOrientation(LinearLayoutCompat.VERTICAL);
        NestedScrollView scrollView = new NestedScrollView(container.getContext());
        scrollView.setLayoutParams(new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scrollView.addView(linearLayout);
        //((NestedScrollView)view).addView(scrollView);

        LinearLayoutCompat.LayoutParams layoutParams =new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        for (Object statusEntry:RadarBox.device.status.getStatusList()) {
            if(statusEntry.getClass().getSuperclass().equals(DeviceStatus.SimpleStatusEntry.class)) {
                DeviceStatus.SimpleStatusEntry simple = (DeviceStatus.SimpleStatusEntry) statusEntry;
                LinearLayoutCompat oneRaw = new LinearLayoutCompat(linearLayout.getContext());
                oneRaw.setLayoutParams(layoutParams);
                oneRaw.setOrientation(LinearLayoutCompat.HORIZONTAL);
                TextView textViewName = new TextView(linearLayout.getContext());
                textViewName.setText(simple.getName());
                layoutParams.weight=1.0f;
                textViewName.setLayoutParams(layoutParams);
                oneRaw.addView(textViewName);

                TextView textViewValue = new TextView(linearLayout.getContext());
                textViewName.setLayoutParams(layoutParams);
                textViewValue.setTypeface(null,Typeface.BOLD);
                layoutParams.weight=4.0f;
                textViewValue.setGravity(Gravity.RIGHT);
                textViewValue.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
//                textViewValue.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
                oneRaw.addView(textViewValue);

                if(!simple.getSummary().isEmpty())
                    oneRaw.setOnClickListener(v ->
                            Snackbar.make(oneRaw, simple.getSummary(), Snackbar.LENGTH_SHORT)
                                    .show());

                if(statusEntry.getClass().equals(DeviceStatus.IntegerStatusEntry.class))
                    textViewValue.setText(Integer.toString(
                            ((DeviceStatus.IntegerStatusEntry) simple).getValue()));
                else if(statusEntry.getClass().equals(DeviceStatus.FloatStatusEntry.class))
                    textViewValue.setText(Float.toString(((DeviceStatus.FloatStatusEntry) simple).getValue()));

                linearLayout.addView(oneRaw);
            }
            if(statusEntry.getClass().equals(DeviceStatus.ComplexStatusEntry.class)) {
                DeviceStatus.ComplexStatusEntry complex = (DeviceStatus.ComplexStatusEntry) statusEntry;
                TableLayout complexTable = new TableLayout(this.requireContext());
                layoutParams.weight=1.0f;
                complexTable.setLayoutParams(layoutParams);
                TableRow firstRow = new TableRow(complexTable.getContext());
                firstRow.setLayoutParams(layoutParams);
                TextView complexStatName = new TextView(firstRow.getContext());
                complexStatName.setText(complex.getID()+"\t"+complex.getName());
                complexStatName.setAllCaps(true);
                complexStatName.setTypeface(null, Typeface.BOLD);
                firstRow.addView(complexStatName,0);
                complexTable.addView(firstRow);
                for (DeviceStatus.ComplexStatusEntry.Bit bit: complex.getBits()) {
                    TableRow nextRow = new TableRow(complexTable.getContext());
                    layoutParams.weight=1.0f;
                    nextRow.setLayoutParams(layoutParams);
                    TextView bitName = new TextView(firstRow.getContext()); bitName.setText(bit.bitName());
                    TextView bitValue = new TextView(nextRow.getContext()); bitValue.setText(Integer.toString(bit.getBitVal()));
                    bitValue.setGravity(Gravity.RIGHT);
                    bitValue.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
                    nextRow.addView(bitName);
                    nextRow.addView(bitValue);
                    complexTable.addView(nextRow);
                    if(!bit.bitSummary().isEmpty())
                        nextRow.setOnClickListener(v ->
                                Snackbar.make(nextRow, bit.bitSummary(), Snackbar.LENGTH_SHORT)
                                        .show());
                }
                linearLayout.addView(complexTable);
            }
        }
        return scrollView;
    }
}
