package org.rdr.radarbox.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import org.rdr.radarbox.R;
import org.rdr.radarbox.RadarBox;

import java.io.File;
import java.io.IOException;

public class AoRDFileSaver {
    AoRDFile aordFile;
    File aordFolder;
    Context aordContext;

    public AoRDFileSaver(AoRDFile aordFile_) {
        aordFile = aordFile_;
        aordFile.data.endWriting();
        aordFolder = aordFile.getUnzipFolder();
        aordContext = aordFile.getContext();
    }

    public void createSavingDialog(Activity activityForDialog, boolean sendFile) {
        final Dialog dialog = new Dialog(activityForDialog);
        Window window = dialog.getWindow();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setContentView(R.layout.aord_file_saving_dialog);

        final EditText textEditor = (EditText)dialog.findViewById(R.id.aord_description_edit);

        updateFilesList(activityForDialog, dialog);

        Button ok = (Button)dialog.findViewById(R.id.save_aord_file_button);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                aordFile.description.write(textEditor.getText().toString());
                RadarBox.logger.add(this, "Written to desc file: " +
                        textEditor.getEditableText().toString());
                aordFile.commit();
                if (sendFile && aordFile != null) {
                    Sender.createDialogToSendFile(activityForDialog, aordFile);
                }
                dialog.dismiss();
            }
        });

        Button cancel = (Button)dialog.findViewById(R.id.cancel_saving_aord_file_button);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                dialog.cancel();
            }
        });
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                aordFile.close();
                if (!aordFile.delete()) {
                    RadarBox.logger.add("ERROR: Can`t delete file " +
                            aordFile.getAbsolutePath());
                }
            }
        });
        dialog.show();
    }

    private void updateFilesList(Activity activityForDialog, Dialog dialog) {
        ListView filesListView = dialog.findViewById(R.id.list_of_additional_files);
        String[] namesList = aordFile.additional.getNamesList();
        if (namesList.length == 0) {
            namesList = new String[] {"Ничего нет"};
        }
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(activityForDialog,
                android.R.layout.simple_list_item_1, namesList);
        filesListView.setAdapter(listAdapter);
    }

    private void createAddFileDialog(Activity activityForDialog) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activityForDialog);
        final LayoutInflater inflater = activityForDialog.getLayoutInflater();
        final View mainDialogView = inflater.inflate(R.layout.choose_file_dialog, null);

        builder.setTitle(aordContext.getString(R.string.file_writer_header));
        builder.setMessage(aordFolder.getName() + ".zip\n\n" +
                aordContext.getString(R.string.description_for_file_to_send));
        builder.setView(R.layout.choose_file_dialog);

        builder.setPositiveButton(aordContext.getString(R.string.str_save),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        builder.setNegativeButton(aordContext.getString(R.string.str_close), (dialog, which) -> { });
        builder.create();
        builder.show();
    }
}
