package org.rdr.radarbox;

import org.rdr.radarbox.Device.DataChannel;
import org.rdr.radarbox.Device.DeviceConfiguration;
import org.rdr.radarbox.File.AoRDSettingsManager;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static org.rdr.radarbox.RadarBox.device;
import static org.rdr.radarbox.RadarBox.logger;

/** Класс для распараллеливания операций сбора, обработки и сохранения данных.
 * Здесь задаётся период главного таймера приложения. Также внутри данного класса созадаётся главный
 * ThreadPoolExecutor, который настраивает количество доступных потоков. Кроме того создаются
 * классы с интерфейсом Runnable для каждого типа операции (сбор данных, обработка,
 * вывод на экран и т.д.), которые будут выполняться в отдельном потоке.
 * @author Сапронов Данил Игоревич
 * @version 1.0
 */
public class DataThreadService {
    public enum DataSource {
        DEVICE,
        FILE,
        NO_SOURCE;
    }

    private final MutableLiveData<DataSource> liveCurrentSource = new MutableLiveData<>();
    /** LiveData интерфейс для генерирования и обозревания событий смены источников данных */
    public LiveData<DataSource> getLiveCurrentSource() {return liveCurrentSource;}

    public enum DataThreadState {
        STARTED,
        STOPPED;
    }
    private final MutableLiveData<DataThreadState> liveDataThreadState = new MutableLiveData<>();
    /** LiveData интерфейс для генерирования и обозревания событий получения данных */
    public LiveData<DataThreadState> getLiveDataThreadState() {return liveDataThreadState;}

    /** Cчетчик кадров. Значение этой переменной инкрементируется в {@link UiUpdater}*/
    private int frameCounter;

    private final MutableLiveData<Integer> liveFrameCounter = new MutableLiveData<>();
    /** LiveData интерфейс для генерирования и обозревания событий получения, обработки и сохранения
     * очередного кадра данных */
    public LiveData<Integer> getLiveFrameCounter() {return liveFrameCounter;}

    /** Cчетчик опросов статуса. Значение этой переменной инкрементируется в {@link StatusGetter}*/
    private int statusCounter;

    private final MutableLiveData<Integer> liveStatusCounter = new MutableLiveData<>();
    /** LiveData интерфейс для генерирования и обозревания событий обновления статуса устройства*/
    public LiveData<Integer> getLiveStatusCounter() {return liveStatusCounter;}


    /** Период основного таймера в мс */
    private int period;

    /** Установка периода основного таймера, по которому в параллельных потоках будут происходить
     * операции сбора, обработки и записи данных
     * @param period период в миллисекундах
     */
    public void setPeriod(int period) {this.period = period;}

    /** Период основного таймера, по которому в параллельных потоках будут происходить
     * операции сбора, обработки и записи данных
     * @return период в миллисекундах
     */
    public int getPeriod() {return this.period;}

    /** переменная, осхраняющая момент запуска сбора данных кадра */
    private long tStart=0;

    /** интервал между обработкой двух последних кадров в [мс] */
    private long lastFrameTimeInterval = 0;
    /** Интервал между обработкой двух последних кадров.
     * Этот интевал учитывает полный цикл, включающий сбор данных, обработку и отображение.
     * Можно отслеживать пиковые задержки.
     * @return мгновенный интервал в миллисекундах*/
    public long getLastFrameTimeInterval() {return lastFrameTimeInterval;}

    /** полное время сканирования с момента последнего старта [мс]*/
    private long fullScanningTime = 0;
    /** Полное время текущего сканирования с момента старта.
     * Если использовать данное значение совместно со счётчиком кадров
     * {@link #getLiveFrameCounter()}, то можно вычислить
     * средний временной интервал между сканированиями или средний период сканирования.
     * В норме он не должен превышать {@link #period}
     * Можно отслеживать пиковые задержки.
     * @return время в [мс] */
    public long getFullScanningTime() {return fullScanningTime;}

    private final DataReading dataReading;
    private final DataSaving dataSaving;
    private final SignalProcessing signalProcessing;
    private final UiUpdater uiUpdater;
    private final StatusGetter statusGetter;
    private ScheduledFuture<?> statusFuture;
    private CyclicBarrier barrier;
    private final ArrayList<ScheduledFuture<?>> taskList;
    ScheduledExecutorService executor;

    public DataThreadService() {
        dataReading = new DataReading();
        dataSaving = new DataSaving();
        signalProcessing = new SignalProcessing();
        uiUpdater = new UiUpdater();
        executor = Executors.newScheduledThreadPool(4);

        statusGetter = new StatusGetter();
        taskList = new ArrayList<>(0);

        setDeviceDataSourceAutoSelection();
        liveDataThreadState.setValue(DataThreadState.STOPPED);
        if(setDataSource(DataSource.DEVICE))
            return;
        if(setDataSource(DataSource.FILE))
            return;
        setDataSource(DataSource.NO_SOURCE);
    }

    public void start() {
        if(!liveCurrentSource.getValue().equals(DataSource.NO_SOURCE) && taskList.isEmpty()) {
            stopGettingStatusAtFixedRate();
            logger.add(this,"Timer started. DataSource: "+liveCurrentSource.getValue()+
                    " Period: "+period);
            int periodForShedule = period;
            if(period==0) periodForShedule=1;
            tStart=System.currentTimeMillis(); fullScanningTime=0;
            barrier = new CyclicBarrier(3, uiUpdater);
            frameCounter = 0; liveFrameCounter.setValue(frameCounter);
            taskList.add(executor.scheduleAtFixedRate(
                    dataReading, 0, periodForShedule, TimeUnit.MILLISECONDS));
            taskList.add(executor.scheduleAtFixedRate(
                    dataSaving, 0, periodForShedule, TimeUnit.MILLISECONDS));
            taskList.add(executor.scheduleAtFixedRate(
                    signalProcessing, 0, periodForShedule, TimeUnit.MILLISECONDS));
            liveDataThreadState.postValue(DataThreadState.STARTED);
        }
    }

    public void stop() {
        logger.add(this,"Timer stopped. DataSource: " + liveCurrentSource.getValue() +
                "\tPeriod: " + period);
        if(!taskList.isEmpty()) {
            for (ScheduledFuture<?> task : taskList) {
                task.cancel(true);
            }
            taskList.clear();
            liveDataThreadState.postValue(DataThreadState.STOPPED);
            if(Objects.equals(liveCurrentSource.getValue(), DataSource.DEVICE))
                startGettingStatusAtFixedRate();
        }
    }

    /** Если мы не работали с устройством и возникает связь с устройством хотя бы по одному
     * из каналов, перестраиваемся на получение данных с устройства. Если мы работали с устройством,
     * то ничего не происходит. Если мы работали с устройством и связь по подключённому каналу
     * пропала, переключаемся на NO_SOURCE.
     */
    private void setDeviceDataSourceAutoSelection() {
        if(RadarBox.device!=null) {
            RadarBox.device.communication.getLiveConnectedChannel().observeForever(
                    connectedDataChannel -> {
                        if (connectedDataChannel == null &&
                                liveCurrentSource.getValue().equals(DataSource.DEVICE)) {
                            // остановить зондирование
                            stop();
                            // попробовать переключиться на файл. Если его нет, то на NO_SOURCE
                            setDataSource(DataSource.NO_SOURCE);
                        }
                        else if (!liveCurrentSource.getValue().equals(DataSource.DEVICE)) {
                            setDataSource(DataSource.DEVICE);
                        }
                    });
        }
    }

    /** Установка текущего источника радиолокационных данных
     * @param dataSource (устройство, файл или NO_SOURCE)
     * @return true, если установка источника данных произошла успешно
     */
    public boolean setDataSource(DataSource dataSource) {

        if (dataSource.equals(DataSource.FILE)) {
            if (RadarBox.fileRead == null)
                return false;
            if (RadarBox.fileRead.config.getVirtual() != null) {
                RadarBox.freqSignals.updateSignalParameters(RadarBox.fileRead.config.getVirtual());
                period = RadarBox.fileRead.config.getVirtual().getIntParameterValue(
                        "Trep");
                if (period != -1) {
                    RadarBox.fileRead.config.getVirtual().getParameters().stream().filter(
                            parameter -> parameter.getID().equals("Trep")
                    ).findAny().ifPresent(parameter ->
                            ((DeviceConfiguration.IntegerParameter)parameter).getLiveValue()
                                    .observeForever(value->period=value));
                    // если мы переключились с устройства на файл, то останавливаем получение статуса
                    stopGettingStatusAtFixedRate();
                    liveCurrentSource.postValue(DataSource.FILE);
                    return true;
                }
                else {
                    logger.add("DataThreadService", "No Trep parameter in parameter list");
                    return false;
                }
            }
            logger.add("DataThreadService",
                    "ReadFile not opened or DeviceConfiguration is null");
            return false;
        }
        else if(dataSource.equals(DataSource.DEVICE)) {
            if(RadarBox.device==null) // в списке устройств в RadarBox отсутствует устройство
                return false;
            if(device.communication.getLiveConnectedChannel().getValue()==null)
                return false;
            if(!device.communication.getLiveConnectedChannel().getValue()
                    .getLiveState().getValue().equals(DataChannel.ChannelState.CONNECTED))
                return false;
            RadarBox.freqSignals.updateSignalParameters(RadarBox.device.configuration);
            DeviceConfiguration.Parameter param =
                    RadarBox.device.configuration.getParameters().stream().filter(
                    parameter -> parameter.getID().equals("Trep")
            ).findAny().orElse(null);
            if(param != null) ((DeviceConfiguration.IntegerParameter) param)
                    .getLiveValue().observeForever(value-> {
                        period = value;
                    });
            else {
                period = 0;
                logger.add("DataThreadService","No Trep parameter in parameter list");
                return false;
            }
            liveCurrentSource.postValue(dataSource);
            startGettingStatusAtFixedRate();
            return true;
        }
        // если мы переключились на NO_SOURCE с DEVICE, то прекратить получать статус
        if(Objects.equals(liveCurrentSource.getValue(), DataSource.DEVICE)) {
            stopGettingStatusAtFixedRate();
        }
        liveCurrentSource.postValue(DataSource.NO_SOURCE);
        return true;
    }

    /** Запускает опрос состяния устройства по таймеру */
    public void startGettingStatusAtFixedRate() {
        if(statusFuture == null || statusFuture.isDone()) {
            statusCounter=0;
            // раз в 2 секунды запрашивать статус устройства
            statusFuture = executor.scheduleAtFixedRate(
                    statusGetter, 0, 2000, TimeUnit.MILLISECONDS);
        }
    }

    /** Останавливает опрос состяния устройства по таймеру */
    public void stopGettingStatusAtFixedRate() {
        if(statusFuture == null || statusFuture.isDone())
            return;
        statusFuture.cancel(true);
    }

    /** Класс для выполнения операций считывания статуса устройства по таймеру (между зондированиями)
     * Отправляются команды, считываются данные, обновляются поля в списке статуса устройства.
     */
    class StatusGetter implements Runnable {
        @Override
        public void run() {
            if(Objects.equals(liveCurrentSource.getValue(), DataSource.DEVICE)) {
                if (!device.getStatus())
                    logger.add(this, "device.getStatus() return FALSE");
                else
                    liveStatusCounter.postValue(statusCounter++);
            }
            else
                logger.add(this, "Trying to get device status, but CurrentSource!=DEVICE");
        }
    }

    /** Класс для выполнения всех необходимых операций для сбора данных из выбранного источника
     * {@link #liveCurrentSource}. Если в качестве источника данных выбрано устройство, то в этом
     * же классе помимо сбора данных может выполняться команда получения статуса устройства или
     * команда передачи новых параметров устройства.
     */
    class DataReading implements Runnable {

        @Override
        public void run() {
            if(liveCurrentSource.getValue().equals(DataSource.FILE)) {
                RadarBox.fileRead.data.getNextFrame(RadarBox.freqSignals.getRawFreqFrame());
            }
            else if(liveCurrentSource.getValue().equals(DataSource.DEVICE)) {
                if(frameCounter==0) {
                    if(!device.setConfiguration()) {
                        logger.add(this,"Device.setConfiguration() returned " +
                                "false on start. Data timer stopped.");
                        RadarBox.dataThreadService.stop();
                    }
                }
                RadarBox.device.getNewFrame(RadarBox.freqSignals.getRawFreqFrame());
                // в заголовке данных содержится часть статуса, поэтому инкрементируем statusCounter
                liveStatusCounter.postValue(statusCounter++);
            }

            try {
                barrier.await();
            }
            catch (BrokenBarrierException bbe) {
                RadarBox.logger.add(this,"barrier is broken " + bbe.getLocalizedMessage());
            }
            catch (InterruptedException ie) {
                RadarBox.logger.add(this,"thread interrupted " + ie.getLocalizedMessage());
            }
        }
    }

    /** Класс для выполнения цифровой обработки сигналов в отдельном потоке */
    class SignalProcessing implements Runnable {
        @Override
        public void run() {

            // дальнейшая обработка сигналов
            if(frameCounter>0)
                RadarBox.freqSignals.doSignalProcessing();
            try {
                barrier.await();
            }
            catch (BrokenBarrierException bbe) {
                RadarBox.logger.add(this,"barrier is broken "+bbe.getLocalizedMessage());
            }
            catch (InterruptedException ie) {
                RadarBox.logger.add(this,"thread interrupted "+ie.getLocalizedMessage());
            }
        }
    }

    /** Класс для сохранения данных в файл в отдельном потоке */
    class DataSaving implements Runnable {
        @Override
        public void run() {
            if (AoRDSettingsManager.needSaveData) {
                if (frameCounter == 0) {
                    if (RadarBox.fileWrite != null) {
                        RadarBox.fileWrite.close();
                    }
                    RadarBox.setAoRDFile(RadarBox.fileWrite,
                            AoRDSettingsManager.createNewAoRDFile());
                    if (RadarBox.fileWrite == null) {
                        RadarBox.logger.add("File to write is null");
                    }
                    RadarBox.fileWrite.data.startWriting();
                }
                else  {
                    RadarBox.fileWrite.data.write(RadarBox.freqSignals.getRawFreqFrame());
                    //TODO запись строки статуса в архив данных
                    if(frameCounter==0) { // если кадр нулевой, значит запись только началась
                        // создание ЗАГОЛОВКА таблицы из полей ID статусных показателей
                        device.status.getStatusList().forEach(statusEntry -> {
                            statusEntry.getID();
                        });
                    }
                    // номер кадра - frameCounter
                    getLiveFrameCounter();
                    // время, прошедшее с начала зондирования - fullScanningTime
                    getFullScanningTime();
                    // время, прошедшее между кадрами - lastFrameTimeInterval
                    getLastFrameTimeInterval();

                    // запись значений статусных показателей
                    device.status.getStatusList().forEach(statusEntry -> {
                        statusEntry.getValue().toString();
                    });
                    // либо так
                    for(int i=0; i<device.status.getStatusList().size(); i++) {
                        device.status.getStatusList().get(i).getID();
                        device.status.getStatusList().get(i).getName();
                        device.status.getStatusList().get(i).getValue().toString();
                    }
                }
            }

            try {
                barrier.await();
            }
            catch (BrokenBarrierException bbe) {
                RadarBox.logger.add(this,"barrier is broken " + bbe.getLocalizedMessage());
                if (AoRDSettingsManager.needSaveData) {
                    RadarBox.fileWrite.data.endWriting();
                    RadarBox.fileWrite.commit();
                }
            }
            catch (InterruptedException ie) {
                RadarBox.logger.add(this,"thread interrupted " + ie.getLocalizedMessage());
                if (AoRDSettingsManager.needSaveData) {
                    RadarBox.fileWrite.data.endWriting();
                    RadarBox.fileWrite.commit();
                }
            }
        }
    }

    /** Класс, выполняющий действия по завершению всех операций по сбору, обработки и записи данных
     */
    class UiUpdater implements Runnable {
        @Override
        public void run() {
            frameCounter++;
            // Вычисление полного времени сканирования и интервала между последними зондированиями
            long tCurrent = System.currentTimeMillis();
            lastFrameTimeInterval = tCurrent-tStart;
            fullScanningTime+=lastFrameTimeInterval;
            tStart = tCurrent;
            /* Увеличение liveData счётчика кадров.
                Запускаются все зарегистрированные изменения интерфейса.*/
            liveFrameCounter.postValue(frameCounter);
        }
    }
}
