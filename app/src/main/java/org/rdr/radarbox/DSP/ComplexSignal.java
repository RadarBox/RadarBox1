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
    public int getLength() {return y.length;}

    /** Задать название единиц измерения по оси X <p>
     * (нужно для графиков)
     *
     * @param unitsX единицы измерения по оси X
     * @return этот же объект для реализации присваивания по цепочке
     */
    public ComplexSignal setUnitsX(String unitsX) { this.unitsX = unitsX; return this;}

    /** Задать название единиц измерения по оси Y <p>
     * (нужно для графиков)
     *
     * @param unitsY единицы измерения по оси Y
     * @return этот же объект для реализации присваивания по цепочке
     */
    public ComplexSignal setUnitsY(String unitsY) { this.unitsY = unitsY; return this;}

    /** Задать название сигнала <p>
     * Нужно для графиков. Особенно, когда сигналов несколько. Например, сигналы с разных
     * передатчиков / приёмников.
     *
     * @param name название сигнала
     * @return этот же объект для реализации присваивания по цепочке
     */
    public ComplexSignal setName(String name) { this.name = name; return this;}

    /** название сигнала <p>
     * Нужно для графиков. Особенно, когда сигналов несколько. Например, сигналы с разных
     * передатчиков / приёмников.
     * @return название сигнала
     */
    public String getName() {return name;}

    public ComplexSignal setX(short[] x) {
        if (x.length != this.y.length) {
            throw new IllegalArgumentException("x.length != y.length");
        }
        this.x = new float[x.length];
        for(int i=0; i<x.length; i++)
            this.x[i] = x[i];
        return this;
    }

    public ComplexSignal setX(int[] x) {
        if (x.length != this.y.length) {
            throw new IllegalArgumentException("x.length != y.length");
        }
        this.x = new float[x.length];
        for(int i=0; i<x.length; i++)
            this.x[i] = x[i];
        return this;
    }

    public ComplexSignal setX(float[] x) {
        if (x.length != this.y.length) {
            throw new IllegalArgumentException("x.length != y.length");
        }
        this.x = new float[x.length];
        for(int i=0; i<x.length; i++)
            this.x[i] = x[i];
        return this;
    }

    /** Простейший конструктор сигнала. По сути выделяет память для хранения.
     *
     * @param length - количество отсчётов сигнала
     */
    public ComplexSignal(int length) {
        this.x = new float[length];
        this.y = new Complex[length];
    }

    public ComplexSignal(float[] x, Complex[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("x.length != y.length");
        }
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
                    this.y[i]=new Complex(y[i],0);
                }
                break;
            case ONLY_IM:
                this.x = new float[y.length];
                this.y = new Complex[y.length];
                for (int i = 0; i < x.length; i++) {
                    this.x[i] = i;
                    this.y[i]=new Complex(0,y[i]);
                }
                break;
            case RE_IM_RE_IM:
                this.x = new float[y.length/2];
                this.y = new Complex[y.length/2];
                for (int i = 0; i < x.length; i++) {
                    this.x[i] = i;
                    this.y[i]=new Complex(y[2*i],y[2*i+1]);
                }
                break;
            case IM_RE_IM_RE:
                this.x = new float[y.length/2];
                this.y = new Complex[y.length/2];
                for (int i = 0; i < x.length; i++) {
                    this.x[i] = i;
                    this.y[i]=new Complex(y[2*i+1],y[2*i]);
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
                    this.y[i]=new Complex(y[i],0);
                }
                break;
            case ONLY_IM:
                this.x = new float[y.length];
                this.y = new Complex[y.length];
                for (int i = 0; i < x.length; i++) {
                    this.x[i] = i;
                    this.y[i]=new Complex(0,y[i]);
                }
                break;
            case RE_IM_RE_IM:
                this.x = new float[y.length/2];
                this.y = new Complex[y.length/2];
                for (int i = 0; i < x.length; i++) {
                    this.x[i] = i;
                    this.y[i]=new Complex(y[2*i],y[2*i+1]);
                }
                break;
            case IM_RE_IM_RE:
                this.x = new float[y.length/2];
                this.y = new Complex[y.length/2];
                for (int i = 0; i < x.length; i++) {
                    this.x[i] = i;
                    this.y[i]=new Complex(y[2*i+1],y[2*i]);
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
                    this.y[i]=new Complex(y[i],0);
                }
                break;
            case ONLY_IM:
                this.x = new float[y.length];
                this.y = new Complex[y.length];
                for (int i = 0; i < x.length; i++) {
                    this.x[i] = i;
                    this.y[i]=new Complex(0,y[i]);
                }
                break;
            case RE_IM_RE_IM:
                this.x = new float[y.length/2];
                this.y = new Complex[y.length/2];
                for (int i = 0; i < x.length; i++) {
                    this.x[i] = i;
                    this.y[i]=new Complex(y[2*i],y[2*i+1]);
                }
                break;
            case IM_RE_IM_RE:
                this.x = new float[y.length/2];
                this.y = new Complex[y.length/2];
                for (int i = 0; i < x.length; i++) {
                    this.x[i] = i;
                    this.y[i]=new Complex(y[2*i+1],y[2*i]);
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

    /** Произведение комплексных сигналов <p>
     * Результат возвращается в объект, вызывающий данный метод.
     * Предполагается, что длины векторов, содержащих отсчёты сигналов совпадают.
     * Т.е. a.y.length == b.y.length == this.y.length
     *
     * @param a один комплексный синал
     * @param b другой комплексный сигнал
     */
    public void mult(ComplexSignal a, ComplexSignal b) {
        if(a.y.length != b.y.length)
            throw new IllegalArgumentException("arg1.length != arg2.length");
        if(this.y.length != a.y.length)
            throw new IllegalArgumentException("this.length != arg.length");
        for(int i =0; i<this.y.length; i++) {
            this.y[i].times(a.y[i],b.y[i]);
        }
    }

    /** Деление комплексных сигналов <p>
     * Результат возвращается в объект, вызывающий данный метод.
     * Предполагается, что длины векторов, содержащих отсчёты сигналов совпадают.
     * Т.е. a.y.length == b.y.length == this.y.length
     *
     * @param a один комплексный синал (делимое)
     * @param b другой комплексный сигнал (делитель)
     */
    public void div(ComplexSignal a, ComplexSignal b) {
        if(a.y.length != b.y.length)
            throw new IllegalArgumentException("arg1.length != arg2.length");
        if(this.y.length != a.y.length)
            throw new IllegalArgumentException("this.length != arg.length");
        for(int i =0; i<this.y.length; i++) {
            this.y[i].div(a.y[i],b.y[i]);
        }
    }
}
