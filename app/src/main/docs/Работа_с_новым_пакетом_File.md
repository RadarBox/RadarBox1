## Руководство по использованию нового пакета File
Новый пакет File состоит из классов `AoRDFile` (наследник `java.io.File`), `AoRD_DialogManager`, `AoRDSettingsManager` и нескольких вспомогательных.

Файл для чтения хранится в `RadarBox.fileRead`, для записи в `RadarBox.fileWrite`.

### AoRDFile
AoRDFile -- класс, предназначенный для работы с AoRD-файлами (Archive of Radar Data). Поддерживает чтение, запись и создание.

Важно: **все методы родителя (java.io.File) относятся к самому AoRD-файлу (не к папке, в которую он распакован)**.

#### Структура
За каждый файл в формата AoRD отвечает свой внутренний класс в `AoRDFile`:
|              | Класс                      | Атрибут объекта`*`       |
| ------------ | -------------------------- | ---------------------  |
| Данные       | `DataFileManager`          | `aordFile.data`        |
| Конфигурация | `ConfigurationFileManager` | `aordFile.config`      |
| Статус       | `StatusFileManager`        | `aordFile.status`      |
| Описание     | `DescriptionFileManager`   | `aordFile.description` |
| Дополнения   | `AdditionalFileManager`    | `aordFile.additional`  |

`*` `aordFile` -- объект класса `AoRDFile`.

Они наследуются от `AoRDFile.BaseInnerFileManager` и обладают как общими, так и специализированными методами.

#### Жизненный цикл
1. Инициализация;
2. `aordFile.isEnabled() == true`:
   * ==== Циклически:
   * Чтение/запись;
   * Сохранение изменений (`aordFile.commit()`);
   * ====
   * Обязательное закрытие (`aordFile.close()`).
3. `aordFile.isEnabled() == false`:
   * Желательно заменить на null, никакой пользы такой объект не несёт.

В процессе использования объект может стать нерабочим (`aordFile.isEnabled() == false`) (и уже не нуждается в закрытии). Методы, которые могут к этому привести, помечены #enable_danger в javadoc. Как правило, это исключительные ситуации, и disable объектов в этом случае нужно для удобства отладки и для безопасности.

#### Инициализация
Для создания объекта `AoRDFile` можно воспользоваться одним из конструкторов. В таком случае нужно обязательно проверить его на `isEnabled()` (отсутствие ошибок при инициализации):
```java
AoRDFile aordFile = new AoRdFile(absolutePath);
if (aordFile.isEnabled()) {
    // код
} else {
    aordFile = null;
}
```
Добавляет неудобства и то, что для создания объекта нужен абсолютный путь. Поэтому в большинстве случаев стоит воспользоваться функцией получения готового AoRD-файла (либо enabled, либо сразу null) по имени из папки для их хранения:
```java
AoRDFile aordFile = AoRDSettingsManager.getFileByName(name);
```
Задавать атрибуты `fileRead` и `fileWrite` для `RadarBox` тоже рекомендуется с помощью метода, который сам закрывает старые файлы:
```java
RadarBox.setAoRDFile(RadarBox.fileRead, aordFile1);
RadarBox.setAoRDFile(RadarBox.fileWrite, aordFile2);
```

#### Создание
Существует статический метод `AoRDFile.createNewAoRDFile(absolutePath)`, который создаёт и инициализирует AoRD-файл, но, как и в случае с иницилизацией, удобнее вызвать метод класса `AoRDSettingsManager`, который самостоятельно передаст необходимый путь:
```java
AoRDFile aordFile = AoRDSettingsManager.createNewAoRDFile();
```

#### Чтение
Общие методы:
```java
// Получение объектов класса File
File dataFile = RadarBox.fileRead.data.getFile();
File configFile = RadarBox.fileRead.config.getFile();
// и т. д.
```
Важно: метод `RadarBox.additional.getFile()` вернёт файл **архивированной** папки, а не её саму.

Специализированные методы:
```java
// Data
// Считывание следующего кадра данных в массив
RadarBox.fileRead.data.getNextFrame(shortArray);

// Configuration
// Получение в виде объекта VirtualDeviceConfiguration
DeviceConfiguration config = RadarBox.fileRead.config.getVirtual();

// Description
// Получение текста
String text = RadarBox.fileRead.description.getText();

// Additional
// Получение папки, в которую был распакован архив дополнений
File folder = RadarBox.fileRead.additional.getFolder();
// Получение списка файлов
String[] namesList = RadarBox.fileRead.additional.getNamesList();
File[] filesList = RadarBox.fileRead.additional.getFilesList();
```

#### Запись
Специализированные методы:
```java
// Data
RadarBox.fileWrite.data.startWriting();
RadarBox.fileWrite.data.write(shortArray);
// ...
RadarBox.fileWrite.data.endWriting();

// Configuration
// Автоматически вызовется при создании
RadarBox.fileWrite.config.write(deviceConfiguration);

// Status
// Запись заголовка (автоматически вызовется при создании)
RadarBox.fileWrite.status.writeHeader(deviceStatus);
// Запись строки
RadarBox.fileWrite.status.write(frameNumber, time, deviceStatus);

// Description
RadarBox.fileWrite.description.write(text);

// Additional
RadarBox.fileWrite.additional.addFile(file);
RadarBox.fileWrite.additional.deleteFile(name); // только имя
// Сохранение изменений (автоматически вызывается в aordFile.commit())
RadarBox.fileRead.additional.commit();
```

### AoRDSettingsManager
Кроме функций, упомянутых в разделе AoRDFile, в `AoRDSettingsManager` также хранятся настройки бывшего `Writer`: `needSaveData` и `fileNamePostfix`.
```java
// Get
boolean need = AoRDSettingsManager.isNeedSaveData();
String postfix = AoRDSettingsManager.getFileNamePostfix();

// Set
AoRDSettingsManager.needSaveData = need;
AoRDSettingsManager.setFileNamePostfix(postfix);
```

### AoRDSender
Наследует функции `Sender` с небольшими изменениями.
