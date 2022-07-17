package org.rdr.radarbox.Radargram;

import org.rdr.radarbox.DataThreadService;
import org.rdr.radarbox.RadarBox;

import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.function.Consumer;

import androidx.lifecycle.ViewModel;

public class RadargramViewModel extends ViewModel {

    // TODO добавить такую же обработку для временных данных типа double

    // под каждый канал создаётся свой двумерный массив,
    // который будет хранить последние [freqBufferSize]x[freqCount] частотных отсчётов
    private int freqBufferSize = 100, freqCount = 0;
    private ArrayList<short[]> freqBufferList = null;
    public RadargramViewModel () {
        //настраивается "обозреватель" состояния главного потока чтения данных
        RadarBox.dataThreadService.getLiveDataThreadState().observeForever( dataThreadState -> {
            // при нажатии на клавишу "старт" обновляются буферы с кадрами данных всех каналов
            if(dataThreadState.equals(DataThreadService.DataThreadState.STARTED)) {
                resetFreqBuffers();
            }
        });
        // при приходе нового кадра данных, его содержимое складывается в буферы частотных данных
        RadarBox.dataThreadService.getLiveFrameCounter().observeForever(frameNumber -> {
            addFrameToFreqBuffers();
        });
    }

    /** метод обновляет частотные буфферы данных в списке freqBufferList */
    private void resetFreqBuffers() {
        //если размер не изменился, только задать первый кадр максимальным
        if(freqCount==RadarBox.freqSignals.getFN() &&
                RadarBox.freqSignals.getChN()==freqBufferList.size()) {
            freqBufferList.forEach(shortBuffer -> {
                for (int i = 0; i < freqCount; i++) shortBuffer[i] = Short.MAX_VALUE;
            });
            return;
        }
        //eсли изменился размер кадра или количество каналов, то пересаздаём массивы
        freqBufferList.clear();
        freqCount = RadarBox.freqSignals.getFN();
        for(int ch=0; ch<RadarBox.freqSignals.getChN(); ch++) {
            freqBufferList.add(new short[freqBufferSize*freqCount]);
        }
    }

    /** метод добавляет новые радиолокацинные данные в буферы */
    private void addFrameToFreqBuffers() {
        // временный промежуточный массив для хранения одного частотного сигнала для каждого канала
        short[] oneChannelFreqSignal = new short[freqCount*2];
        for(int ch=0; ch<RadarBox.freqSignals.getChN(); ch++) {
            // сдвигаем содержимое массива на один кадр вперёд
            System.arraycopy(
                    freqBufferList.get(ch),0,
                    freqBufferList.get(ch),freqCount,
                    freqBufferList.get(ch).length-freqCount);
            // забираем данные из RadarBox.FreqSignals и записываем в последние кадры каждого канала
            RadarBox.freqSignals.getRawFreqOneChannelSignal(ch,oneChannelFreqSignal);
            for(int f=0; f<freqCount; f++)
                freqBufferList.get(ch)[f]=oneChannelFreqSignal[2*f]; //только действительная часть
        }
    }

    public short[] getOneChannelFreqBuffer(int channel) {return freqBufferList.get(channel);}
}