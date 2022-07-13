package org.rdr.radarbox.DSP;

import java.util.ArrayList;

/**
 * Интерфейс абстрактной операции в рамках цифровой обработки сигналов. <p>
 * Абстрактная операция представляется, как чёрный ящик, у которого есть набор входных сигналов,
 * набор выходных сигналов и название. Весь процесс выполнения операции происходит в методе
 * {@link #doOperation()};
 */
public interface OperationDSP {
    String getName();
    ArrayList<ComplexSignal> getInputSignals();
    void setInputSignals(ArrayList<ComplexSignal> inputSignals);
    ArrayList<ComplexSignal> getOutputSignals();
    void doOperation();
}
