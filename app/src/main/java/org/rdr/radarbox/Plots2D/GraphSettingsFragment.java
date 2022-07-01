package org.rdr.radarbox.Plots2D;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;

import org.rdr.radarbox.R;

import java.io.Serializable;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;


public class GraphSettingsFragment extends Fragment {
    ArrayList<Line2D> allLines2D;
    final int someLinesMaxSize = 3;
    ArrayList<Line2D> someLines2D =
            new ArrayList<>(someLinesMaxSize);
    GraphView graphView;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        if(args!=null) {
            Serializable graphArg = args.getSerializable("GraphView");
            if(graphArg!=null) {
                graphView = (GraphView) graphArg;
                allLines2D = graphView.getLines();
            }
        }
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.graph_settings_fragment,container,false);
        final TypedValue value = new TypedValue();
        this.getContext().getTheme().resolveAttribute(R.attr.colorOnPrimary,value,true);
        view.setBackgroundColor(value.data);
        EditText scaleFactor = view.findViewById(R.id.scale_factor);
        final String[] oldTextScaleFactor = new String[1];
        oldTextScaleFactor [0] = scaleFactor.getText().toString();
        scaleFactor.setOnKeyListener((v, keyCode, event) -> {
                    if(event.getAction() == KeyEvent.ACTION_DOWN &&
                            (keyCode == KeyEvent.KEYCODE_ENTER) ) {
                        if (scaleFactor.getText().toString().isEmpty()){
                            scaleFactor.setText(oldTextScaleFactor[0]);
                        }
                        graphView.scaleYlimitMul(Double.parseDouble(scaleFactor.getText().toString()));
                        graphView.scaleYlimitDev(Double.parseDouble(scaleFactor.getText().toString()));
                        graphView.invalidate();
                        return true;
                    }
                    return false;
                }
        );


        EditText axisXmin = view.findViewById(R.id.axis_x_min);
        axisXmin.setText(Double.toString(graphView.getxMin()));
        final String[] oldTextXmin = new String[1];
        oldTextXmin [0] = axisXmin.getText().toString();
        axisXmin.setOnKeyListener((v, keyCode, event) -> {
                    if(event.getAction() == KeyEvent.ACTION_DOWN &&
                            (keyCode == KeyEvent.KEYCODE_ENTER) ) {
                        if (axisXmin.getText().toString().isEmpty()){
                            axisXmin.setText(oldTextXmin[0]);
                        }
                        graphView.setxMin(Double.parseDouble(axisXmin.getText().toString()));
                        graphView.invalidate();
                        return true;
                    }
                    return false;
                }
        );

        EditText axisXmax = view.findViewById(R.id.axis_x_max);
        axisXmax.setText(Double.toString(graphView.getxMax()));
        final String[] oldTextXmax = new String[1];
        oldTextXmax [0] = axisXmax.getText().toString();
        axisXmax.setOnKeyListener((v, keyCode, event) -> {
                    if(event.getAction() == KeyEvent.ACTION_DOWN &&
                            (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        if (axisXmax.getText().toString().isEmpty()) {
                            axisXmax.setText(oldTextXmax[0]);
                        }
                        graphView.setxMax(Double.parseDouble(axisXmax.getText().toString()));
                        graphView.invalidate();
                        return true;
                    }
                    return false;
                }
        );
        EditText axisYmax = view.findViewById(R.id.axis_y_max);
        axisYmax.setText(Double.toString(graphView.getyMax()));
        final String[] oldTextYmax = new String[1];
        oldTextYmax [0] = axisYmax.getText().toString();
        axisYmax.setOnKeyListener((v, keyCode, event) -> {
                    if(event.getAction() == KeyEvent.ACTION_DOWN &&
                            (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        if (axisYmax.getText().toString().isEmpty()) {
                            axisYmax.setText(oldTextYmax[0]);
                        }
                        graphView.setyMax(Double.parseDouble(axisYmax.getText().toString()));
                        if(graphView.getyMin()!=0f)
                            graphView.setyMin(-1*Double.parseDouble(axisYmax.getText().toString()));
                        graphView.invalidate();
                        return true;
                    }
                    return false;
                }
        );
        SwitchCompat axisYabs = view.findViewById(R.id.axis_y_abs_switch);
        if(graphView.getyMin()==0f) axisYabs.setChecked(true);
        axisYabs.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                    graphView.setyMin(0);
                else
                    graphView.setyMin(-1*graphView.getyMax());
                graphView.invalidate();
            }
        });
        Button buttonPlus = view.findViewById(R.id.button_plus);
        buttonPlus.setOnClickListener(v -> {
            graphView.scaleYlimitDev(Double.parseDouble(scaleFactor.getText().toString()));
            axisYmax.setText(Double.toString(graphView.getyMax()));
            graphView.invalidate();
        });

        Button buttonMinus = view.findViewById(R.id.button_minus);
        buttonMinus.setOnClickListener(v -> {
            graphView.scaleYlimitMul(Double.parseDouble(scaleFactor.getText().toString()));
            axisYmax.setText(Double.toString(graphView.getyMax()));
            graphView.invalidate();
        });

        if(allLines2D !=null) {
            // настройка отображения нескольких линий
            int i=0;
            someLines2D.clear();
            for(int j=0; j<allLines2D.size() && i<someLinesMaxSize; j++) {
                if(allLines2D.get(j).isNeedShow()) {
                    someLines2D.add(i, allLines2D.get(j));
                    i++;
                }
            }
            RecyclerView rvSingle = view.findViewById(R.id.line2D_single);
            Line2D_Adapter adapter = new Line2D_Adapter(view.getContext(), someLines2D);
            rvSingle.setAdapter(adapter);
        }

        AppCompatImageButton buttonExpandLinesList = view.findViewById(R.id.line2D_expand_button);
        buttonExpandLinesList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(allLines2D !=null) {
                    RecyclerView recyclerView = new RecyclerView(view.getContext());
                    RecyclerView.LayoutParams params = new
                            RecyclerView.LayoutParams(
                            RecyclerView.LayoutParams.WRAP_CONTENT,
                            RecyclerView.LayoutParams.WRAP_CONTENT
                    );
                    recyclerView.setLayoutParams(params);
                    GridLayoutManager llm = new GridLayoutManager(view.getContext(),3);
                    Line2D_Adapter adapter2 = new Line2D_Adapter(view.getContext(), allLines2D);
                    recyclerView.setAdapter(adapter2);
                    recyclerView.setLayoutManager(llm);
                    recyclerView.setVisibility(View.VISIBLE);
                    AlertDialog linesDialog = new AlertDialog.Builder(view.getContext())
                            .setView(recyclerView)
                            .setPositiveButton("Close", (dialog, which) -> dialog.cancel())
                            .create();
                    linesDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            int i=0;
                            someLines2D.clear();
                            for(int j=0; j<allLines2D.size() && i<someLinesMaxSize; j++) {
                                if(allLines2D.get(j).isNeedShow()) {
                                    someLines2D.add(i, allLines2D.get(j));
                                    i++;
                                }
                            }
                            RecyclerView rvSingle = view.findViewById(R.id.line2D_single);
                            Line2D_Adapter adapter =
                                    new Line2D_Adapter(view.getContext(), someLines2D);
                            rvSingle.setAdapter(adapter);
                            graphView.invalidate();
                        }
                    });
                    Window window = linesDialog.getWindow();
                    WindowManager.LayoutParams wlp = window.getAttributes();
                    window.setDimAmount(0);
                    wlp.gravity = Gravity.BOTTOM;
                    window.setAttributes(wlp);
                    linesDialog.show();
                }
            }
        });
        return view;
    }
}