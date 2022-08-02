package org.rdr.radarbox.DSP.Operations;

import org.rdr.radarbox.DSP.ComplexSignal;
import org.rdr.radarbox.DSP.FFT;

import java.util.ArrayList;

public class OperationFFT implements OperationDSP {
    ArrayList<ComplexSignal> inputSignals;
    ArrayList<ComplexSignal> outputSignals;
    Integer fftLength;
    String outputUnitsX = "";
    float[] x;

    @Override
    public String getName() {
        return "FFT";
    }

    @Override
    public void setInputSignals(ArrayList<ComplexSignal> inputSignals) {
        if(inputSignals==null)
            throw new IllegalArgumentException("inputSignals is null");
        this.inputSignals=inputSignals;
    }

    @Override
    public ArrayList<ComplexSignal> getOutputSignals() {
        return this.outputSignals;
    }

    /** Чтобы в результате преобразования Фурье были правильные единицы измерения по оси X
     * @param input входной сигнал (до БПФ)
     */
    private void setOutputUnitsXbasedOnInput(ComplexSignal input) {
        x = new float[fftLength];
        if(input.getUnitsX().equals("MHz")) {
            outputUnitsX = "ns";
            float fullSpectrumWidth = (input.getX()[(input.getX().length-1)]-input.getX()[0]);
            for(int i=0; i<fftLength; i++)
                x[i] = i/fullSpectrumWidth/1000;
        }
        else {
            for(int i=0; i<fftLength; i++)
                x[i] = i;
        }
    }

    @Override
    public void doOperation() {
        if(inputSignals==null)
            return;

        setOutputUnitsXbasedOnInput(inputSignals.get(0));
        for (int i=0; i<inputSignals.size(); i++) {
            ComplexSignal inputSignal = inputSignals.get(i);
            // создание выходного сигнала
            outputSignals.add(new ComplexSignal(fftLength)
                    .setName(inputSignal.getName())
                    .setX(x)
                    .setUnitsX(outputUnitsX)
            );
            // выполнение операции преобразования Фурье
            // 1) копирование из входного сигнала в массив выходного сигнала
            for (int j=0; j<Math.min(inputSignal.getLength(),fftLength); j++)
                outputSignals.get(i).getY()[j]=inputSignal.getY()[j];
            // 2) вызов функции БПФ
            FFT.fft(outputSignals.get(i).getY());
        }
    }

    @Override
    public boolean setParameters(Object parameters) throws IllegalArgumentException {
        if(!(parameters instanceof Integer))
            throw new IllegalArgumentException(
                    "Input argument is not instance of Integer");
        fftLength = (Integer)parameters;
        if(!FFT.isPowerOfTwo(fftLength))
            fftLength=FFT.nextPowerOf2(fftLength);
        return true;
    }
}
