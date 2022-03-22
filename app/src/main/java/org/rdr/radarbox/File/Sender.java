package org.rdr.radarbox.File;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import org.rdr.radarbox.BuildConfig;
import org.rdr.radarbox.RadarBox;

import java.io.File;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.FileProvider;

public class Sender {
    private static String lastExtraText ="";
    public static void createDialogToSendFile(Context context, File file){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if((lastExtraText ==null || lastExtraText.equals("")) && RadarBox.device!=null)
            lastExtraText = "#"+RadarBox.device.getDevicePrefix()+"\n";
        final AppCompatEditText editExtra = new AppCompatEditText(context);
        editExtra.setText(lastExtraText);
        builder.setTitle("Отправить записанные файлы?");
        builder.setMessage(file.getName()+"\nОписание к файлу:")
                .setView(editExtra)
                .setPositiveButton("Отправить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendFileToOtherApplication(context, file, editExtra.getText().toString());
                        lastExtraText = editExtra.getText().toString();
                        RadarBox.logger.add(this,"File "+file.getName()+" have sent");
                    }
                }).setNegativeButton("Закрыть", (dialog, which) -> { }).create().show();
    }

    public static void sendFileToOtherApplication(Context context, File file, String extraText) {
        Intent sendFileIntent = new Intent();
        sendFileIntent.setAction(Intent.ACTION_SEND);
        if(extraText!=null && !extraText.equals(""))
            sendFileIntent.putExtra(Intent.EXTRA_TEXT, extraText);
        sendFileIntent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sendFileIntent.putExtra(Intent.EXTRA_STREAM,
                FileProvider.getUriForFile(context,
                        BuildConfig.APPLICATION_ID+".file_provider",file));
        sendFileIntent.setType("application/pdf");
        context.startActivity(sendFileIntent);

    }
}
