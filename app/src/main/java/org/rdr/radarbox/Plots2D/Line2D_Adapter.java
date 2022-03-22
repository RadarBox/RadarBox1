package org.rdr.radarbox.Plots2D;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.rdr.radarbox.R;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class Line2D_Adapter extends RecyclerView.Adapter<Line2D_Adapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final ArrayList<Line2D> lines2D;

    Line2D_Adapter(Context context, final ArrayList<Line2D> lines2D) {
        this.lines2D=lines2D;
        this.inflater = LayoutInflater.from(context);
    }
    @NonNull
    @Override
    public Line2D_Adapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.line2d_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Line2D_Adapter.ViewHolder holder, int position) {
        Line2D line2D = lines2D.get(position);
        holder.lineTypeView.setBackgroundColor(line2D.getColor());
        holder.legendNameView.setText(line2D.getLegendName());
        holder.needShowView.setChecked(line2D.isNeedShow());
        holder.needShowView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                line2D.setNeedShow(isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return lines2D.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView lineTypeView;
        final TextView legendNameView;
        final CheckBox needShowView;
        ViewHolder(View view){
            super(view);
            lineTypeView = view.findViewById(R.id.line_type);
            legendNameView = view.findViewById(R.id.legend_name);
            needShowView = view.findViewById(R.id.need_show);
        }
    }
}
