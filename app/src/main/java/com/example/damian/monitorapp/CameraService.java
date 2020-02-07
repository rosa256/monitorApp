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
import android.os.CountDownTimer;
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
import com.example.damian.monitorapp.Utils.CustomCountDownTimer;
import com.example.damian.monitorapp.Utils.FileManager;
import com.example.damian.monitorapp.Utils.ImageSaver;
import com.example.damian.monitorapp.requester.RekognitionRequester;


import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


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
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private int cameraFacing;
    private CameraDevice cameraDevice;

    private ImageReader imageReader;

    static final int DEFAULT_DELAY = 60000;
    int pictureDelay;
    int pictureDelaySaved;

    private boolean cameraClosed;

    private ClientAWSFactory clientAWSFactory = new ClientAWSFactory();
    //private final IBinder binder = new LocalBinder();
    private BroadcastReceiver launchReceiver;

    private PowerManager.WakeLock wakeLock;
    private CustomCountDownTimer countDownTimer = null;


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

    //TODO: BĘDĘ MUSIAŁ ZROBIĆ SPRAWDZANIE TEGO PRZYPADKU: SERWIS DZIAŁA W TLE, WLACZAM APKE, I NIE LOGUJE MNIE... A sweris dalej dziala?? xD
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

        startForeground(ONGOING_NOTIFICATION_ID, getNotification(""));

    }

    public void initCustomTimer() {
        Log.d(TAG, "initCustomTimer(): Invoked");
        countDownTimer = new CustomCountDownTimer(DEFAULT_DELAY, 1000, this);
        countDownTimer.start();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if(countDownTimer != null) {
            countDownTimer.cancel();
        }
        stopService();
    }

    public void stopService(){
        stopSelf();
        closeBackgroundThread();
        unregisterReceiver(launchReceiver);
        Log.d(TAG, "stopService: Stopping service");
    }

    private void lockCPU() {
        Log.i(TAG, "lockCPU(): Invoked");

        // (START)Edited to work without charging
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager == null) {
            Log.i(TAG, "lockCPU(): Failed to lock CPU.");
            return;

        }
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::MyWakelockTag");
        wakeLock.acquire();
        // (END)Edited to work without charging
        Log.i(TAG, "lockCPU(): success");
    }

    private void unlockCPU() {
        Log.i(TAG, "unlockCPU(): Invoked");
        // (START)Edited to work without charging
        if (wakeLock.isHeld() && wakeLock != null) {
            Log.i(TAG, "unlockCPU(): Release LOCK");
            wakeLock.release();
        }
        // (END)Edited to work without charging
    }

    public void start(){

        // Initialize view drawn over other apps
        Log.d(TAG, "start(): Run Service with NO PREVIEW.");

        setUpCamera();
        isInternetConnection();
        initCustomTimer();

    }

    private Notification getNotification(String timeStatus){
        Intent notificationIntent = new Intent(CameraService.this, context.getClass());
        PendingIntent pendingIntent = PendingIntent.getActivity(context,0, notificationIntent, 0);

        //ONLY FOR API 26 and HIGHER!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getText(R.string.app_name))
                .setContentText("Picture in: "+timeStatus)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentIntent(pendingIntent)
                .setTicker(getText(R.string.app_name))
                .build();

        return notification;
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
            unlockCPU();
            reopenCameraPreview();
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
            new RekognitionRequester().doAwsService(rekognitionClient, FileManager.getInstance().getCurrentTakenPhotoFile(), Constants.AWS_COMPARE_FACES, CameraService.this);

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

    public void isInternetConnection(){

        //(START) CHECK INTERNET CONNECTION
        ConnectivityManager connectivityManager =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isInternetConnection = connectivityManager.getActiveNetworkInfo() != null &&
                connectivityManager.getActiveNetworkInfo().isConnected();
        if(!isInternetConnection){
            Log.i(TAG, "isInternetConnection(): No Internet Connection");
            System.out.println("NO INTERNET CONNECTION!");
            Toast.makeText(context, "No internet connection!", Toast.LENGTH_LONG).show();
        }
    }

    public void updateNotification(String timer) {

        Notification newNotification = getNotification(timer +" secounds.");
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(ONGOING_NOTIFICATION_ID, newNotification);
    }

    public void savePicture(){
        Log.i(TAG, "savePicture(): Invoked");
        savePictureNow();
    }

    public void savePictureNow(){
        lockCPU();
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

    private class LaunchBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_USER_PRESENT.equals(action)) {
                Log.d(TAG, "PRESENT!");
                resumeTimer();
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                Log.d(TAG, "SCREEN ON!");
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                Log.d(TAG, "SCREEN OFF!");
                countDownTimer.pauseTimer();
            }
        }
    }

    private void resumeTimer() {
        Log.i(TAG, "resumeTimer(): Time Left:" + readLeftTimeSharedPref());
        countDownTimer = new CustomCountDownTimer(readLeftTimeSharedPref(), 1000, CameraService.this);
        countDownTimer.start();
    }

    private void reopenCameraPreview() {
        System.out.println("SENDING TO PREVIEW");
        Intent reopenPreviewIntent = new Intent();
        reopenPreviewIntent.setAction("com.example.damian.monitorApp.REOPEN_PREVIEW");
        sendBroadcast(reopenPreviewIntent);
    }

    private void readDelayPreference() {
        // reads picture delay from preferences, updates this.pictureDelay and delay button text
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int delay = prefs.getInt(DELAY_PREFERENCES_KEY, DEFAULT_DELAY);
      //  if (!DELAY_DURATIONS.contains(delay)) {
      //      delay = DEFAULT_DELAY;
      //  }
        this.pictureDelay = delay;
    }

    private void writeServiceStateSharedPref(boolean state) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(SERVICE_STATE_KEY, state);
        editor.commit();
    }

    private long readLeftTimeSharedPref() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        long delay = prefs.getLong(SERVICE_PICTURE_DELAY_SAVED, DEFAULT_DELAY);
        Log.i(TAG, "readLeftTimeSharedPref(): Delay saved: " + delay);
        return delay;
    }
}
