package org.rdr.radarbox.File;

import android.app.Activity;
import android.content.Context;

import android.app.Dialog;
import android.content.DialogInterface;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.rdr.radarbox.R;
import org.rdr.radarbox.RadarBox;
import org.rdr.radarbox.BuildConfig;

import java.io.File;
import androidx.core.content.FileProvider;

/**
 * Класс для отправки AoRD-файлов.
 * @author Шишмарев Ростислав Иванович; Сапронов Данил Игоревич
 * @version 1.2.0
 */
public class AoRDSender {
    private static String lastSendFileExtraText = "";

    /**
     * Создание диалога для отправки AoRD-файла с функцией добавления сообщения.
     * @param context - контекст.
     * @param file - файл, который нужно отправить.
     */
    public static void createDialogToSendAoRDFile(Context context, AoRDFile file) {
        createDialogToSendAoRDFile(context, file,
                new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                    }
                },
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                    }
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
        createDialogToSendAoRDFile(activity, file,
                new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        activity.finish();
                    }
                },
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        activity.finish();
                    }
                });
    }

    private static void createDialogToSendAoRDFile(Context context, AoRDFile aordFile,
                                                   DialogInterface.OnDismissListener dismissListener,
                                                   DialogInterface.OnCancelListener cancelListener) {
        if ((lastSendFileExtraText == null || lastSendFileExtraText.equals("")) &&
                RadarBox.device != null)
            lastSendFileExtraText = "#" + RadarBox.device.getDevicePrefix() + "\n";

        final Dialog dialog = new Dialog(context);
        Window window = dialog.getWindow();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.send_aordfile_dialog);

        TextView aordFileNameView = dialog.findViewById(R.id.send_aordfile_file_name_textview);
        aordFileNameView.setText(aordFile.getName());

        TextView aordFileMessageView = dialog.findViewById(R.id.send_aordfile_description_textview);
        aordFileMessageView.setText(aordFile.description.getText());

        final EditText messageEditor = dialog.findViewById(R.id.send_aordfile_message_edit);
        messageEditor.setText(lastSendFileExtraText);

        Button ok = dialog.findViewById(R.id.send_aordfile_send_button);
        ok.setOnClickListener(new View.OnClickListener() {
                                  public void onClick(View view) {
                                      lastSendFileExtraText = messageEditor.getText().toString();
                                      sendFileToOtherApplication(context, aordFile,
                                              messageEditor.getText().toString());
                                      RadarBox.logger.add(this,"File " +
                                              aordFile.getName() + " have sent");
                                      dialog.dismiss();
                                  }
        });

        Button cancel = dialog.findViewById(R.id.send_aordfile_close_button);
        cancel.setOnClickListener(new View.OnClickListener() {
                                      public void onClick(View view) {
                                          dialog.cancel();
                                      }
                                  });

        dialog.setOnDismissListener(dismissListener);
        dialog.setOnCancelListener(cancelListener);
        dialog.show();
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
