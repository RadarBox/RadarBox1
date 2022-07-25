package org.rdr.radarbox.DSP;

import androidx.annotation.NonNull;

/** Класс для работы с сигналами. <p>
 * Сигнал представялется, как функция y(x). <p>
 *  y - всегда комплексные ({@link Complex} отсчёты типа float <p>
 *  x - всегда действительный аргумент типа float <p>
 * Для удобства использования в графиках и других средствах отображения, для переменных x и y
 * указываются единицы измерений unitsX, unitsY. <p>
 */
public class ComplexSignal {

    /** Порядок следования отсчётов сигнала при передачи массива сигнала в конструктор класса.
     * По названиям понятно, какой порядок, что означает.
     */
    enum SamplesOrder {
        ONLY_RE,
        ONLY_IM,
        RE_IM_RE_IM,
        IM_RE_IM_RE,
    };

    private float[] x;
    private Complex[] y;
    private String unitsX="";
    private String unitsY="";
    private String name="";

    public float[] getX() { return x; }
    public Complex[] getY() { return y; }
    public String getUnitsX() { return unitsX; }
    public String getUnitsY() { return unitsY; }
    public void setUnitsX(String unitsX) { this.unitsX = unitsX; }
    public void setUnitsY(String unitsY) { this.unitsY = unitsY; }
    public String getName() {return name;}

    public ComplexSignal(float[] x, Complex[] y) {
        this.x = x; this.y=y;
    }

    public ComplexSignal(float[] x, Complex[] y, String name) {
        this(x,y); this.name = name;
    }

    public ComplexSignal(float[] x, String unitsX, Complex[] y) {
        this(x,y);
        this.unitsX = unitsX;
    }

    public ComplexSignal(float[] x, String unitsX, Complex[] y, String unitsY) {
        this(x,unitsX,y); this.unitsY=unitsY;
    }

    public ComplexSignal(float[] x, String unitsX, Complex[] y, String unitsY, String name) {
        this(x,unitsX,y,unitsY); this.name=name;
    }

    /** Упрощённый конструктор сигнала. В x в таком случае записываются номера отсчётов (индексы).
     * В зависимости от порядка следования отсчётов <em>y</em>, определяемом переменной samplesOrder,
     * данные из аргумента <em>y</em> будут по-разному записываться в this.y.
     *
     * @param y - массив отсчётов сигнала <em>y(x)</em>
     * @param samplesOrder - порядок следования отсчётов
     */
    public ComplexSignal(float[] y, @NonNull SamplesOrder samplesOrder) {
        switch (samplesOrder) {
            case ONLY_RE:
                this.x = new float[y.length];
                this.y = new Complex[y.length];
                for (int i = 0; i < x.length; i++) {
                    this.x[i] = i;
                    this.y[i].re=y[i];
                }
                break;
            case ONLY_IM:
                this.x = new float[y.length];
                this.y = new Complex[y.length];
                for (int i = 0; i < x.length; i++) {
                    this.x[i] = i;
                    this.y[i].im=y[i];
                }
                break;
            case RE_IM_RE_IM:
                this.x = new float[y.length/2];
                this.y = new Complex[y.length/2];
                for (int i = 0; i < x.length; i++) {
                    this.x[i] = i;
                    this.y[i].re=y[2*i];
                    this.y[i].im=y[2*i+1];
                }
                break;
            case IM_RE_IM_RE:
                this.x = new float[y.length/2];
                this.y = new Complex[y.length/2];
                for (int i = 0; i < x.length; i++) {
                    this.x[i] = i;
                    this.y[i].re=y[2*i+1];
                    this.y[i].im=y[2*i];
                }
                break;
        }
    }

    /** Упрощённый конструктор сигнала. В x в таком случае записываются номера отсчётов (индексы).
     * В зависимости от порядка следования отсчётов <em>y</em>, определяемом переменной samplesOrder,
     * данные из аргумента <em>y</em> будут по-разному записываться в this.y.
     *
     * @param y - массив отсчётов сигнала <em>y(x)</em>
     * @param samplesOrder - порядок следования отсчётов
     */
    public ComplexSignal(short[] y, @NonNull SamplesOrder samplesOrder) {
        switch (samplesOrder) {
            case ONLY_RE:
                this.x = new float[y.length];
                this.y = new Complex[y.length];
                for (int i = 0; i < x.length; i++) {
                    this.x[i] = i;
                    this.y[i].re=y[i];
                }
                break;
            case ONLY_IM:
                this.x = new float[y.length];
                this.y = new Complex[y.length];
                for (int i = 0; i < x.length; i++) {
                    this.x[i] = i;
                    this.y[i].im=y[i];
                }
                break;
            case RE_IM_RE_IM:
                this.x = new float[y.length/2];
                this.y = new Complex[y.length/2];
                for (int i = 0; i < x.length; i++) {
                    this.x[i] = i;
                    this.y[i].re=y[2*i];
                    this.y[i].im=y[2*i+1];
                }
                break;
            case IM_RE_IM_RE:
                this.x = new float[y.length/2];
                this.y = new Complex[y.length/2];
                for (int i = 0; i < x.length; i++) {
                    this.x[i] = i;
                    this.y[i].re=y[2*i+1];
                    this.y[i].im=y[2*i];
                }
                break;
        }
    }

    /** Упрощённый конструктор сигнала. В x в таком случае записываются номера отсчётов (индексы).
     * В зависимости от порядка следования отсчётов <em>y</em>, определяемом переменной samplesOrder,
     * данные из аргумента <em>y</em> будут по-разному записываться в this.y.
     *
     * @param y - массив отсчётов сигнала <em>y(x)</em>
     * @param samplesOrder - порядок следования отсчётов
     */
    public ComplexSignal(int[] y, @NonNull SamplesOrder samplesOrder) {
        switch (samplesOrder) {
            case ONLY_RE:
                this.x = new float[y.length];
                this.y = new Complex[y.length];
                for (int i = 0; i < x.length; i++) {
                    this.x[i] = i;
                    this.y[i].re=y[i];
                }
                break;
            case ONLY_IM:
                this.x = new float[y.length];
                this.y = new Complex[y.length];
                for (int i = 0; i < x.length; i++) {
                    this.x[i] = i;
                    this.y[i].im=y[i];
                }
                break;
            case RE_IM_RE_IM:
                this.x = new float[y.length/2];
                this.y = new Complex[y.length/2];
                for (int i = 0; i < x.length; i++) {
                    this.x[i] = i;
                    this.y[i].re=y[2*i];
                    this.y[i].im=y[2*i+1];
                }
                break;
            case IM_RE_IM_RE:
                this.x = new float[y.length/2];
                this.y = new Complex[y.length/2];
                for (int i = 0; i < x.length; i++) {
                    this.x[i] = i;
                    this.y[i].re=y[2*i+1];
                    this.y[i].im=y[2*i];
                }
                break;
        }
    }

    /** Возвращает массив модулей комплексного сигнала
     * <p>
     *     Пример применения:
     *     short[] y = new short[100];
     *     ComplexSignal sig1 = new ComplexSignal(y, SamplesOrder.ONLY_RE);
     *     float[] absSig1 = sig1.abs();
     * </p>
     * @return указатель на массив модулей комплексного сигнала
     */
    public float[] getAbs() {
        float[] absY = new float[this.y.length];
        for (int i=0; i<y.length; i++)
            absY[i] = y[i].abs();
        return absY;
    }

    /** Возвращает массив аргументов комплексного сигнала
     * <p>
     *     Пример применения:
     *     short[] y = new short[100];
     *     ComplexSignal sig1 = new ComplexSignal(y, SamplesOrder.ONLY_RE);
     *     float[] argSig1 = sig1.abs();
     * </p>
     * @return указатель на массив модулей комплексного сигнала
     */
    public float[] getArg() {
        float[] argY = new float[this.y.length];
        for (int i=0; i<y.length; i++)
            argY[i] = y[i].arg();
        return argY;
    }
}
