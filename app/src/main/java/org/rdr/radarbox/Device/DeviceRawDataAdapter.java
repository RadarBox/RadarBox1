package org.rdr.radarbox.Device;

/** Класс для перемешивания сырых данных с устройства и приведения их в единый формат.
 * {@link DimensionOrder} определяет все возможные порядки сбора данных.
 * @author Сапронов Данил
 * @version 1.0
 */
public class DeviceRawDataAdapter {
    /** <p>Порядок сбора данных (порядок измерений в одном кадре данных)</p>
     * <p>Объяснение формы записи:</p>
     * {@code TF_RX_TX} - сначала передаются данные для одного приёмо-передающего канала, для всех
     * частотно-временных отсчётов. Затем номер приёмника инкрементируется и прошлый шаг
     * повторяется. Затем номер передатчика инкрементируется и повторяются прошлые шаги.
     */
    public enum DimensionOrder {
        TF_RX_TX,
        TF_TX_RX,
        RX_TF_TX,
        RX_TX_TF,
        TX_RX_TF,
        TX_TF_RX
    }

    /** <p>Данные перемешиваются и приводятся к единому виду, где данные сортируются в следующем
     * порядке:</p>
     * <p>1 координата - время/частота,</p>
     * <p>2 координата - номер приёмника</p>
     * <p>3 координата - номер передатчика</p>
     * То есть в соответствии с порядком {@code TF_RX_TX} из {@link DimensionOrder}.
     *
     * @param rawData - массив сырых данных, распределённых в одном из нескольких
     *                порядков {@link DimensionOrder}. В случае успешного перемешивания, результат
     *                записывается в этот же массив.
     * @param dimensionOrder - порядок измерений в сырых данных:
     *                       (RX_TF_TX) - сначала данные со всех приёмников, затем по всем
     *                       точкам по времени/частоте, затем для всех передатчиков
     * @param tfCount - количество точек по частоте/времени
     * @param rxCount - количество приёмников
     * @param txCount - количество передатчиков
     * @param isComplex - true, если отсчёты комплексные
     *
     * @return true, если переданы корректные аргументы и выполнено преобразование, либо если
     * данные уже лежат в правильном порядке.
     * Результат помещается во входной массив {@code rawData}.
     */
    public static boolean reshuffleRawData(short[] rawData, DimensionOrder dimensionOrder,
                            int tfCount, int rxCount, int txCount, boolean isComplex){

        if (rxCount<0 || txCount<0 || tfCount<0)
            return false;
        int ps = 1; // PointSize - размер одной точки (один отсчёт / два комплексных отсчёта)
        if (isComplex) ps = 2;
        if(rawData.length!=txCount*rxCount*tfCount*ps)
            return false;
        if(rxCount==1 && txCount==1)
            return true;
        //различный порядок взятия отсчётов в зависимости от порядка измерений
        int txSkipStep=1;
        int rxSkipStep=1;
        int tfSkipStep=1;
        switch (dimensionOrder) {
            case TF_RX_TX:
                // данные уже лежат в правильном пордяке
                return true;
            case TF_TX_RX:
                txSkipStep = tfCount*ps;
                rxSkipStep = txCount*tfCount*ps;
                tfSkipStep = ps;
                break;
            case RX_TF_TX:
                txSkipStep = tfCount*rxCount*ps;
                tfSkipStep = rxCount*ps;
                rxSkipStep = ps;
                break;
            case RX_TX_TF:
                txSkipStep = rxCount*ps;
                rxSkipStep = ps;
                tfSkipStep = txCount*rxCount*ps;
                break;
            case TX_RX_TF:
                txSkipStep = ps;
                tfSkipStep = txCount*rxCount*ps;
                rxSkipStep = txCount*ps;
                break;
            case TX_TF_RX:
                txSkipStep = ps;
                tfSkipStep = txCount*ps;
                rxSkipStep = tfCount*txCount*ps;
                break;
        }
        //создание временного массива для выполнения перемешивания
        short[] reshuffledRawData = new short[rawData.length];

        for(int tx=0; tx<txCount; tx++) {
            int txReshuffledStep = tx*rxCount*tfCount*ps;
            for (int rx=0; rx<rxCount; rx++) {
                int rxReshuffledStep = rx*tfCount*ps;
                for(int tf=0; tf<tfCount; tf++) {
                    // этот цикл нужен, чтобы учесть, что данные бывают комплексные
                    for(int p=0; p<ps; p++) {
                        reshuffledRawData[txReshuffledStep + rxReshuffledStep + tf * ps + p] =
                                rawData[tx * txSkipStep + rx * rxSkipStep + tf * tfSkipStep + p];
                    }

                }
            }
        }
        //перемешивание завершено, копируем данные во входной массив и возвращаем true
        System.arraycopy(reshuffledRawData,0,rawData,0,rawData.length);
        return true;
    }

}
