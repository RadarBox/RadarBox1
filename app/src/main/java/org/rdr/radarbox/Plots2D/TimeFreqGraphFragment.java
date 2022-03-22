package org.rdr.radarbox.Plots2D;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import org.rdr.radarbox.Plots2D.GraphView;
import org.rdr.radarbox.R;
import org.rdr.radarbox.RadarBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

public class TimeFreqGraphFragment extends Fragment {
    GraphView freqGraphView, timeGraphView;
    private SharedPreferences pref;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.time_freq_graph_fragment,container,false);
        pref = PreferenceManager.getDefaultSharedPreferences(this.requireContext());
        freqGraphView = view.findViewById(R.id.graph);
        freqGraphView.setxMin(pref.getFloat("FreqGraphView"+"xMin",1000));
        freqGraphView.setxMax(pref.getFloat("FreqGraphView"+"xMax",3000));
        freqGraphView.setyMax(pref.getFloat("FreqGraphView"+"yMax",3000));
        freqGraphView.setyMin(pref.getFloat("FreqGraphView"+"yMin",-3000));
        resetAllFreqLines();
        // обновление графика происходит при получении нового кадра, номер кадра передаётся в качестве аргумента
        RadarBox.dataThreadService.getLiveFrameCounter().observe(getViewLifecycleOwner(),this::update);

        Button settingsButton = view.findViewById(R.id.btn_grSettings);
        assert settingsButton!=null;
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GraphSettingsFragment graphSettingsFragment = new GraphSettingsFragment();
                // Передаём весь объект с графиками в бандл, чтобы его мог открыть фрагмент с настройками
                Bundle args = new Bundle();
                args.putSerializable("GraphView", freqGraphView);
                graphSettingsFragment.setArguments(args);

                getParentFragmentManager()
                        .beginTransaction()
                        .add(R.id.graph_settings_container, graphSettingsFragment)
                        .addToBackStack(null)
                        .commit();

            }
        });

        view.findViewById(R.id.graph_settings_container).addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {

            FrameLayout graphContainer = view.findViewById(R.id.graph_container);
            graphContainer.post(() -> {
                ViewGroup.LayoutParams params = graphContainer.getLayoutParams();
                params.height = view.getHeight()-
                        view.findViewById(R.id.graph_settings_container).getHeight();
                graphContainer.setLayoutParams(params);
            });
        });

        getParentFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {
                        if(getParentFragmentManager().getBackStackEntryCount()==0) {
                            settingsButton.setVisibility(View.VISIBLE);
                        }
                        else {
                            settingsButton.setVisibility(View.GONE);
                        }
                    }
                });
        view.setOnKeyListener( new View.OnKeyListener() {
            @Override
            public boolean onKey( View v, int keyCode, KeyEvent event )
            {
                if( keyCode == KeyEvent.KEYCODE_BACK ) {
                    settingsButton.setVisibility(View.VISIBLE);
                    return true;
                }
                else return false;
            }
        } );

        return view;
    }

    private void resetAllFreqLines() {
        if(!freqGraphView.getLines().isEmpty())
            freqGraphView.getLines().clear();

        double[] tempY = new double[1];
        int rxN = RadarBox.freqSignals.getRxN();
        int txN = RadarBox.freqSignals.getTxN();
        int chN = rxN*txN;

        for(int rx = 0; rx<rxN; rx++) {
            for(int tx=0; tx<txN; tx++) {
                int line = rx*txN+tx;
                freqGraphView.addLine(new Line2D(tempY, tempY,
                        GraphColor.values()[line%GraphColor.values().length].argb,
                        "r"+rx+"t"+tx+"re"));
                freqGraphView.addLine(new Line2D(tempY, tempY,
                        GraphColor.values()[(line+1)%GraphColor.values().length].argb,
                        "r"+rx+"t"+tx+"im"));
                freqGraphView.addLine(new Line2D(tempY, tempY,
                        GraphColor.values()[(line+2)%GraphColor.values().length].argb,
                        "r"+rx+"t"+tx+"abs"));
            }
        }
    }

    private void update(long frameNumber) {
        int rxN = RadarBox.freqSignals.getRxN();
        int txN = RadarBox.freqSignals.getTxN();
        int chN = rxN*txN;
        if(freqGraphView.getLines().size()!=chN*3)
            resetAllFreqLines();

        double[] tempVector = new double[RadarBox.freqSignals.getFrequenciesMHz().length];
        for(int i = 0; i<tempVector.length;i++)
            tempVector[i]=RadarBox.freqSignals.getFrequenciesMHz()[i];
        // координата X
        for(int rx = 0; rx<rxN; rx++) {
            for (int tx = 0; tx < txN; tx++) {
                freqGraphView.getLine("r" + rx + "t" + tx + "re").setX(tempVector);
                freqGraphView.getLine("r" + rx + "t" + tx + "im").setX(tempVector);
                freqGraphView.getLine("r" + rx + "t" + tx + "abs").setX(tempVector);
            }
        }
        // координата Y
        short[] rawFreqSignal = new short[RadarBox.freqSignals.getFN()*2];
        for(int rx = 0; rx<rxN; rx++) {
            for (int tx = 0; tx<txN; tx++) {
                if(RadarBox.freqSignals.getRawFreqOneChannelSignal(rx,tx,rawFreqSignal)>=0) {
                    for (int i = 0; i < tempVector.length; i++)
                        tempVector[i] = rawFreqSignal[2 * i];
                    freqGraphView.getLine("r" + rx + "t" + tx + "re").setY(tempVector);
                    for (int i = 0; i < tempVector.length; i++)
                        tempVector[i] = rawFreqSignal[2 * i + 1];
                    freqGraphView.getLine("r" + rx + "t" + tx + "im").setY(tempVector);
                    for(int i =0; i<tempVector.length; i++)
                        tempVector[i]=Math.sqrt(rawFreqSignal[2*i]*rawFreqSignal[2*i]
                                +rawFreqSignal[2*i+1]*rawFreqSignal[2*i+1]);
                    freqGraphView.getLine("r" + rx + "t" + tx + "abs").setY(tempVector);
                }
            }
        }
        freqGraphView.invalidate();
    }

    @Override
    public void onPause() {
        pref.edit().putFloat("FreqGraphView"+"xMax",(float)freqGraphView.getxMax()).apply();
        pref.edit().putFloat("FreqGraphView"+"xMin",(float)freqGraphView.getxMin()).apply();
        pref.edit().putFloat("FreqGraphView"+"yMax",(float)freqGraphView.getyMax()).apply();
        pref.edit().putFloat("FreqGraphView"+"yMin",(float)freqGraphView.getyMin()).apply();
        super.onPause();
    }
}
