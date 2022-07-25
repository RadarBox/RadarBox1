package org.rdr.radarbox.DSP.Operations;

import org.rdr.radarbox.DSP.ComplexSignal;
import org.rdr.radarbox.DSP.OperationDSP;

import java.util.ArrayList;

/** Класс, применяющий некоторую амплитудно-фазовую коррекцию к сигналам.
 * В простейшем случае, применяющий "калибровочные коеффициенты" к частотным данным сигнала.
 */
public class SignalCorrection implements OperationDSP {
    @Override
    public String getName() {
        return "Amplitude and phase correction";
    }

    @Override
    public ArrayList<ComplexSignal> getInputSignals() {
        return null;
    }

    @Override
    public void setInputSignals(ArrayList<ComplexSignal> inputSignals) {

    }

    @Override
    public ArrayList<ComplexSignal> getOutputSignals() {
        return null;
    }

    @Override
    public void doOperation() {

    }
}
