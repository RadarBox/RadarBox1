package org.rdr.radarbox.Plots2D;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

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
    GraphSettingsFragment graphSettingsFragment = null;
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
        // создание фрагмента с настройками графика, который будет выдвигаться с помощью свайпа
        graphSettingsFragment = new GraphSettingsFragment();
        // Передаём весь объект с графиками в бандл, чтобы его мог открыть фрагмент с настройками
        Bundle args = new Bundle();
        args.putSerializable("GraphView", freqGraphView);
        graphSettingsFragment.setArguments(args);
        // Настраиваем действие свайпа вверх-вниз, когда ориентация вертикальная и влево-вправо, когда горизонтальная
        final GestureDetector gestureDetector = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                final int SWIPE_MIN_DISTANCE = 120;
                final int SWIPE_MAX_OFF_PATH = 250;
                final int SWIPE_THRESHOLD_VELOCITY = 200;
                try {
                    int orientation = view.getContext().getResources().getConfiguration().orientation;
                    if(orientation == Configuration.ORIENTATION_PORTRAIT) {
                        if(Math.abs(e1.getX() - e2.getX()) > SWIPE_MAX_OFF_PATH)
                            return false;
                        if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE
                                && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                            openGraphSettings();
                        }
                        else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE
                                && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                            closeGraphSettings();
                        }
                    }
                    else { // ГОРИЗОНТАЛЬНАЯ ОРИЕНТАЦИЯ
                        if(Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                            return false;
                        if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                                && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                            openGraphSettings();
                        } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                                && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                            closeGraphSettings();
                        }
                    }
                } catch (Exception e) {
                    // nothing
                }
                return super.onFling(e1, e2, velocityX, velocityY);
            }
        });
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });

        view.findViewById(R.id.graph_settings_container).addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {

            FrameLayout graphContainer = view.findViewById(R.id.graph_container);
            graphContainer.post(() -> {
                ViewGroup.LayoutParams params = graphContainer.getLayoutParams();
                int orientation = view.getContext().getResources().getConfiguration().orientation;
                if(orientation == Configuration.ORIENTATION_PORTRAIT) {
                    params.height = view.getHeight() -
                            view.findViewById(R.id.graph_settings_container).getHeight();
                }
                else {
                    params.width = view.getWidth() -
                            view.findViewById(R.id.graph_settings_container).getWidth();
                }
                graphContainer.setLayoutParams(params);
            });
        });

        return view;
    }

    private void openGraphSettings() {
        if(!graphSettingsFragment.isAdded())
            getParentFragmentManager()
                    .beginTransaction()
                    .add(R.id.graph_settings_container, graphSettingsFragment)
                    .addToBackStack(null)
                    .commit();
    }

    private void closeGraphSettings() {
        if(graphSettingsFragment.isAdded())
            getParentFragmentManager()
                    .beginTransaction()
                    .remove(graphSettingsFragment)
                    .commit();
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
        if(graphSettingsFragment.isAdded())
            closeGraphSettings();
        super.onPause();
    }
}
