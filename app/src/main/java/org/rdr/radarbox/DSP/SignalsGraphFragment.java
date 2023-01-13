package org.rdr.radarbox.DSP;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.rdr.radarbox.DSP.Operations.OperationDSP;
import org.rdr.radarbox.Plots2D.GraphColor;
import org.rdr.radarbox.Plots2D.GraphSettingsFragment;
import org.rdr.radarbox.Plots2D.GraphView;
import org.rdr.radarbox.Plots2D.Line2D;
import org.rdr.radarbox.R;
import org.rdr.radarbox.RadarBox;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SignalsGraphFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SignalsGraphFragment extends Fragment {

    private static final String ARG_OPERATION_DSP = "operationDSP";
    private static GraphView graphView;
    private static OperationDSP operationDSP;
    GraphSettingsFragment graphSettingsFragment = null;
    private SharedPreferences pref;

    public SignalsGraphFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param operationDSP1 операция DSP, из которой будут извлекаться сигналы для отображения
     * @return A new instance of fragment SignalsGraphFragment.
     */
    public static SignalsGraphFragment newInstance(OperationDSP operationDSP1) {
        SignalsGraphFragment fragment = new SignalsGraphFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_OPERATION_DSP,operationDSP);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            operationDSP = (OperationDSP) getArguments().getSerializable(ARG_OPERATION_DSP);
        }
        RadarBox.dataThreadService.getLiveFrameCounter().observe(getViewLifecycleOwner(),
                this::updateLines);

        // создание фрагмента с настройками графика, который будет выдвигаться с помощью свайпа
        graphSettingsFragment = new GraphSettingsFragment();
        // Передаём весь объект с графиками в бандл, чтобы его мог открыть фрагмент с настройками
        Bundle args = new Bundle();
        args.putSerializable("GraphView", graphView);
        graphSettingsFragment.setArguments(args);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_signal_graph, container, false);
        pref = PreferenceManager.getDefaultSharedPreferences(this.requireContext());
        graphView = view.findViewById(R.id.graph);
        graphView.setxMin(pref.getFloat(operationDSP.getName()+"GraphView"+"xMin",1000));
        graphView.setxMax(pref.getFloat(operationDSP.getName()+"GraphView"+"xMax",3000));
        graphView.setyMax(pref.getFloat(operationDSP.getName()+"GraphView"+"yMax",3000));
        graphView.setyMin(pref.getFloat(operationDSP.getName()+"GraphView"+"yMin",-3000));

        setSwipeGestureDetector(view);

        return view;
    }

    private void setSwipeGestureDetector(View view) {
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

    @Override
    public void onPause() {
        pref.edit()
                .putFloat(operationDSP.getName()+"GraphView"+"xMax",(float)graphView.getxMax())
                .putFloat(operationDSP.getName()+"GraphView"+"xMin",(float)graphView.getxMin())
                .putFloat(operationDSP.getName()+"GraphView"+"yMax",(float)graphView.getyMax())
                .putFloat(operationDSP.getName()+"GraphView"+"yMin",(float)graphView.getyMin())
                .apply();
        if(graphSettingsFragment.isAdded())
            closeGraphSettings();
        super.onPause();
    }

    private void resetLines(){
        ArrayList<ComplexSignal> outputSignals = (ArrayList<ComplexSignal>) operationDSP.getOutputSignals();
        for (int line=0; line<outputSignals.size(); line++) {
            graphView.addLine(
                    new Line2D(
                            outputSignals.get(line).getX(),
                            outputSignals.get(line).getX(),
                            GraphColor.values()[line % GraphColor.values().length].argb,
                            outputSignals.get(line).getName()+"re"));
            graphView.addLine(
                    new Line2D(
                            outputSignals.get(line).getX(),
                            outputSignals.get(line).getX(),
                            GraphColor.values()[line % GraphColor.values().length].argb,
                            outputSignals.get(line).getName()+"im"));
            graphView.addLine(
                    new Line2D(
                            outputSignals.get(line).getX(),
                            outputSignals.get(line).getX(),
                            GraphColor.values()[line % GraphColor.values().length].argb,
                            outputSignals.get(line).getName()+"abs"));
        }
    }

    private void updateLines(long frameNumber) {
        ArrayList<ComplexSignal> outputSignals = operationDSP.getOutputSignals();
        if(graphView.getLines().size()!=outputSignals.size()*3)
            resetLines();

        // координата X
        for (int line=0; line<outputSignals.size(); line++) {
            String name =outputSignals.get(line).getName();
            graphView.getLine(name+"re").setX(outputSignals.get(line).getX());
            graphView.getLine(name+"im").setX(outputSignals.get(line).getX());
            graphView.getLine(name+"abs").setX(outputSignals.get(line).getX());
        }

        float[] tempSignalVector = new float[outputSignals.get(0).getLength()];
        // координата Y
        for (int line=0; line<outputSignals.size(); line++) {
            String name =outputSignals.get(line).getName();
            ComplexSignal complexSignal = outputSignals.get(line);
            // Re
            for (int i = 0; i < tempSignalVector.length; i++)
                tempSignalVector[i] = complexSignal.getY()[i].re;
            graphView.getLine(name+"re").setY(tempSignalVector);
            // Im
            for (int i = 0; i < tempSignalVector.length; i++)
                tempSignalVector[i] = complexSignal.getY()[i].im;
            graphView.getLine(name+"im").setY(tempSignalVector);
            // Abs
            for (int i = 0; i < tempSignalVector.length; i++)
                tempSignalVector[i] = complexSignal.getY()[i].abs();
            graphView.getLine(name+"abs").setY(tempSignalVector);
        }
    }
}