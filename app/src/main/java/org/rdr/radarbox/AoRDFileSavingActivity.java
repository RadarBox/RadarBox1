package org.rdr.radarbox;

import android.os.Bundle;
import android.app.Activity;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import android.net.Uri;
import android.content.Intent;

import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ListView;
import android.widget.ArrayAdapter;

import org.rdr.radarbox.File.AoRDFile;
import org.rdr.radarbox.File.AoRDSender;

/**
 * Активность для сохранения AoRD-файла.
 */
public class AoRDFileSavingActivity extends AppCompatActivity {
    private static boolean sendFile = false;

    private static final int CHOOSE_FILE_REQUEST_CODE = 1;

    // Life cycle methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aordfile_saving);

        TextView aordFileNameView = findViewById(R.id.aord_saving_file_name_textview);
        aordFileNameView.setText(RadarBox.fileWrite.getName());

        updateFilesList();
    }

    // Set methods
    /**
     * Задаёт классу флаг отправки файла (если true, после сохранения создаётся диалог
     * {@link AoRDSender#createDialogToSendAoRDFile(Activity, AoRDFile)}.
     * @param value - новое значение.
     */
    public static void setSendFile(boolean value) {
        sendFile = value;
    }

    // Handle methods
    public void onClickAddFile(View view) {
        Intent filePickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
        filePickerIntent.setType("*/*");
        filePickerIntent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(filePickerIntent, CHOOSE_FILE_REQUEST_CODE);
    }

    public void onClickDeleteFile(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.elements_deletion));

        Spinner delFileChooser = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, RadarBox.fileWrite.additional.getNamesList());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        delFileChooser.setAdapter(adapter);
        builder.setView(delFileChooser);

        builder.setPositiveButton(getString(R.string.str_delete),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Object item = delFileChooser.getSelectedItem();
                        if (item != null) {
                            String name = item.toString();
                            RadarBox.fileWrite.additional.deleteFile(name);
                            updateFilesList();
                            RadarBox.logger.add(this,
                                    "End of deleting file " +
                                            RadarBox.fileWrite.additional.getFolder().getPath() + "/"
                                            + name);
                        }
                    }
                });
        builder.setNegativeButton(getString(R.string.str_close),
                (dialog, which) -> { });
        builder.create();
        builder.show();
    }

    public void onClickSaveAoRDFile(View view) {
        final EditText textEditor = findViewById(R.id.aord_description_edit);
        RadarBox.fileWrite.description.write(textEditor.getText().toString());
        RadarBox.fileWrite.commit();
        if (RadarBox.fileWrite.isEnabled()) {
            if (sendFile) {
                AoRDSender.createDialogToSendAoRDFile(this, RadarBox.fileWrite);
                return;
            }
        }
        finish();
    }

    public void onClickExit(View view) {
        abortSaving();
    }

    @Override
    public void onBackPressed() {}

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case CHOOSE_FILE_REQUEST_CODE: {
                if (resultCode == RESULT_OK) {
                    Uri fileUri = data.getData();
                    RadarBox.fileWrite.additional.addFile(fileUri, this);
                    updateFilesList();
                }
                break;
            }
        }
    }

    // Help methods
    /**
     * Обновление списка дополнительных файлов.
     */
    private void updateFilesList() {
        ListView filesListView = findViewById(R.id.list_of_additional_files);
        String[] namesList = RadarBox.fileWrite.additional.getNamesList();
        if (namesList.length == 0) {
            namesList = new String[] {getString(R.string.str_container_empty)};
        }
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, namesList);
        filesListView.setAdapter(listAdapter);
        filesListView.setSelection(listAdapter.getCount() - 1);
    }

    private void abortSaving() {
        AoRDFile aordFile = RadarBox.fileWrite;
        RadarBox.setAoRDFile(RadarBox.fileWrite, null);
        if (!aordFile.delete()) {
            RadarBox.logger.add("ERROR: Can`t delete file " +
                    aordFile.getAbsolutePath());
        }
        finish();
    }
}
