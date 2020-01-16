package com.example.damian.monitorapp.Utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ImageSaver{
    public static final String TAG = "ImageSaver";
    private Image image;

    public ImageSaver(Image image) {
        this.image = image;
        saveImage();
    }

    private void saveImage() {
        Long tsLong = System.currentTimeMillis()/1000;
        String timeStampStart = tsLong.toString();

        Log.d(TAG, "saveImage: Start at: "+ timeStampStart);
        FileOutputStream outputPhoto = null;
        if (image == null)
            return;
        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] ImageBytes = new byte[buffer.capacity()];
            buffer.get(ImageBytes);

            final Bitmap bmp = BitmapFactory.decodeByteArray(ImageBytes, 0, ImageBytes.length);

            FileManager fileManager = FileManager.getInstance();
            fileManager.setCurrentTakenPhotoFile(fileManager.createImageFile());

            outputPhoto = new FileOutputStream(fileManager.getCurrentTakenPhotoFile());

            bmp.compress(Bitmap.CompressFormat.PNG, 100, outputPhoto);

            tsLong = System.currentTimeMillis()/1000;
            String timeStampEnd = tsLong.toString();
            Log.d(TAG, "saveImage: End at: "+ timeStampEnd);
            Log.d(TAG, "saveImage: Has taken: "+ (Long.parseLong(timeStampEnd )- Long.parseLong(timeStampStart)));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (image != null) {
                image.close();
            }
            if (outputPhoto != null) {
                try {
                    outputPhoto.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}