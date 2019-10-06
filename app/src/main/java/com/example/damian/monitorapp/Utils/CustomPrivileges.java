package com.example.damian.monitorapp.Utils;

import android.Manifest;
import android.app.Activity;
import android.support.v4.app.ActivityCompat;

public class CustomPrivileges {
    private static final int REQUEST_WRITE_STORAGE_CAMERA_REQUEST_CODE = 1;

    public static void setUpPrivileges(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[] {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA
                },
                REQUEST_WRITE_STORAGE_CAMERA_REQUEST_CODE);
    }
}
