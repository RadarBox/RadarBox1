package org.rdr.radarbox.DSP;

import androidx.annotation.NonNull;

import java.io.Serializable;

/** Класс комплексных чисел. <p>
 * За основу взят тип данных float для экономии ресурсов.
 * (для радарных применений редко требуется бОльшая точность)
 * Пример использования:
 * Создание двух комплексных чисел и запись их перемножения с сохранением результата в 1-е число:
 *  Complex a(1,2); Complex b(3,4); a.times(b);
 * @author Danil Sapronov
 * @version 1.0
 */
public class Complex implements Serializable {

    public float re;
    public float im;

    /** Создание комплексного числа в формате re+i*im: re = 0, im = 0. */
    public Complex() {
        re=0;
        im=0;
    }

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

    /** Сумма этого комплексного числа с другим <p>
     * Результат возвращается в объект, вызывающий метод.
     *
     * @param b второе слогаемое
     */
    public void plus(Complex b) {
        this.re = this.re +b.re;
        this.im = this.im +b.im;
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

    /** Разность этого комплексного числа с другим <p>
     * Результат возвращается в объект, вызывающий метод.
     *
     * @param b вычитаемое
     */
    public void minus(Complex b) {
        this.re = this.re -b.re;
        this.im = this.im -b.im;
    }

    /** Разность двух комплексных чисел.<p>
     * Результат возвращается в объект, вызывающий метод.
     *
     * @param a уменьшаемое
     * @param b вычитаемое
     *
     */
    public void minus(Complex a, Complex b) {
        this.re = a.re -b.re;
        this.im = a.im -b.im;
    }

    /** Произведение этого комплексного числа на {@param b} <p>
     * Результат возвращается в объект, вызывающий метод.
     *
     * @param b второй сомножитель
     * @return произведение
     */
    public Complex times(Complex b) {
        float tempRe = this.re;
        this.re = tempRe *b.re -this.im *b.im;
        this.im = tempRe *b.im +this.im *b.re;
        return this;
    }

    /** Произведение двух комплексных чисел. <p>
     * Результат возвращается в объект, вызывающий метод.
     * @param a первый сомножитель
     * @param b второй сомножитель
     */
    public void times(Complex a, Complex b) {
        this.re = a.re *b.re -a.im *b.im;
        this.im = a.re *b.im +a.im *b.re;
    }

    /** Произведение комплексного числа на скаляр. <p>
     * Результат возвращается в объект, вызывающий метод.
     * @param alpha скаляр
     */
    public void scale(float alpha) {
        this.re=this.re*alpha;
        this.im=this.im*alpha;
    }

    /** Деление двух комплексных чисел. <p>
     * Результат возвращается в объект, вызывающий метод.
     * @param a делимое
     * @param b делитель
     */
    public void div(Complex a, Complex b) {
        this.re = (a.re *b.re +a.im *b.im)/(b.re*b.re+b.im*b.im);
        this.im = (b.re *a.im -a.re *b.im)/(b.re*b.re+b.im*b.im);
    }

    /** Комплексное сопряжение <p>
     * Результат возвращается в объект, вызывающий метод.
     */
    public void conj() {
        this.im = -1*this.im;
    }
}
