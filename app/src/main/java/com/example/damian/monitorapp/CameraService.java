package com.example.damian.monitorapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import butterknife.OnClick;

public class CameraService extends Service {
    Context context = this;

    private static final String TAG = "CameraService";

    static final String ACTION_START = "com.example.damian.monitorApp.START";
    static final String ACTION_START_WITH_PREVIEW = "com.example.damian.monitorApp.START_WITH_PREVIEW";
    static final String ACTION_STOP = "com.example.damian.monitorApp.STOP";

    private static final Integer ONGOING_NOTIFICATION_ID = 6660;
    private static final String CHANNEL_ID = "cam_service_channel_id";
    private static final String CHANNEL_NAME = "cam_service_channel_name";

    private Boolean shouldShowPreview = true;
    private View rootView;
    private TextureView textureView;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        switch (action){
            case ACTION_START:
                start();
                break;
            case ACTION_START_WITH_PREVIEW:
                startWithPreview();
                break;
            case ACTION_STOP:
                stopService();
                break;
        }
        return START_STICKY; /** NIE WIEM O CO Z TYM CHODZI */

    }

    public void stopService(){
        stopSelf();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startWithForeground();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void start(){
        shouldShowPreview = false;
        //initCam(320, 200);
    }

    private void initOverlay(){
        LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rootView = (View) li.inflate(R.layout.overlay, null);
        textureView = (TextureView) rootView.findViewById(R.id.texPreview);

        WindowManager.LayoutParams params;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE /*or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE*/,
                    PixelFormat.TRANSLUCENT);
        }else {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE /*or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE*/,
                    PixelFormat.TRANSLUCENT
            );
        }

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        wm.addView(rootView, params);
    }
    private void startWithPreview(){
        shouldShowPreview = true;

        // Initialize view drawn over other apps
        initOverlay();
        System.out.println(TAG + ": startWithPreview() ---------------");

        //TODO: Ustawienie Kamery
        // Initialize camera here if texture view already initialized
        //textureView.setSurfaceTextureListener(surfaceTextureListener);
        //if (textureView!!.isAvailable)
        //initCam(textureView!!.width, textureView!!.height)
        //else
        //textureView!!.surfaceTextureListener = surfaceTextureListener
    }


    private void startWithForeground(){

        System.out.println(TAG + ": startWithForeground() ---------------");
        Intent notificationIntent = new Intent(CameraService.this, context.getClass());
        PendingIntent pendingIntent = PendingIntent.getActivity(context,0, notificationIntent, 0);

        //ONLY FOR API 26 and HIGHER!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.app_name))
                .setSmallIcon(R.drawable.notification_template_icon_bg)
                .setContentIntent(pendingIntent)
                .setTicker(getText(R.string.app_name))
                .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }
}
