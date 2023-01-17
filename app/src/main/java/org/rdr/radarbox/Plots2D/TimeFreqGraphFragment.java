package org.rdr.radarbox.Plots2D;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.rdr.radarbox.DSP.Operations.OperationDSP;
import org.rdr.radarbox.DSP.SNR;
import org.rdr.radarbox.DSP.SettingsDSP;
import org.rdr.radarbox.R;
import org.rdr.radarbox.RadarBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class TimeFreqGraphFragment extends Fragment {
    GraphView graphView;
    GraphSettingsFragment graphSettingsFragment = null;
    private SharedPreferences pref;
    private List<SNR> listSnr;

    private static final char
    SELECT_RAW = 0,
    SELECT_SNR = 1,
    SELECT_FFT = 2;
    private int flag;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // добавить проверку savedInstanceState
        View view = inflater.inflate(R.layout.time_freq_graph_fragment,container,false);
        pref = PreferenceManager.getDefaultSharedPreferences(this.requireContext());
        graphView = view.findViewById(R.id.graph);
        graphView.setxMin(pref.getFloat("GraphView"+"xMin",1000));
        graphView.setxMax(pref.getFloat("GraphView"+"xMax",3000));
        graphView.setyMax(pref.getFloat("GraphView"+"yMax",3000));
        graphView.setyMin(pref.getFloat("GraphView"+"yMin",-3000));
        listSnr = new ArrayList<SNR>();
        resetAllLines();
        // обновление графика происходит при получении нового кадра, номер кадра передаётся в качестве аргумента
        RadarBox.dataThreadService.getLiveFrameCounter().observe(getViewLifecycleOwner(),this::update);
        flag = Integer.parseInt(pref.getString("select_signal","0"));
        SettingsDSP.SettingsDspFragment.restorePreferences(getContext());// создание фрагмента с настройками графика, который будет выдвигаться с помощью свайпа
        // создание фрагмента с настройками графика, который будет выдвигаться с помощью свайпа
        graphSettingsFragment = new GraphSettingsFragment();
        // Передаём весь объект с графиками в бандл, чтобы его мог открыть фрагмент с настройками
        Bundle args = new Bundle();
        args.putSerializable("GraphView", graphView);
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

        flag = Integer.parseInt(pref.getString("select_signal","0"));
        SettingsDSP.SettingsDspFragment.restorePreferences(getContext());
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

    private void resetAllLines() {
        if(!graphView.getLines().isEmpty())
            graphView.getLines().clear();

        flag = Integer.parseInt(pref.getString("select_signal","0"));
        if (flag==SELECT_RAW)
            resetFreqLines();
        else if (flag==SELECT_SNR) {
            resetSNRLines();
            resetSNR();
        } else if (flag==SELECT_FFT) {
            resetTimeLines();
        }
    }

    private void resetFreqLines() {
        float[] tempY = new float[1];
        int rxN = RadarBox.freqSignals.getRxN();
        int txN = RadarBox.freqSignals.getTxN();
        int chN = rxN*txN;

        for(int rx = 0; rx<rxN; rx++) {
            for(int tx=0; tx<txN; tx++) {
                int line = rx*txN+tx;
                    graphView.addLine(new Line2D(tempY, tempY,
                            GraphColor.values()[line % GraphColor.values().length].argb,
                            "r" + rx + "t" + tx + "re"));
                    graphView.addLine(new Line2D(tempY, tempY,
                            GraphColor.values()[(line + 1) % GraphColor.values().length].argb,
                            "r" + rx + "t" + tx + "im"));
                    graphView.addLine(new Line2D(tempY, tempY,
                            GraphColor.values()[(line + 2) % GraphColor.values().length].argb,
                            "r" + rx + "t" + tx + "abs"));
            }
        }
    }

    private void resetTimeLines() {
        float[] tempY = new float[1];
        if(RadarBox.processing.getProcessingSequence().stream()
                .noneMatch(operationDSP -> operationDSP.getName().equals("FFT")))
            return;
        OperationDSP operationFFT =
                RadarBox.processing.getProcessingSequence().stream()
                        .filter(operationDSP -> operationDSP.getName().equals("FFT")).findFirst().get();
        int signalsCount = operationFFT.getOutputSignals().size();
        if(signalsCount==0)
            return;

        graphView.getLines().clear();

        for(int line=0; line<signalsCount; line++) {
            graphView.addLine(new Line2D(tempY, tempY,
                    GraphColor.values()[line % GraphColor.values().length].argb,
                    operationFFT.getOutputSignals().get(line).getName() + "re"));
            graphView.addLine(new Line2D(tempY, tempY,
                    GraphColor.values()[line % GraphColor.values().length].argb,
                    operationFFT.getOutputSignals().get(line).getName() + "im"));
            graphView.addLine(new Line2D(tempY, tempY,
                    GraphColor.values()[line % GraphColor.values().length].argb,
                    operationFFT.getOutputSignals().get(line).getName() + "abs"));
        }
    }

    private void resetSNRLines() {
        float[] tempY = new float[1];
        int rxN = RadarBox.freqSignals.getRxN();
        int txN = RadarBox.freqSignals.getTxN();
        int chN = rxN*txN;

        for(int rx = 0; rx<rxN; rx++) {
            for(int tx=0; tx<txN; tx++) {
                int line = rx*txN+tx;
                graphView.addLine(new Line2D(tempY, tempY,
                        GraphColor.values()[(line + 2) % GraphColor.values().length].argb,
                        "r" + rx + "t" + tx + "snr"));
            }
        }
    }

    private void resetSNR(){
        int rxN = RadarBox.freqSignals.getRxN();
        int txN = RadarBox.freqSignals.getTxN();
        int chN = rxN*txN;

        listSnr.clear();
        for(int rx = 0; rx<rxN; rx++) {
            for(int tx=0; tx<txN; tx++) {
                int line = rx*txN+tx;
                listSnr.add(new SNR(RadarBox.freqSignals.getFrequenciesMHz().length));
            }
        }
    }

    private void update(long frameNumber) {
        flag = Integer.parseInt(pref.getString("select_signal","0"));

        if (flag == SELECT_RAW) {
            updateFreq(frameNumber);
        } else if (flag == SELECT_SNR) {
            updateSNR(frameNumber); // TODO исправить ошибку в считывании SNR
        } else if(flag == SELECT_FFT) {
            updateFFT(frameNumber);
        }
        graphView.invalidate();
    }

    private void updateFreq(long frameNumber) {
        int rxN = RadarBox.freqSignals.getRxN();
        int txN = RadarBox.freqSignals.getTxN();
        int chN = rxN*txN;
        if(graphView.getLines().size()!=chN*3)
            resetAllLines();

        float[] tempVector = new float[RadarBox.freqSignals.getFrequenciesMHz().length];
        for(int i = 0; i<tempVector.length;i++)
            tempVector[i]=RadarBox.freqSignals.getFrequenciesMHz()[i];
        // координата X
        for(int rx = 0; rx<rxN; rx++) {
            for (int tx = 0; tx < txN; tx++) {
                    graphView.getLine("r" + rx + "t" + tx + "re").setX(tempVector);
                    graphView.getLine("r" + rx + "t" + tx + "im").setX(tempVector);
                    graphView.getLine("r" + rx + "t" + tx + "abs").setX(tempVector);
            }
        }
        // координата Y
        short[] rawFreqSignal = new short[RadarBox.freqSignals.getFN()*2];
        for(int rx = 0; rx<rxN; rx++) {
            for (int tx = 0; tx<txN; tx++) {
                if(RadarBox.freqSignals.getRawFreqOneChannelSignal(rx,tx,rawFreqSignal)>=0) {
                        for (int i = 0; i < tempVector.length; i++)
                            tempVector[i] = rawFreqSignal[2 * i];
                        graphView.getLine("r" + rx + "t" + tx + "re").setY(tempVector);
                        for (int i = 0; i < tempVector.length; i++)
                            tempVector[i] = rawFreqSignal[2 * i + 1];
                        graphView.getLine("r" + rx + "t" + tx + "im").setY(tempVector);
                        for (int i = 0; i < tempVector.length; i++)
                            tempVector[i] = (float)Math.sqrt(rawFreqSignal[2 * i] * rawFreqSignal[2 * i]
                                    + rawFreqSignal[2 * i + 1] * rawFreqSignal[2 * i + 1]);
                        graphView.getLine("r" + rx + "t" + tx + "abs").setY(tempVector);
                    }
                }
            }
        }


    private void updateSNR(long frameNumber) {
        int rxN = RadarBox.freqSignals.getRxN();
        int txN = RadarBox.freqSignals.getTxN();
        int chN = rxN*txN;
        if(graphView.getLines().size()!=chN)
            resetAllLines();

        float[] tempVector = new float[RadarBox.freqSignals.getFrequenciesMHz().length];
        for(int i = 0; i<tempVector.length;i++)
            tempVector[i]=RadarBox.freqSignals.getFrequenciesMHz()[i];
        // координата X
        for(int rx = 0; rx<rxN; rx++) {
            for (int tx = 0; tx < txN; tx++) {
                    graphView.getLine("r" + rx + "t" + tx + "snr").setX(tempVector);

            }
        }
        // координата Y
        short[] rawFreqSignal = new short[RadarBox.freqSignals.getFN()*2];
        for(int rx = 0; rx<rxN; rx++) {
            for (int tx = 0; tx<txN; tx++) {
                int line = rx*txN+tx;
                if(RadarBox.freqSignals.getRawFreqOneChannelSignal(rx,tx,rawFreqSignal)>=0) {
                        for (int i = 0; i < tempVector.length; i++)
                            tempVector[i] = (float)Math.sqrt(rawFreqSignal[2 * i] * rawFreqSignal[2 * i]
                                    + rawFreqSignal[2 * i + 1] * rawFreqSignal[2 * i + 1]);
                        listSnr.get(line).calculateSNR(tempVector);
                        graphView.getLine("r" + rx + "t" + tx + "snr").setY(listSnr.get(line).getArrayAvgSNR());
                }
            }
        }
    }

    private void updateFFT(long frameNumber) {

        if(RadarBox.processing.getProcessingSequence().stream()
                .noneMatch(operationDSP -> operationDSP.getName().equals("FFT")))
            return;
        OperationDSP operationFFT =
                RadarBox.processing.getProcessingSequence().stream()
                .filter(operationDSP -> operationDSP.getName().equals("FFT")).findFirst().get();
        int signalsCount = operationFFT.getOutputSignals().size();
        if(signalsCount==0)
            return;

        if(graphView.getLines().size()!=signalsCount)
            resetAllLines();

        float[] tempVector = new float[operationFFT.getOutputSignals().get(0).getLength()];
        for(int i = 0; i<tempVector.length;i++)
            tempVector[i]=operationFFT.getOutputSignals().get(0).getX()[i];

        for(int rxtx = 0; rxtx < signalsCount; rxtx++) {
            // координаты X и Y действительная часть
            graphView.getLine(operationFFT.getOutputSignals().get(rxtx).getName()+"re")
                    .setX(operationFFT.getOutputSignals().get(rxtx).getX());
            for(int i =0; i<tempVector.length; i++)
                tempVector[i]=operationFFT.getOutputSignals().get(rxtx).getY()[i].re;
            graphView.getLine(operationFFT.getOutputSignals().get(rxtx).getName()+"re")
                    .setY(tempVector);
            // координаты X и Y мнимая часть
            graphView.getLine(operationFFT.getOutputSignals().get(rxtx).getName()+"im")
                    .setX(operationFFT.getOutputSignals().get(rxtx).getX());
            for(int i =0; i<tempVector.length; i++)
                tempVector[i]=operationFFT.getOutputSignals().get(rxtx).getY()[i].im;
            graphView.getLine(operationFFT.getOutputSignals().get(rxtx).getName()+"im")
                    .setY(tempVector);
        }
    }

    @Override
    public void onPause() {
        pref.edit().putFloat("GraphView"+"xMax",(float) graphView.getxMax()).apply();
        pref.edit().putFloat("GraphView"+"xMin",(float) graphView.getxMin()).apply();
        pref.edit().putFloat("GraphView"+"yMax",(float) graphView.getyMax()).apply();
        pref.edit().putFloat("GraphView"+"yMin",(float) graphView.getyMin()).apply();
        if(graphSettingsFragment.isAdded())
            closeGraphSettings();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        resetAllLines();
    }
}
