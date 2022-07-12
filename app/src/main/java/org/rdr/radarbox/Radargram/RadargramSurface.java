package org.rdr.radarbox.Radargram;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class RadargramSurface extends SurfaceView {
    private double xMax, xMin;
    private double yMax, yMin;
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

    Path path;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Thread thread = null;
    SurfaceHolder surfaceHolder;
    volatile boolean running = false;
    public RadargramSurface(Context context) {
        super(context);
        surfaceHolder = getHolder();
    }

    public void updateFrame() {

    }

    private int XtoW(double x, int w) {
        return (int) ((x-xMin)*w/(xMax-xMin));
    }
    private int YtoH(double y, int h) {
        return (int) ((yMax-y)/(yMax-yMin)*h);
    }
}
