# Инструкция по скачиванию проекта

## Подключение к Android Studio.
1. Регистрируемся на [GitHub](https://github.com);
2. Ведущий проекта добавит Вас в проект;
3. Заходим в Android Studio в появившемся окне выбираем "Get from VCS" (Version Control System);
![get_from_vcs_1.png](https://rostislavshishmarev.pythonanywhere.com/static/get_from_vcs_1.png)
4. Выбираем GitHub (1), затем "Use token..." (2);
![get_from_vcs_2.png](https://rostislavshishmarev.pythonanywhere.com/static/get_from_vcs_2.png)
![get_from_vcs_3.png](https://rostislavshishmarev.pythonanywhere.com/static/get_from_vcs_3.png)
5. Нажимаем "Generate..." (если токена у Вас ещё нет);
6. Откроется страница в браузере, где можно как-то обозвать токен и задать время его действия (например, безграничное - "No expiration").
![generate_token_1.png](https://rostislavshishmarev.pythonanywhere.com/static/generate_token_1.png)
При этом остальные настройки рекомендуем не менять. Нажимаем внизу "Generate token";
7. Полученный токен копируем и вставляем в Android Studio (1), нажимаем "Log in" (2);
![generate_token_2.png](https://rostislavshishmarev.pythonanywhere.com/static/generate_token_2.png)
8. Выбираем из списка репозиториев RadarBox/RadarBox1 (1), выбираем удобное место расположения локальной копии проекта (2), нажимаем "Clone" (3);
![get_from_vcs_4png](https://rostislavshishmarev.pythonanywhere.com/static/get_from_vcs_4.png)
9. Далее – соглашаемся и доверяем проекту.

## Запуск проекта.

1. Запускаем приложение;
2. Включаем проводник:
    * Кнопкой в Android Studio для эмулятора;
![start_project_1png](https://rostislavshishmarev.pythonanywhere.com/static/start_project_1.png)
    * Через приложение для физического устройства;
3. Заходим в папку _storage/emulated/0/Android/data/org.rdr.radarbox/files/Documents_;
![start_project_2.png](https://rostislavshishmarev.pythonanywhere.com/static/start_project_2.png)
4. Добавляем туда файл с данными;
    * При ошибке **PermissionDenied** нужно использовать эмулятор, никак не связанный с Google:
        1. Выбрать устройство без иконки Play Store;
        ![permission_error_1.png](https://rostislavshishmarev.pythonanywhere.com/static/permission_error_1.png)
        2. Выбрать изображение системы, не связанное с Google и AOSP (загрузить при необходимости);
        ![permission_error_2.png](https://rostislavshishmarev.pythonanywhere.com/static/permission_error_2.png)
        3. Завершить создание.
5. Перезагрузить устройство;
6. Выбрать радар (1) и включить чтение из файла (2);

![start_project_3.png](https://rostislavshishmarev.pythonanywhere.com/static/start_project_3.png)

7. Запустить график;

## Создание Fork’a.

Немного о форке и пул реквестах:
> Модель «Fork + Pull» позволяет любому склонировать (fork) существующий репозиторий и сливать изменения в свой личный fork без необходимости иметь доступ к оригинальному репозиторию. Затем изменения должны быть включены в исходный репозиторий его хозяином.

Для того чтобы создать ответвление проекта, зайдите на страницу проекта и нажмите кнопку «Создать ответвление» («Fork»), которая расположена в правом верхнем углу.

![fork_creation_1.png](https://rostislavshishmarev.pythonanywhere.com/static/fork_creation_1.png)

Теперь он будет доступен в Android Studio.

## Внесение изменений. Pull request.

Среда Android Studio не позволяет адекватно делать Pull request изнутри себя, поэтому заходим на страницу вашего fork’a на GitHub и нажимаем кнопку Pull Request.
Далее будет страница, на которой можно задать название и описание внесенных правок.
Затем ведущий проекта одобрит или отклонит Pull request.
