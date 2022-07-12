package org.rdr.radarbox.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
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
        AlertDialog.Builder builder = new AlertDialog.Builder(activityForDialog);
        final LayoutInflater inflater = activityForDialog.getLayoutInflater();
        final View mainDialogView = inflater.inflate(R.layout.aord_file_saving_dialog, null);

        builder.setTitle(aordContext.getString(R.string.file_writer_header));
        builder.setMessage(aordFile.getName());
        builder.setView(R.layout.aord_file_saving_dialog);

        final EditText textEditor = (EditText) mainDialogView.findViewById(R.id.aord_description_edit);

        ListView filesListView = mainDialogView.findViewById(R.id.list_of_additional_files);
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(activityForDialog,
                android.R.layout.simple_list_item_1, aordFile.additional.getNamesList());
        filesListView.setAdapter(listAdapter);

        Button addFilesButton = (Button)mainDialogView.findViewById(R.id.add_aord_item_button);
        addFilesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createAddFileDialog(activityForDialog);
            }
        });

        builder.setPositiveButton(aordContext.getString(R.string.str_save),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        aordFile.description.write(textEditor.getText().toString());
                        RadarBox.logger.add(this, "Written to desc file: " +
                                textEditor.getText().toString());
                        aordFile.commit();
                        if (sendFile && aordFile != null) {
                            Sender.createDialogToSendFile(activityForDialog, aordFile);
                        }
                    }
                });
        builder.setNegativeButton(aordContext.getString(R.string.str_close),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        aordFile.close();
                        if (!aordFile.delete()) {
                            RadarBox.logger.add("ERROR: Can`t delete file " +
                                    aordFile.getAbsolutePath());
                        }
                    }
                });
        builder.create();
        builder.show();
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
