package org.rdr.radarbox.File;

import android.app.AlertDialog;

import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;

import androidx.core.content.FileProvider;
import androidx.appcompat.widget.AppCompatEditText;

import org.rdr.radarbox.R;
import org.rdr.radarbox.RadarBox;
import org.rdr.radarbox.BuildConfig;

import java.io.File;

/**
 * Класс для отправки архивов с данными.
 * @author Сапронов Данил Игоревич
 * @version 1.1.0
 */
public class Sender {
    private static String lastExtraText = "";

    /**
     * Создание диалога для отправки файла с функцией добавления сообщения.
     * @param context - текущая активность.
     * @param file - файл, который нужно отправить.
     */
    public static void createDialogToSendFile(Context context, File file) {
        Reader reader = new Reader(context);
        reader.setFileRead(file.getName());

        if ((lastExtraText == null || lastExtraText.equals("")) && RadarBox.device!=null)
            lastExtraText = "#" + RadarBox.device.getDevicePrefix() + "\n";
        final AppCompatEditText editExtra = new AppCompatEditText(context);
        editExtra.setText(lastExtraText);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.file_sender_header));
        builder.setMessage(file.getName() + "\n" + context.getString(
                R.string.description_for_file_to_send) + "\n\n" + reader.getDescription() +
                "\n\n" + context.getString(R.string.str_message) + ":");
        builder.setView(editExtra);
        builder.setPositiveButton(context.getString(R.string.str_send),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        lastExtraText = editExtra.getText().toString();
                        sendFileToOtherApplication(context, file, editExtra.getText().toString());
                        RadarBox.logger.add(this,"File " + file.getName()+" have sent");
                    }
                });
        builder.setNegativeButton(context.getString(R.string.str_close), (dialog, which) -> { });
        builder.create();
        builder.show();
    }

    /**
     * Отправка файла по сети (с помощью приложения, которое выберет пользователь).
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
        sendFileIntent.setType("application/pdf");
        context.startActivity(sendFileIntent);
    }
}
