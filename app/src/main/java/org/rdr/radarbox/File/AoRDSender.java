package org.rdr.radarbox.File;

import android.app.Activity;
import android.content.Context;
import android.app.AlertDialog;
import android.content.DialogInterface;

import android.content.Intent;

import androidx.appcompat.widget.AppCompatEditText;

import org.rdr.radarbox.R;
import org.rdr.radarbox.RadarBox;
import org.rdr.radarbox.BuildConfig;

import java.io.File;
import androidx.core.content.FileProvider;

/**
 * Класс для отправки AoRD-файлов.
 * @author Сапронов Данил Игоревич; Шишмарев Ростислав Иванович
 * @version 1.1.0
 */
public class AoRDSender {
    private static String lastSendFileExtraText = "";

    /**
     * Создание диалога для отправки AoRD-файла с функцией добавления сообщения.
     * @param context - контекст.
     * @param file - файл, который нужно отправить.
     */
    public static void createDialogToSendAoRDFile(Context context, AoRDFile file) {
        createDialogToSendAoRDFile(context, file, new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {}
        });
    }

    /**
     * Создание диалога для отправки AoRD-файла с функцией добавления сообщения. Отличие от
     * {@link AoRDSender#createDialogToSendAoRDFile} в том, что при вызове этого метода
     * activity будет завершена ({@link Activity#finish()}).
     * @param activity - текущая активность.
     * @param file - файл, который нужно отправить.
     */
    public static void createDialogToSendAoRDFile(Activity activity, AoRDFile file) {
        createDialogToSendAoRDFile(activity, file, new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                activity.finish();
            }
        });
    }

    private static void createDialogToSendAoRDFile(Context context, AoRDFile file,
                                                   DialogInterface.OnDismissListener closeListener) {
        if ((lastSendFileExtraText == null || lastSendFileExtraText.equals("")) &&
                RadarBox.device != null)
            lastSendFileExtraText = "#" + RadarBox.device.getDevicePrefix() + "\n";
        final AppCompatEditText editExtra = new AppCompatEditText(context);
        editExtra.setText(lastSendFileExtraText);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.file_sender_header));
        builder.setMessage(file.getName() + "\n" + context.getString(
                R.string.description_for_file_to_send) + "\n\n" +
                file.description.getText() + "\n\n" + context.getString(R.string.str_message) + ":");
        builder.setView(editExtra);
        builder.setPositiveButton(context.getString(R.string.str_send),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        lastSendFileExtraText = editExtra.getText().toString();
                        sendFileToOtherApplication(context, file,
                                editExtra.getText().toString());
                        RadarBox.logger.add(this,"File " + file.getName()+" have sent");
                    }
                });
        builder.setNegativeButton(context.getString(R.string.str_close), (dialog, which) -> { });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
            }
        });
        builder.setOnDismissListener(closeListener);
        builder.create();
        builder.show();
    }

    /**
     * Отправка AoRD-файла по сети (с помощью приложения, которое выберет пользователь).
     * @param context - текущая активность.
     * @param file - файл, который нужно отправить.
     * @param extraText - сообщение к файлу.
     */
    public static void sendFileToOtherApplication(Context context, File file, String extraText) {
        Intent sendFileIntent = new Intent();
        sendFileIntent.setAction(Intent.ACTION_SEND);
        if (extraText != null && !extraText.equals(""))
            sendFileIntent.putExtra(Intent.EXTRA_TEXT, extraText);
        sendFileIntent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sendFileIntent.putExtra(Intent.EXTRA_STREAM,
                FileProvider.getUriForFile(context,
                        BuildConfig.APPLICATION_ID + ".file_provider", file));
        sendFileIntent.setType("application/zip");
        context.startActivity(sendFileIntent);
    }
}
