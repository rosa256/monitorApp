package com.example.damian.monitorapp.Utils;

import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;

import com.example.damian.monitorapp.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileManager {
    private static final String TAG = "FileManager";
    private static FileManager fileManager = null;
    private File galleryFolder;
    private File gallerySourceFolder;
    private File sourceFileImage;
    private Resources resources;
    private Boolean init;

    private File currentTakenPhotoFile;

    private FileManager() {
        init = false;
    }

    public static FileManager getInstance(){
        if(fileManager == null) {
            synchronized (FileManager.class) {
                fileManager = new FileManager();
            }
        }
        return fileManager;
    }

    public void initFileManager(Resources resources){
        if (!init){
            this.resources = resources;
            createImageGallery();
            init = true;
        }
    }

    public File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "image_" + timeStamp;
        String sufix = ".jpg";
        Log.i(TAG, "createdImageFile:" + galleryFolder + "/" + imageFileName + sufix);
        return File.createTempFile(imageFileName, sufix, galleryFolder);
    }

    public File createSelectedImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "selected";
        String sufix = ".jpg";
        Log.i(TAG, "createdImageFile:" + galleryFolder + "/" + imageFileName + sufix);
        return File.createTempFile(imageFileName, sufix, galleryFolder);
    }

    public File createSourceImageFile() throws IOException{
        String imageFileName = "sourceImage";
        String sufix = ".jpg";
        Log.i(TAG, "createdImageFile:" + gallerySourceFolder + "/" + imageFileName + sufix);
        sourceFileImage = new File(gallerySourceFolder + "/" + imageFileName + sufix); //Tu może być problem, bo sourceFileImage jest pusty.
        return sourceFileImage;
    }

    public void createImageGallery() {
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if(resources == null){
            Log.i(TAG,"Resources is null.");
            return;
        }

        galleryFolder = new File(storageDirectory, resources.getString(R.string.app_name)+"/targetFolder");
        if (!galleryFolder.exists()) {
            boolean wasCreated = galleryFolder.mkdirs();
            if (!wasCreated) {
                Log.i(TAG, "Failed to create target gallery directory");
            }
        }

        gallerySourceFolder = new File(storageDirectory, resources.getString(R.string.app_name)+"/sourceFolder");
        if (!gallerySourceFolder.exists()) {
            boolean wasCreated = gallerySourceFolder.mkdirs();
            if (!wasCreated) {
                Log.i(TAG, "Failed to create source gallery directory");
            }
        }
    }

    public File getSourceFileImage() {
        return sourceFileImage;
    }
    public File getCurrentTakenPhotoFile() {
        return currentTakenPhotoFile;
    }
    public void setCurrentTakenPhotoFile(File currentTakenPhotoFile) {
        this.currentTakenPhotoFile = currentTakenPhotoFile;
    }
}
