package org.rdr.radarbox.Plots2D;

import java.io.Serializable;
import java.util.Arrays;

public class Line2D implements Serializable {
    private double[] x;
    private double[] y;
    private int color;
    private String legendName;
    private boolean needShow;

    public Line2D(double[] x, double[] y, int color, String legendName) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.legendName = legendName;
        this.needShow = true;
    }

    public void setX(double[] x)           { this.x= Arrays.copyOf(x,x.length);}
    public void setY(double[] y)           {this.y= Arrays.copyOf(y,y.length);}
    void setColor(int c)          {this.color=c;}
    void setLegendName(String s)    {this.legendName=s;}
    void setNeedShow(boolean value) {this.needShow = value;}

    public double[] getX()         {return this.x;}
    double[] getY()         {return this.y;}
    int getColor()        {return this.color;}
    String getLegendName()  {return this.legendName;}
    boolean isNeedShow()    {return this.needShow;}
}
