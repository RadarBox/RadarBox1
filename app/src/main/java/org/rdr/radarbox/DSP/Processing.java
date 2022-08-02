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
    protected LinkedList<OperationDSP> processingSequence = new LinkedList<>();
    public Processing() {
        processingSequenceClear();
        processingSequenceAdd(RadarBox.freqSignals);
        //processingSequence.add(new OperationCorrection());
        //processingSequence.add(new OperationFFT());
    }

    public void processingSequenceClear() {
        processingSequence.clear();
    }

    public void processingSequenceAdd(OperationDSP operation) {
        if(!processingSequence.contains(operation))
            processingSequence.add(operation);
    }

    /** Главноая функция, которая вызывается для последовательного выполнения
     *  всех необходимых операций в рамках цифровой обработки сигналов.
     */
    public void doProcessing() {
        if(processingSequence.isEmpty()) return;
        processingSequence.getFirst().doOperation();
        for (int i = 1; i < processingSequence.size(); i++) {
            processingSequence.get(i).setInputSignals(
                    processingSequence.getFirst().getOutputSignals());
            processingSequence.get(i).doOperation();
        }
    }
}
