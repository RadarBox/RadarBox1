package org.rdr.radarbox.DSP;

import androidx.annotation.NonNull;

/** Класс комплексных чисел. <p>
 * За основу взят тип данных float для экономии ресурсов.
 * (для радарных применений редко требуется бОльшая точность)
 * Пример использования:
 * Создание двух комплексных чисел и запись их перемножения с сохранением результата в 1-е число:
 *  Complex a(1,2); Complex b(3,4); a.times(b);
 * @author Danil Sapronov
 * @version 0.2
 */
public class Complex {

    public float re;
    public float im;

    /** Создание комплексного числа в формате re+i*im
     *
     * @param real - действительная составляющая
     * @param imag - мнимая составляющая
     */
    public Complex(float real, float imag) {
        this.re = real;
        this.im = imag;
    }

    /** Возвращает строковое представление комплексного числа
     * @return - строковое представление
     */
    @NonNull
    @Override
    public String toString() {
        if (im == 0) return re + "";
        if (re == 0) return im + "i";
        if (im <  0) return re + " - " + (-im) + "i";
        return re + " + " + im + "i";
    }

    /** Возвращает модуль текущего комплексного числа
     * @return модуль текущего комплексного числа
     */
    public float abs() {
        return (float) Math.hypot(re, im);
    }

    /** Возвращает аргумент текущего комплексного числа в диапазоне -pi...+pi
     * @return аргумент текущего комплексного числа
     */
    public float arg() {
        return (float) Math.atan2(im, re);
    }

    /** Сумма этого комплексного числа с другим
     * Результат возвращается в текущее число.
     *
     * @param b второе слогаемое
     * @return сумма
     */
    public Complex plus(Complex b) {
        this.re = this.re +b.re;
        this.im = this.im +b.im;
        return this;
    }

    /** Сумма двух комплексных чисел
     *
     * @param a слогаемое
     * @param b слогаемое
     */
    public void plus(Complex a, Complex b) {
        this.re = a.re +b.re;
        this.im = a.im +b.im;
    }

    /** Разность этого комплексного числа с другим
     * Результат возвращается в текущее число.
     *
     * @param b вычитаемое
     * @return разность
     */
    public Complex minus(Complex b) {
        this.re = this.re -b.re;
        this.im = this.im -b.im;
        return this;
    }

    /** Разность двух комплексных чисел
     *
     * @param a уменьшаемое
     * @param b вычитаемое
     * @return разность
     */
    public Complex minus(Complex a, Complex b) {
        this.re = a.re -b.re;
        this.im = a.im -b.im;
        return this;
    }

    /** Произведение этого комплексного числа на {@param b}
     * Результат возвращается в текущее число.
     *
     * @param b второй сомножитель
     * @return произведение
     */
    public Complex times(Complex b) {
        this.re = this.re *b.re -this.im *b.im;
        this.im = this.re *b.im +this.im *b.re;
        return this;
    }

    /** Произведение двух комплексных чисел.
     * @param a первый сомножитель
     * @param b второй сомножитель
     */
    public void times(Complex a, Complex b) {
        this.re = a.re *b.re -a.im *b.im;
        this.im = a.re *b.im +a.im *b.re;
    }

    /** Произведение комплексного числа на скаляр.
     * @param alpha скаляр
     */
    public void scale(float alpha) {
        this.re=this.re*alpha;
        this.im=this.im*alpha;
    }

    //TODO Добавить операции деления div(Complex b) и div(Complex a, Complex b) по аналогии c times()
    //TODO Добавить операции комплекного сопряжения
}
