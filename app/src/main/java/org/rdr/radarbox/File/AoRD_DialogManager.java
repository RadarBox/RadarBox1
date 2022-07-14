package org.rdr.radarbox.File;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;

import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.os.Environment;
import androidx.core.content.FileProvider;

import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import androidx.appcompat.widget.AppCompatEditText;

import org.rdr.radarbox.R;
import org.rdr.radarbox.RadarBox;
import org.rdr.radarbox.BuildConfig;

import java.io.File;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

public class AoRD_DialogManager {
    private AoRDFile aordFile;

    private final String mainDir = Environment.getExternalStorageDirectory().getAbsolutePath();
    private EditText pathEdit;
    private Spinner chooser;

    private static String lastExtraText = "";

    public AoRD_DialogManager(AoRDFile aordFile_) {
        aordFile = aordFile_;
        aordFile.data.endWriting();
    }

    public void createSavingDialog(Activity activityForDialog, boolean sendFile) {
        final Dialog dialog = new Dialog(activityForDialog);
        Window window = dialog.getWindow();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setContentView(R.layout.aord_file_saving_dialog);

        final EditText textEditor = dialog.findViewById(R.id.aord_description_edit);

        updateFilesList(activityForDialog, dialog);

        Button addFileButton = dialog.findViewById(R.id.add_aord_item_button);
        addFileButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                createAddFileDialog(activityForDialog, dialog);
            }
        });

        Button delFileButton = dialog.findViewById(R.id.delete_aord_item_button);
        delFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createDeleteFileDialog(activityForDialog, dialog);
            }
        });

        Button ok = dialog.findViewById(R.id.save_aord_file_button);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                aordFile.description.write(textEditor.getText().toString());
                RadarBox.logger.add(this, "Written to desc file: " +
                        textEditor.getEditableText().toString());
                aordFile.commit();
                if (sendFile && aordFile != null) {
                    createDialogToSendFile(activityForDialog, aordFile);
                }
                dialog.dismiss();
            }
        });

        Button cancel = dialog.findViewById(R.id.cancel_saving_aord_file_button);
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
        filesListView.setSelection(listAdapter.getCount() - 1);
    }

    private void createAddFileDialog(Activity activityForDialog, Dialog parentDialog) {
        if (!Environment.isExternalStorageManager()) {
            Helpers.requestPermissions(activityForDialog, Helpers.PERMISSION_STORAGE);
        }
        final Dialog dialog = new Dialog(activityForDialog);
        Window window = dialog.getWindow();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setContentView(R.layout.choose_file_dialog);

        pathEdit = dialog.findViewById(R.id.path_edit);
        pathEdit.setText("");
        TextView statusBar = dialog.findViewById(R.id.file_chooser_status_bar);

        Button up = dialog.findViewById(R.id.up_in_explorer_button);
        up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = pathEdit.getText().toString();
                if (path.equals("")) {
                    return;
                }
                String[] splitArray = path.split("/");
                List<String> splitList = Arrays.asList(splitArray);
                path = String.join("/", splitList.subList(0, splitArray.length - 1));
                pathEdit.setText(path);
                updateSpinner(activityForDialog);
            }
        });
        chooser = dialog.findViewById(R.id.files_list_spinner);
        chooser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Object item = chooser.getSelectedItem();
                if (item != null) {
                    String newPath = chooser.getSelectedItem().toString();
                    if (!newPath.equals("")) {
                        pathEdit.setText(pathEdit.getText().toString() + "/" + newPath);
                        updateSpinner(activityForDialog);
                    }
                }
            }
            public void onNothingSelected(AdapterView<?> adView) {

            }
        });
        updateSpinner(activityForDialog);

        Button ok = dialog.findViewById(R.id.choose_file_button);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                File fileToAdd = new File(getCurrentPath());
                if (!fileToAdd.exists()) {
                    statusBar.setText("Файл не существует");
                } else if (!fileToAdd.isFile()) {
                    statusBar.setText("Вы не выбрали файл");
                } else {
                    aordFile.additional.addFile(fileToAdd);
                    updateFilesList(activityForDialog, parentDialog);
                    RadarBox.logger.add(AoRD_DialogManager.this, "End of adding file " +
                            fileToAdd.getAbsolutePath());
                    dialog.dismiss();
                }
            }
        });

        Button cancel = dialog.findViewById(R.id.close_file_chooser_button);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                dialog.cancel();
            }
        });
        dialog.show();
    }

    private String getCurrentPath() {
        String result = mainDir + "/" + pathEdit.getText().toString();
        if (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private List<String> getCurrentFolderList() {
        File curFile = new File(getCurrentPath());
        if (curFile.exists() && curFile.isDirectory()) {
            String[] filesArray = curFile.list();
            if (filesArray == null) {
                return new ArrayList<>();
            }
            Arrays.sort(filesArray);
            ArrayList<String> list = new ArrayList<String>(Arrays.asList(filesArray));
            list.add(0, "");
            return list;
        }
        return new ArrayList<>();
    }

    private void updateSpinner(Activity activityForDialog) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(activityForDialog,
                android.R.layout.simple_spinner_item, getCurrentFolderList());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chooser.setAdapter(adapter);
    }

    private void createDeleteFileDialog(Activity activityForDialog, Dialog parentDialog) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activityForDialog);
        builder.setTitle(activityForDialog.getString(R.string.elements_deletion));

        Spinner delFileChooser = new Spinner(activityForDialog);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(activityForDialog,
                android.R.layout.simple_spinner_item, aordFile.additional.getNamesList());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        delFileChooser.setAdapter(adapter);
        builder.setView(delFileChooser);

        builder.setPositiveButton(activityForDialog.getString(R.string.str_delete),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = delFileChooser.getSelectedItem().toString();
                        aordFile.additional.deleteFile(name);
                        updateFilesList(activityForDialog, parentDialog);
                        RadarBox.logger.add(AoRD_DialogManager.this,
                                "End of deleting file " + aordFile.additional.getFilePath() +
                                        "/" + name);
                    }
                });
        builder.setNegativeButton(activityForDialog.getString(R.string.str_close),
                (dialog, which) -> { });
        builder.create();
        builder.show();
    }

    /**
     * Создание диалога для отправки AoRD-файла с функцией добавления сообщения.
     * @param context - текущая активность.
     * @param file - файл, который нужно отправить.
     */
    public static void createDialogToSendFile(Context context, AoRDFile file) {
        if ((lastExtraText == null || lastExtraText.equals("")) && RadarBox.device!=null)
            lastExtraText = "#" + RadarBox.device.getDevicePrefix() + "\n";
        final AppCompatEditText editExtra = new AppCompatEditText(context);
        editExtra.setText(lastExtraText);

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
