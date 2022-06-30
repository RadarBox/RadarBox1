package org.rdr.radarbox.DSP;

/** Класс комлпексных чисел.
 * Пример использования:
 * Создание двух комплексных чисел и запись их перемножение с сохранением результата в 1-е число:
 *  Complex a(1,2); Complex b(3,4); a.times(b);
 * Создание двух чисел в экспоненциальной форме их сложение с записью в третье число:
 *  Complex a(1, 0.4, ABS_ARG), b(1, -0.4, ABS_ARG), c(0,0);
 *  c.plus(a,b);
 * @author Danil Sapronov
 * @version 0.1
 */
public class Complex {
    /** Тип записи комплексного числа:
     * RE_IM    -> Re + i*Im
     * ABS_ARG  -> ABS*exp(i*ARG)
     */
    enum Type {
        RE_IM,
        ABS_ARG
    }
    private double real;
    private double imag;
    private double arg;
    private double abs;

    /** Создание комплексного числа в формате real+i*imag
     *
     * @param real - действительная составляющая
     * @param imag - мнимая составляющая
     */
    public Complex(double real, double imag) {
        this.real=real;
        this.imag = imag;
        this.abs = Math.sqrt(real * real + imag * imag);
        this.arg = Math.atan2(imag,real);
    }

    /** Создание комплексного числа из двух чисел. Поддерживается запись в любой из форм:
     * действительная, мнимая составляющие ЛИБО модуль, аргумент
     *
     * @param real_abs действительная составляющая ЛИБО модуль
     * @param imag_arg мнимая составляющая ЛИБО аргумента
     * @param type тип записи создаваемого комплексного числа RE_IM либо ABS_ARG
     */
    public Complex(double real_abs, double imag_arg, Type type) {
        if(type.equals(Type.RE_IM)) {
            this.real = real_abs;
            this.imag = imag_arg;
            this.abs = Math.sqrt(real * real + imag * imag);
            this.arg = Math.atan2(imag,real);
        } else if(type.equals(Type.ABS_ARG)) {
            this.abs = real_abs;
            this.arg = imag_arg;
            this.real = abs*Math.cos(arg);
            this.imag = abs*Math.sin(arg);
        }
    }

    /** Сумма этого комплексного числа с другим
     * Результат возвращается в текущее число.
     *
     * @param b второе слогаемое
     * @return сумма
     */
    public Complex plus(Complex b) {
        this.real = this.real+b.real;
        this.imag = this.imag+b.imag;
        this.abs = Math.sqrt(real * real + imag * imag);
        this.arg = Math.atan2(imag,real);
        return this;
    }

    /** Сумма двух комплексных чисел
     *
     * @param a слогаемое
     * @param b слогаемое
     * @return сумма
     */
    public Complex plus(Complex a, Complex b) {
        this.real = a.real+b.real;
        this.imag = a.imag+b.imag;
        this.abs = Math.sqrt(real * real + imag * imag);
        this.arg = Math.atan2(imag,real);
        return this;
    }

    /** Разность этого комплексного числа с другим
     * Результат возвращается в текущее число.
     *
     * @param b вычитаемое
     * @return разность
     */
    public Complex minus(Complex b) {
        this.real = this.real-b.real;
        this.imag = this.imag-b.imag;
        this.abs = Math.sqrt(real * real + imag * imag);
        this.arg = Math.atan2(imag,real);
        return this;
    }

    /** Разность двух комплексных чисел
     *
     * @param a уменьшаемое
     * @param b вычитаемое
     * @return разность
     */
    public Complex minus(Complex a, Complex b) {
        this.real = a.real-b.real;
        this.imag = a.imag-b.imag;
        this.abs = Math.sqrt(real * real + imag * imag);
        this.arg = Math.atan2(imag,real);
        return this;
    }

    /** Произведение этого комплексного числа на {@param b}
     * Результат возвращается в текущее число.
     *
     * @param b второй сомножитель
     * @return произведение
     */
    public Complex times(Complex b) {
        this.real = this.real*b.real-this.imag*b.imag;
        this.imag = this.real*b.imag+this.imag*b.real;
        this.abs = Math.sqrt(real * real + imag * imag);
        this.arg = Math.atan2(imag,real);
        return this;
    }

    /** Произведение двух комплексных чисел.
     * @param a первый сомножитель
     * @param b второй сомножитель
     * @return произведение
     */
    public Complex times(Complex a, Complex b) {
        this.real = a.real*b.real-a.imag*b.imag;
        this.imag = a.real*b.imag+a.imag*b.real;
        this.abs = Math.sqrt(real * real + imag * imag);
        this.arg = Math.atan2(imag,real);
        return this;
    }

    //TODO Добавить операции деления div(Complex b) и div(Complex a, Complex b) по аналогии c times()
    //TODO Добавить операции комплекного сопряжения
    //TODO Добавить геттеры и сеттеры (учитывая, что в сеттерах должны меняться и вторые представления числа)

}
