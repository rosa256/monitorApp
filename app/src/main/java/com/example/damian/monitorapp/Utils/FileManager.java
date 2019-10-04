package com.example.damian.monitorapp.Utils;

import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;

import com.amazonaws.auth.policy.Resource;
import com.example.damian.monitorapp.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileManager {

    private static final String TAG = "FileManager";

    private File galleryFolder;

    private Resources resources;
    public FileManager(Resources resources) {
        this.resources = resources;
    }

    private File createImageFile(File galleryFolder) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "image_" + timeStamp;
        String sufix = ".jpg";
        Log.i(TAG, "createdImageFile:" + galleryFolder + "/" + imageFileName + sufix);
        return File.createTempFile(imageFileName, sufix, galleryFolder);
    }

    private void createImageGallery() {
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        galleryFolder = new File(storageDirectory, resources.getString(R.string.app_name));
        if (!galleryFolder.exists()) {
            boolean wasCreated = galleryFolder.mkdirs();
            if (!wasCreated) {
                Log.e("CapturedImages", "Failed to create directory");
            }
        }
    }


}
