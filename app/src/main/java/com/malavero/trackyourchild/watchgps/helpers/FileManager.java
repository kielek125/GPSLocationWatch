package com.malavero.trackyourchild.watchgps.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;

/**
 * Created by Kiełson on 01.05.2018.
 */

public class FileManager {
    public void checkPermissionFile(Activity activity){
        if(ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)){

        }
        else{
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, AppProperty.MEMORY_ACCESS);
        }
    }
    public void saveFile(Activity activity, Context context, String message) {

        //checkPermissionFile(activity);
        //createDir(context);
        //createFile(context, message);
    }
    public void saveFile(Context context, String message) {
        createDir(context);
        createFile(context, message);
    }

    private void createDir(Context context) {
        File folder = new File(AppProperty.path);
        if (!folder.exists())
            try {
                folder.mkdirs();
            } catch (Exception e) {
                Toast.makeText(context, "The catalog has not been created: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
    }

    private void createFile(Context context, String message) {
        File file = new File(AppProperty.path + AppProperty.fileName);
        if (!file.exists())
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

        FileOutputStream fout;
        OutputStreamWriter outWriter;
        try {
            fout = new FileOutputStream(file, true);
            outWriter = new OutputStreamWriter(fout);
            outWriter.append(Calendar.getInstance().getTime().toString() + " --- " + message + System.getProperty("line.separator"));
            outWriter.close();

        } catch (Exception e) {
            Toast.makeText(context, "The file has not been created: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
