package org.rdr.radarbox.DSP;

/**
 *  Класс {@code FFT} предоставляет методы для вычисления БПФ, обратного БПФ, линейной и
 *  кольцевой свёртки двух комплексных массивов. Основа взята с сайта Принстонского университета.
 *  It is a bare-bones implementation that runs in <em>n</em> log <em>n</em> time,
 *  where <em>n</em> is the length of the complex array. For simplicity,
 *  <em>n</em> must be a power of 2.
 *  Our goal is to optimize the clarity of the code, rather than performance.
 *  It is not the most memory efficient implementation because it uses
 *  objects to represents complex numbers and it it re-allocates memory
 *  for the subarray, instead of doing in-place or reusing a single temporary array.
 *  <p>
 *  This computes correct results if all arithmetic performed is
 *  without floating-point rounding error or arithmetic overflow.
 *  In practice, there will be floating-point rounding error.
 *  <p>
 *  For additional documentation,
 *  see <a href="https://algs4.cs.princeton.edu/99scientific">Section 9.9</a> of
 *  <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 *
 *  @author Robert Sedgewick
 *  @author Kevin Wayne
 *  @author Danil Sapronov
 */
public class FFT {

    private static final Complex ZERO = new Complex(0, 0);

    /**
     * Do not instantiate.
     */
    private FFT() {
    }

    public static boolean isPowerOfTwo(int n)
    {
        return (int)(Math.ceil((Math.log(n) / Math.log(2))))
                == (int)(Math.floor(((Math.log(n) / Math.log(2)))));
    }

    public static int nextPowerOf2(int a)
    {
        int b = 1;
        while (b < a) {
            b = b << 1;
        }
        return b;
    }

    /**
     * Returns the FFT of the specified complex array.
     *
     * @param x the complex array
     * @return the FFT of the complex array {@code x}
     * @throws IllegalArgumentException if the length of {@code x} is not a power of 2
     */
    public static Complex[] fft(Complex[] x) {
        int n = x.length;

        // base case
        if (n == 1) {
            return new Complex[]{x[0]};
        }

        // radix 2 Cooley-Tukey FFT
        if (!isPowerOfTwo(n)) {
            throw new IllegalArgumentException("n is not a power of 2");
        }

        // fft of even terms
        Complex[] even = new Complex[n / 2];
        for (int k = 0; k < n / 2; k++) {
            even[k] = x[2 * k];
        }
        Complex[] q = fft(even);

        // fft of odd terms
        Complex[] odd = even;  // reuse the array
        for (int k = 0; k < n / 2; k++) {
            odd[k] = x[2 * k + 1];
        }
        Complex[] r = fft(odd);

        // combine
        Complex[] y = new Complex[n];
        for (int i=0; i<n; i++) { // к сожалению, приходится ещё выделять память под каждый объект
            y[i]=new Complex();
        }
        for (int k = 0; k < n / 2; k++) {
            double kth = -2 * k * Math.PI / n;
            Complex wk = new Complex((float)Math.cos(kth), (float)Math.sin(kth));
            wk.times(r[k]);
            y[k].plus(q[k], wk);
            y[k + n / 2].minus(q[k], wk);
        }
        return y;
    }


    /**
     * Returns the inverse FFT of the specified complex array.
     *
     * @param x the complex array
     * @return the inverse FFT of the complex array {@code x}
     * @throws IllegalArgumentException if the length of {@code x} is not a power of 2
     */
    public static Complex[] ifft(Complex[] x) {
        int n = x.length;
        Complex[] y = new Complex[n];

        // take conjugate of input array
        for (int i = 0; i < n; i++) {
            y[i].re = x[i].re;
            y[i].im = -x[i].im;
        }

        // compute forward FFT
        y = fft(y);

        // take conjugate again
        for (int i = 0; i < n; i++) {
            y[i].im = -y[i].im;
        }

        // divide by n
        for (int i = 0; i < n; i++) {
            y[i].scale((float) (1.0 / n));
        }

        return y;
    }

    /**
     * Returns the circular convolution of the two specified complex arrays.
     *
     * @param x one complex array
     * @param y the other complex array
     * @return the circular convolution of {@code x} and {@code y}
     * @throws IllegalArgumentException if the length of {@code x} does not equal
     *                                  the length of {@code y} or if the length is not a power of 2
     */
    public static Complex[] cconvolve(Complex[] x, Complex[] y) {

        // should probably pad x and y with 0s so that they have same length
        // and are powers of 2
        if (x.length != y.length) {
            throw new IllegalArgumentException("Dimensions don't agree");
        }

        int n = x.length;

        // compute FFT of each sequence
        Complex[] a = fft(x);
        Complex[] b = fft(y);

        // point-wise multiply
        Complex[] c = new Complex[n];
        for (int i = 0; i < n; i++) {
            c[i].times(a[i], b[i]);
        }

        // compute inverse FFT
        return ifft(c);
    }

    /**
     * Returns the linear convolution of the two specified complex arrays.
     *
     * @param x one complex array
     * @param y the other complex array
     * @return the linear convolution of {@code x} and {@code y}
     * @throws IllegalArgumentException if the length of {@code x} does not equal
     *                                  the length of {@code y} or if the length is not a power of 2
     */
    public static Complex[] convolve(Complex[] x, Complex[] y) {
        Complex[] a = new Complex[2 * x.length];
        for (int i = 0; i < x.length; i++)
            a[i] = x[i];
        for (int i = x.length; i < 2 * x.length; i++)
            a[i] = ZERO;

        Complex[] b = new Complex[2 * y.length];
        for (int i = 0; i < y.length; i++)
            b[i] = y[i];
        for (int i = y.length; i < 2 * y.length; i++)
            b[i] = ZERO;

        return cconvolve(a, b);
    }
}
