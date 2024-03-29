package org.rdr.radarbox.DSP.Operations;

import org.rdr.radarbox.DSP.ComplexSignal;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Интерфейс абстрактной операции в рамках цифровой обработки сигналов. <p>
 * Абстрактная операция представляется, как чёрный ящик, у которого есть набор входных сигналов,
 * набор выходных сигналов и название. Весь процесс выполнения операции происходит в методе
 * {@link #doOperation()};
 */
public interface OperationDSP extends Serializable {
    String getName();
    void setInputSignals(ArrayList<ComplexSignal> inputSignals);
    ArrayList<ComplexSignal> getOutputSignals();
    void doOperation();

    /**
     * Абстрактный метод, который подразумевает, что в реализации конкретной операции цифровой
     * обработки сигналов в нём будут задаваться параметры этой операции
     * @param parameters новые параметры операции (количество точек для БПФ, порядок для фильтра и т.д.)
     * @throws IllegalArgumentException должно выбрасываться тогда, когда с аргументом метода
     * есть проблемы
     * @return true, если параметры установлены
     */
    boolean setParameters(Object parameters) throws IllegalArgumentException;
}
