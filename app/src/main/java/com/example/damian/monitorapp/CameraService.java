package com.example.damian.monitorapp;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.widget.Toast;

import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.example.damian.monitorapp.Utils.ClientAWSFactory;
import com.example.damian.monitorapp.Utils.Constants;
import com.example.damian.monitorapp.Utils.FileManager;
import com.example.damian.monitorapp.Utils.ImageSaver;
import com.example.damian.monitorapp.fragments.CameraPreviewFragment;
import com.example.damian.monitorapp.requester.RekognitionRequester;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class CameraService extends Service {
    Context context = this;

    private static final String TAG = "CameraService";

    public static final String ACTION_START = "com.example.damian.monitorApp.START";
    public static final String ACTION_START_WITH_PREVIEW = "com.example.damian.monitorApp.START_WITH_PREVIEW";
    public static final String ACTION_STOP = "com.example.damian.monitorApp.STOP";

    private static final Integer ONGOING_NOTIFICATION_ID = 6660;
    private static final String CHANNEL_ID = "cam_service_channel_id";
    private static final String CHANNEL_NAME = "cam_service_channel_name";
    private static final String DELAY_PREFERENCES_KEY = "delay";
    private static final String SERVICE_STATE_KEY = "service_state";
    private static final String SERVICE_PICTURE_DELAY_SAVED = "picture_delay_saved";


    String tagLock = "com.my_app:LOCK";

    private CameraManager cameraManager;
    private String cameraId;
    private Size previewSize;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private int cameraFacing;
    private CameraDevice cameraDevice;


    private PowerManager.WakeLock pmWakeLock;

    private ImageReader imageReader;

    private boolean isON = true;
    int currentPictureID = 0;
    int pictureTimer = 0;
    ScheduledExecutorService executor;
    Handler handler;
    static final int DEFAULT_DELAY = 60;
    int pictureDelay;
    int pictureDelaySaved;

    private boolean cameraClosed;

    private PowerManager.WakeLock mWakeLock;
    private final Object mLock = new Object();

    private ClientAWSFactory clientAWSFactory = new ClientAWSFactory();
    //private final IBinder binder = new LocalBinder();
    private BroadcastReceiver launchReceiver;


    /*
    * Aby serwis działał w tle, muszę wyłączyć na Huwaweiu w Batery -> Launch Ap -> monitorApp
    * */

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private AmazonRekognitionClient rekognitionClient;

    //TODO: BĘDĘ MUSIAŁ ZROBIĆ SPRAWDZANIE TEGO TYPU: SERWIS DZIAŁA W TLE, WLACZAM APKE, I NIE LOGUJE MNIE... A sweris dalej dziala?? xD
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        Log.i(TAG, "onStartCommand(): action = " + action);
        switch (action) {
            case ACTION_START:
                start();
                writeServiceStateSharedPref(true);
                break;
            case ACTION_STOP:
                writeServiceStateSharedPref(false);
                Log.i(TAG, "Received Stop Foreground Intent");
                stopForeground(true);
                stopSelf();
                break;
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        cameraFacing = CameraCharacteristics.LENS_FACING_FRONT;
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        handler = new Handler();
        openBackgroundThread();
        rekognitionClient = (AmazonRekognitionClient) clientAWSFactory.createRekognitionClient(getApplicationContext());
        readDelayPreference();
        pictureDelaySaved = pictureDelay;

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        launchReceiver = new LaunchBroadcastReceiver();
        registerReceiver(launchReceiver, filter);

        startWithForeground();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService();
    }

    public void stopService(){
        stopSelf();
        closeBackgroundThread();
        unlockCPU();

        if(executor != null && handler != null) {
            handler.removeCallbacksAndMessages(null);
            executor.shutdownNow();
        }

        unregisterReceiver(launchReceiver);
        Log.d(TAG, "stopService: Stopping service");
    }

    private void lockCPU() {
        PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (mgr == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            Build.MANUFACTURER.equals("Huawei")) {
            tagLock = "com.example.damian.monitorApp:LocationManagerService";
            Log.d(TAG, "Device is Huawei manufacturer");
        }
        pmWakeLock = mgr.newWakeLock(1, tagLock);
        pmWakeLock.acquire();
        Log.d(TAG, "CameraService lockCPU()");
    }

    private void unlockCPU() {
        if (pmWakeLock != null && pmWakeLock.isHeld()) {
            pmWakeLock.release();
            pmWakeLock = null;
            Log.d(TAG, "CameraService unlockCPU()");
        }
    }

    public void start(){

        // Initialize view drawn over other apps
        Log.d(TAG, "start: Run Service with NO PREVIEW.");
        lockCPU();
        setUpCamera();

        runApp();

    }

    private void startWithForeground(){
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
                .setSmallIcon(R.drawable.ic_bell)
                .setContentIntent(pendingIntent)
                .setTicker(getText(R.string.app_name))
                .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    private void setUpCamera() {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == cameraFacing) {
                    this.cameraId = cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, stateCallback, null);
            }
        } catch (CameraAccessException e) {
            Log.i(TAG,":MissingPermission - CAMERA");
            e.printStackTrace();
        }
    }

    private final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
                Log.i(TAG, "done taking picture from camera " + cameraDevice.getId());
            closeCamera();
            reopenCameraPreview();
            if(pictureDelay != pictureDelaySaved) {
                pictureDelaySaved = pictureDelay;
                executor.shutdown();
                executor = Executors.newSingleThreadScheduledExecutor();
                executor.scheduleAtFixedRate(periodicTask, 0, pictureDelay + 1, TimeUnit.SECONDS);
            }
        }
    };

    public void takePhoto() throws CameraAccessException {
        if (null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        final CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraDevice.getId());
        Size[] jpegSizes = null;
        StreamConfigurationMap streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (streamConfigurationMap != null) {
            jpegSizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
        }
        final boolean jpegSizesNotEmpty = jpegSizes != null && 0 < jpegSizes.length;
        int width = jpegSizesNotEmpty ? jpegSizes[0].getWidth() : 640;
        int height = jpegSizesNotEmpty ? jpegSizes[0].getHeight() : 480;
        final ImageReader reader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 2);
        final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        captureBuilder.addTarget(reader.getSurface());
        captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(2)/*getOrientation()*/);
        reader.setOnImageAvailableListener(onImageAvailableListener, null);
        cameraDevice.createCaptureSession(Arrays.asList(reader.getSurface()), new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        try {
                            session.capture(captureBuilder.build(), captureListener, null);
                        } catch (final CameraAccessException e) {
                            Log.e(TAG, " exception occurred while accessing " + cameraDevice.getId(), e);
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        Log.d(TAG, "onConfigureFailed: Faild to create cameraCaptureSession");
                    }
                },null);
    }

    ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "onImageAvailableListener: Image Available to save.");

            final Image image = reader.acquireNextImage();
            new ImageSaver(image);
            //new RekognitionRequester().doAwsService(rekognitionClient, FileManager.getInstance().getCurrentTakenPhotoFile(), Constants.AWS_COMPARE_FACES, CameraService.this);

        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraClosed = false;
            cameraDevice = camera;
            Log.d(TAG, "onOpened: Camera " + camera.getId() + " is opened.");
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    try {
                        takePhoto();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, "onDisconnected: Camera " + camera.getId() + " is Disconnected");
            if (cameraDevice != null && !cameraClosed) {
                cameraClosed = true;
                cameraDevice.close();
            }
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            Log.e(TAG, "onError: Camera "+ camera.getId() +" has error, code: " + error);
            if (cameraDevice != null && !cameraClosed)
            cameraDevice.close();
            CameraService.this.cameraDevice = null;
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            cameraClosed = true;
        }
    };

    private void openBackgroundThread() {
        backgroundThread = new HandlerThread("camera_background_thread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void closeBackgroundThread() {
        if (backgroundHandler != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    public void runApp(){
        if (isON) { //ON

            //(START) CHECK INTERNET CONNECTION
            ConnectivityManager connectivityManager =
                    (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            boolean isInternetConnection = connectivityManager.getActiveNetworkInfo() != null &&
                    connectivityManager.getActiveNetworkInfo().isConnected();
            if(!isInternetConnection){
                Log.i(TAG, "runApp: No Internet Connection");
                System.out.println("NO INTERNET CONNECTION!");
                Toast.makeText(context, "No internet connection!", Toast.LENGTH_LONG).show();
                return;
            }
            //(END) CHECK INTERNET CONNECTION
            isON = false;
            currentPictureID = 0;
            synchronized (mLock) {
                executor = Executors.newSingleThreadScheduledExecutor();
                executor.scheduleAtFixedRate(periodicTask, 0, pictureDelay +1, TimeUnit.SECONDS);
            }
        } else { //OFF
            isON = true;
            executor.shutdownNow();

            //Operations to end counting proccess.
            currentPictureID++;
            decrementTimer(-1);
            //Stoping handler postDelayed.
            handler.removeCallbacksAndMessages(null);
        }
    }

    public void decrementTimer(final int pictureID){
        if (pictureID != this.currentPictureID) {
            return;
        }
        boolean takePicture = (pictureTimer == 1);
        --pictureTimer;
        sendDataToActivity();
        if (takePicture) {
            savePictureNow();
            //playTimerBeep();
        } else if (pictureTimer > 0) {

            System.out.println("ODLICZAM: " + pictureTimer);
            handler.postDelayed(makeDecrementTimerFunction(pictureID), 1000);
        }
    }


    Runnable periodicTask = new Runnable() {
        public void run() {
            // Invoke method(s) to do the work
                savePicture();
        }
    };

    public void savePicture(){
        if (this.pictureDelay == 0) {
            savePictureNow();
        } else if(pictureDelay != pictureDelaySaved) {
            savePictureAfterDelay(this.pictureDelaySaved);
            Log.i(TAG, "savePicture(): with pictureDelaySaved: " + pictureDelaySaved);
        } else {
            savePictureAfterDelay(this.pictureDelay);
            Log.i(TAG, "savePicture(): with pictureDelay: " + pictureDelay);
        }
    }

    void savePictureAfterDelay(int delay) {

        pictureTimer = delay;
        //updateTimerMessage(false);
        currentPictureID++;
        handler.postDelayed(makeDecrementTimerFunction(currentPictureID), 1000);
    }

    Runnable makeDecrementTimerFunction(final int pictureID) {
        return new Runnable() {
            public void run() {
                    decrementTimer(pictureID);
            }
        };
    }

    public void savePictureNow(){
        openCamera();
    }

    private void closeCamera() {
        Log.d(TAG, "closing camera " + cameraDevice.getId());
        if (null != cameraDevice && !cameraClosed) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height){
        List<Size> bigEnough = new ArrayList<Size>();
        for(Size option: choices){
            if(option.getHeight() == option.getWidth() * height/ width &&
                    option.getWidth() >= width && option.getHeight() >= height){
                bigEnough.add(option);
            }
        }
        if(bigEnough.size() > 0){
            return Collections.min(bigEnough, new CameraPreviewFragment.CompareSizeByArea());
        } else{
            return choices[0];
        }
    }

    private class LaunchBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_USER_PRESENT.equals(action)) {
                Log.d(TAG, "PRESENT!");
                pictureDelaySaved = readServicePicutreDelaySharedPref();
                executor = Executors.newSingleThreadScheduledExecutor();
                executor.scheduleAtFixedRate(periodicTask, 0, pictureDelaySaved + 1, TimeUnit.SECONDS);
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                Log.d(TAG, "SCREEN ON!");

            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                Log.d(TAG, "SCREEN OFF!");
                handler.removeCallbacksAndMessages(null);
                executor.shutdown();
                writeServicePicutreDelaySharedPref(pictureTimer);

            }
        }

        private void stopApp() {

            if(executor != null && handler != null) {
                //handler.removeCallbacksAndMessages(null);
                executor.shutdownNow();
                Log.d(TAG, "stopApp(): Stoping countering");
            }
            //isON = false;
        }
    }

    private void sendDataToActivity()
    {
        System.out.println("SENDING TIMEE");
        Intent sendLevel = new Intent();
        sendLevel.setAction("com.example.damian.monitorApp.GET_TIME");
        sendLevel.putExtra( "LEVEL_TIME", pictureTimer);
        sendBroadcast(sendLevel);
    }

    private void reopenCameraPreview()
    {
        System.out.println("SENDING TO PREVIEW");
        Intent reopenPreviewIntent = new Intent();
        reopenPreviewIntent.setAction("com.example.damian.monitorApp.REOPEN_PREVIEW");
        sendBroadcast(reopenPreviewIntent);
    }

    void readDelayPreference() {
        // reads picture delay from preferences, updates this.pictureDelay and delay button text
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int delay = prefs.getInt(DELAY_PREFERENCES_KEY, DEFAULT_DELAY);
      //  if (!DELAY_DURATIONS.contains(delay)) {
      //      delay = DEFAULT_DELAY;
      //  }
        this.pictureDelay = delay;
    }

    void writeServiceStateSharedPref(boolean state) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(SERVICE_STATE_KEY, state);
        editor.commit();
    }

    int readServicePicutreDelaySharedPref() {
        // reads picture delay from preferences, updates this.pictureDelay and delay button text
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int delay = prefs.getInt(SERVICE_PICTURE_DELAY_SAVED, DEFAULT_DELAY);
        Log.i(TAG, "readServicePicutreDelaySharedPref(): Delay saved: " + delay);
        //  if (!DELAY_DURATIONS.contains(delay)) {
        //      delay = DEFAULT_DELAY;
        //  }
        return delay;
    }



    void writeServicePicutreDelaySharedPref(int pictureTimer) {
        Log.i(TAG, "writeServicePicutreDelaySharedPref(): pictureTimer to save: " + pictureTimer);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(SERVICE_PICTURE_DELAY_SAVED, pictureTimer);
        editor.commit();
    }

}
