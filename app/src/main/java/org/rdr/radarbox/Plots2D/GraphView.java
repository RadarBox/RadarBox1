package org.rdr.radarbox.Plots2D;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import org.rdr.radarbox.R;
import org.rdr.radarbox.RadarBox;

import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;


/**
 * Класс для отображения графиков
 * @author Сапронов Данил Игоревич
 * @version 0.1
 */
public class GraphView extends View implements Serializable {

    private double xMax, xMin;
    private double yMax, yMin;
    private boolean mShowLabelsX, mShowLabelsY, mShowTitleX, mShowTitleY;
    private String mTitleX, mTitleY;
    private List<String> mLabelsX, mLabelsY;


    public void addLine(Line2D line2D) {
        lines.add(line2D);
    }

    private final ArrayList<Line2D> lines = new ArrayList<Line2D>();
    public final ArrayList<Line2D> getLines() {return lines;}

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GraphView);

        try {
            mShowLabelsX = a.getBoolean(R.styleable.GraphView_showLabelsX, false);
            mShowLabelsY = a.getBoolean(R.styleable.GraphView_showLabelsY, false);
            mShowTitleX = a.getBoolean(R.styleable.GraphView_showTitleX, false);
            mShowTitleY = a.getBoolean(R.styleable.GraphView_showTitleY, false);

            mTitleX = a.getString(R.styleable.GraphView_titleX);
            mTitleY = a.getString(R.styleable.GraphView_titleY);
        } finally {
            a.recycle();
        }
    }

    // сеттеры и геттеры для полей класса
    public void setxMax(double xMax) {
        this.xMax = xMax;
    }
    public void setxMin(double xMin) {
        this.xMin = xMin;
    }
    public void setyMax(double yMax) {
        this.yMax = yMax;
    }
    public void setyMin(double yMin) {
        this.yMin = yMin;
    }
    public double getxMax() {return this.xMax;}
    public double getxMin() {return this.xMin;}
    public double getyMax() {return this.yMax;}
    public double getyMin() {return this.yMin;}
    public void setShowLabelsX(boolean value) {mShowLabelsX=value;}
    public void setShowLabelsY(boolean value) {mShowLabelsY=value;}
    public void setShowTitleX(boolean value) {mShowTitleX=value;}
    public void setShowTitleY(boolean value) {mShowTitleY=value;}
    public boolean getShowLabelsX() {return mShowLabelsX;}
    public boolean getShowLabelsY() {return mShowLabelsY;}
    public boolean getShowTitleX() {return mShowTitleX;}
    public boolean getShowTitleY() {return mShowTitleY;}

    /* Функции, которые необходимо реализовать */

    /** Изменяеет текущий масштаб графика в scale раз по оси Y:
     * изменяются подписи к линиям сетки на оси Y, перерисовывается виджет.
     * Так как график может иметь и отрицательные значения, изменяются, как
     * верхняя, так и нижняя границы.
     * @param scaleMul - коэффициент изменения масштаба. При scale>1 границы (пределы) графика
     *              увеличиваются, следовательно линии на графике приобретают меньший размах.
     *              При scale<1 обратная ситуация.
     */

    public void scaleYlimitMul(double scaleMul) {
        yMin=scaleMul*yMin;
        yMax=scaleMul*yMax;
    }
    public void  scaleYlimitDev(double scaleDev) {
        yMin=yMin/scaleDev;
        yMax=yMax/scaleDev;
    }
    public Line2D getLine(String legendName) throws NoSuchElementException {
        return lines.stream().filter(
                line2D -> line2D.getLegendName().equals(legendName)).findFirst().get();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Создание сетки
        drawGrid(canvas);
        //Рисование сигнала
        drawLines2D(canvas);
        //Создание значний оси
        axisCaptions(canvas);
    }

    private void drawLines2D(Canvas canvas) {
        Path path = new Path();
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);

        // Определение границ холста
        int height = canvas.getHeight();
        int width = canvas.getWidth();

        // Определение параметров кисти (цвет, ширина и т.д.)
        for (Line2D line2D:lines) {
            if(!line2D.isNeedShow())
                continue;
            paint.setColor(line2D.getColor());
            path.moveTo(XtoW(line2D.getX()[0],width),YtoH(line2D.getY()[0],height));
            for(int i=1; i<line2D.getX().length; i++) {
                path.lineTo(XtoW(line2D.getX()[i],width),YtoH(line2D.getY()[i],height));
            }
            canvas.drawPath(path, paint);
            path.reset();
        }
    }
    private int XtoW(double x, int w) {
        return (int) ((x-xMin)*w/(xMax-xMin));
    }
    private int YtoH(double y, int h) {
        return (int) ((yMax-y)/(yMax-yMin)*h);
    }

    private void drawGrid(Canvas canvas) {
        // Определение параметров кисти (цвет, ширина и т.д.)
        Paint paint = new Paint();
        final TypedValue value = new TypedValue();
        this.getContext().getTheme().resolveAttribute(R.attr.colorOnSecondary,value,true);
        paint.setColor(value.data);
        paint.setStrokeWidth(3);
        paint.setStyle(Paint.Style.STROKE);
        paint.setPathEffect(new DashPathEffect(new float[]{30,30},0));

        // Определение границ холста
        int height = canvas.getHeight();
        int width = canvas.getWidth();

        // Создание инструмента для рисования линий
        Path path = new Path();

        // Очерчивание границ
        path.moveTo(0,0);
        path.lineTo(width,0);
        path.lineTo(width,height);
        path.lineTo(0,height);
        path.lineTo(0,0);

        // Рисование сетки
        for (int i =1; i<10; i++) { // горизонтальные линии
            path.moveTo(0, i*height/10);
            path.lineTo(width, i*height/10);
        }
        for (int i =1; i<10; i++) { // вертикальные линии
            path.moveTo(i*width/10, 0);
            path.lineTo(i*width/10, height);
        }



        canvas.drawPath(path, paint);
    }
    public void axisCaptions(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.GRAY);
        paint.setStrokeWidth(3);

        // Определение границ холста
        int height = canvas.getHeight();
        int width = canvas.getWidth();
        //отрисовка вертикальных значений

        paint.setTextSize(40);

        //максимум
        canvas.drawText(String.valueOf(yMax), 0, 30,paint);
        //минимум
//        canvas.drawText(String.valueOf(yMin), 0, height,paint);

        for (int i =1; i<10; i++) {
            canvas.drawText(String.valueOf(yMin+(yMax-yMin)*i/10), 0, height-i*height/10,paint);
        }

        //отрисовка горизонтальных значений
    /*for (Map.Entry<String,Line2D> line:lines.entrySet()) {
        paint.setStrokeWidth(30);
        Line2D temp = line.getValue();
        double X[] = temp.getX();

         double maximumX,minimumX =  0;
         maximumX = (int)X[0];
         minimumX = (int)X[0];

        for (int i = 1; i < X.length; i++)
        {
            if (X[i] > maximumX)
            {
                maximumX = (int)X[i];
            }
            if (X[i] < minimumX)
            {
                minimumX = (int)X[i];
            }
        }*/

        //canvas.drawText(String.valueOf(temp1), 0, height,paint);
        paint.setTextSize(40);
        //максимум
        canvas.drawText(String.valueOf(2700), width, height,paint);
        //минимум
        canvas.save();
        canvas.rotate(-90f, 40, height);
        canvas.drawText(String.valueOf(xMin), 40, height,paint);
        canvas.restore();
        //canvas.translate(200,height);
        //canvas.drawText(String.valueOf(xMin+xMin*1/5), width/5, 2*height,paint);

        for (int i =1; i<6; i++) {
            canvas.save();
            canvas.rotate(-90f, i*width/5, height);
            canvas.drawText(String.valueOf(xMin+(xMax-xMin)*i/5), i*width/5, height,paint);
            canvas.restore();
        }
    }

}