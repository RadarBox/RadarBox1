package org.rdr.radarbox.DSP;

/** Класс комплексных чисел.
 * Пример использования:
 * Создание двух комплексных чисел и запись их перемножение с сохранением результата в 1-е число:
 *  Complex a(1,2); Complex b(3,4); a.times(b);
 * @author Danil Sapronov
 * @version 0.2
 */
public class Complex {

    public double re;
    public double im;

    /** Создание комплексного числа в формате re+i*im
     *
     * @param real - действительная составляющая
     * @param imag - мнимая составляющая
     */
    public Complex(double real, double imag) {
        this.re = real;
        this.im = imag;
    }

    /** Возвращает модуль текущего комплексного числа
     * @return модуль текущего комплексного числа
     */
    public double abs() {
        return Math.sqrt(this.re * this.re + this.im * this.im);
    }

    /** Возвращает аргумент текущего комплексного числа
     * @return аргумент текущего комплексного числа
     */
    public double arg() {
        return Math.atan2(im, re);
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
     * @return сумма
     */
    public Complex plus(Complex a, Complex b) {
        this.re = a.re +b.re;
        this.im = a.im +b.im;
        return this;
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
     * @return произведение
     */
    public Complex times(Complex a, Complex b) {
        this.re = a.re *b.re -a.im *b.im;
        this.im = a.re *b.im +a.im *b.re;
        return this;
    }

    //TODO Добавить операции деления div(Complex b) и div(Complex a, Complex b) по аналогии c times()
    //TODO Добавить операции комплекного сопряжения
}
