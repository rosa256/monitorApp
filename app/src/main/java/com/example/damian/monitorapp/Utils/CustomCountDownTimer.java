package com.example.damian.monitorapp.Utils;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.damian.monitorapp.CameraService;

public class CustomCountDownTimer extends CountDownTimer {

    private static final String TAG = "CustomCountDownTimer";
    private static final String SERVICE_PICTURE_DELAY_SAVED = "picture_delay_saved";
    static final long DEFAULT_DELAY = 60000;

    private long timeLeftMili = DEFAULT_DELAY;

    private CameraService cameraService;

    public CustomCountDownTimer(long millisInFuture, long countDownInterval, CameraService cameraService) {
        super(millisInFuture, countDownInterval);
        this.cameraService = cameraService;
    }

    @Override
    public void onTick(long millisUntilFinished) {
        timeLeftMili = millisUntilFinished;
        long timeInSecounds = millisUntilFinished / 1000;
        Log.i(TAG, "Countdown seconds remaining: " + timeInSecounds);

        cameraService.updateNotification(String.valueOf(timeInSecounds));
        sendDataToActivity(String.valueOf(timeInSecounds));
    }

    @Override
    public void onFinish() {
        Log.i(TAG, "Timer finished");
        cameraService.savePicture();
        cancel();
        cameraService.initCustomTimer();
    }


    public void sendDataToActivity(String timeInSecounds)
    {
        System.out.println("SENDING TIMEE");
        Intent sendLevel = new Intent();
        sendLevel.setAction("com.example.damian.monitorApp.GET_TIME");
        sendLevel.putExtra( "LEVEL_TIME", timeInSecounds);
        cameraService.sendBroadcast(sendLevel);
    }

    public void pauseTimer(){
        writeTimeLeftSharedPref(timeLeftMili);
        cancel();
    }

    private void writeTimeLeftSharedPref(long pictureTimer) {
        Log.i(TAG, "writeServicePicutreDelaySharedPref(): pictureTimer to save: " + pictureTimer);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(cameraService.getBaseContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(SERVICE_PICTURE_DELAY_SAVED, pictureTimer);
        editor.commit();
    }
}
