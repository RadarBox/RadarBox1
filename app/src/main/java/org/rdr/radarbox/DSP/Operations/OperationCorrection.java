package org.rdr.radarbox.DSP.Operations;


import org.rdr.radarbox.DSP.ComplexSignal;

import java.util.ArrayList;

/** Класс, применяющий некоторую амплитудно-фазовую коррекцию к сигналам.
 * В простейшем случае, применяющий "калибровочные коеффициенты" к частотным данным сигнала.
 */
public class OperationCorrection implements OperationDSP {
    ArrayList<ComplexSignal> correctionCoefficients;
    ArrayList<ComplexSignal> inputSignals;
    ArrayList<ComplexSignal> outputSignals;
    @Override
    public String getName() {
        return "Amplitude and phase correction";
    }

    @Override
    public void setInputSignals(ArrayList<ComplexSignal> inputSignals) {
        this.inputSignals = inputSignals;
    }

    @Override
    public ArrayList<ComplexSignal> getOutputSignals() {
        return outputSignals;
    }

    @Override
    public void doOperation() {
        if(inputSignals==null) return;
        for (int i=0; i<inputSignals.size(); i++) {
            ComplexSignal inputSignal = inputSignals.get(i);
            // создание выходного сигнала
            outputSignals.add(new ComplexSignal(inputSignal.getLength())
                    .setName("Signal after correction")
                    .setX(inputSignal.getX())
                    .setUnitsX(inputSignal.getUnitsX())
                    .setUnitsY(inputSignal.getUnitsY())
            );
            // выполнение операции произведения входного сигнала на калибровочные коэффициенты
            outputSignals.get(i).mult(inputSignal,correctionCoefficients.get(i));
        }
    }

    @Override
    public boolean setParameters(Object parameters) throws IllegalArgumentException {
        if(!(parameters instanceof ArrayList<?>))
            throw new IllegalArgumentException(
                    "Input argument is not instance of ArrayList<?>");
        else if(!(((ArrayList) parameters).get(0) instanceof ComplexSignal))
            throw new IllegalArgumentException(
                    "Input argument is ArrayList<?> but not instance of ArrayList<ComplexSignal>");
        correctionCoefficients = (ArrayList<ComplexSignal>) parameters;
        return true;
    }
}
