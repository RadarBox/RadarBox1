package org.rdr.radarbox.DSP;

import org.rdr.radarbox.DSP.Operations.OperationCorrection;
import org.rdr.radarbox.DSP.Operations.OperationDSP;
import org.rdr.radarbox.DSP.Operations.OperationFFT;
import org.rdr.radarbox.RadarBox;

import java.util.LinkedList;

/** Главный класс, содержащий всю последовательность обработки цифровых сигналов.
 */
public class Processing {
    /** Последовательность обработки сигналов. Создаётся в конструкторе класса */
    protected LinkedList<OperationDSP> processingSequence;
    public Processing() {
        processingSequence.add(RadarBox.freqSignals);
        processingSequence.add(new OperationCorrection());
        processingSequence.add(new OperationFFT());
    }

    /** Главноая функция, которая вызывается для последовательного выполнения
     *  всех необходимых операций в рамках цифровой обработки сигналов.
     */
    public void doProcessing() {
        processingSequence.getFirst().doOperation();
        for (int i = 1; i < processingSequence.size(); i++) {
            processingSequence.get(i).setInputSignals(
                    processingSequence.getFirst().getOutputSignals());
            processingSequence.get(i).doOperation();
        }
    }
}
