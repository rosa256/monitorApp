package com.example.damian.monitorapp;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
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
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.damian.monitorapp.Utils.ImageSaver;
import com.example.damian.monitorapp.fragments.CameraPreviewFragment;


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

    String tagLock = "com.my_app:LOCK";

    private Boolean shouldShowPreview = true;
    private View rootView;
    private TextureView textureView;

    private Activity activity;

    private CameraPreviewFragment cameraPreviewFragment;
    private TextureView.SurfaceTextureListener surfaceTextureListener;
    private CameraManager cameraManager;
    private String cameraId;
    private Size previewSize;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private int cameraFacing;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSession;

    private PowerManager.WakeLock pmWakeLock;

    public static final int ACTIVITY_START_CAMERA_APP = 0;
    public static final int STATE_PREVIEW = 0;
    public static final int STATE__WAIT_LOCK = 1;
    private int mState;
    private ImageReader imageReader;

    private boolean isON = true;
    int currentPictureID = 0;
    int pictureTimer = 0;
    ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor();
    Handler handler;
    static final int DEFAULT_DELAY = 5;
    int pictureDelay = DEFAULT_DELAY;

    static final String SERVICE_STATE_KEY = "serviceState";
    private boolean cameraClosed;

    private WifiManager.WifiLock mWiFiLock;
    private PowerManager.WakeLock mWakeLock;
    private final Object mLock = new Object();

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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        Log.i(TAG, "onStartCommand(): action = " + action);


        if (intent!= null) {
            Log.d(TAG, "onStartCommand: *** action = " + action);
            switch (action) {
                case ACTION_START:
                    start();
                    break;
                case ACTION_START_WITH_PREVIEW:
                    startWithPreview();
                    break;
            }
        }
        return START_STICKY; /** NIE WIEM O CO Z TYM CHODZI */
    }

    @Override
    public void onCreate() {
        super.onCreate();
        cameraFacing = CameraCharacteristics.LENS_FACING_FRONT;
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        handler = new Handler();
        cameraPreviewFragment = new CameraPreviewFragment();
        openBackgroundThread();



        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        BroadcastReceiver launchReceiver = new LaunchBroadcastReceiver();
        registerReceiver(launchReceiver, filter);

        surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                setUpCamera(width, height);
                //openCamera();
            }
            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) { }
            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) { return false; }
            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) { }
        };
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
        pmWakeLock.release();

        if(executor != null && handler != null) {
            handler.removeCallbacksAndMessages(null);
            executor.shutdownNow();
        }
        Log.d(TAG, "stopService: Stopping service");
    }

    private void lockCPU() {
        PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (mgr == null) {
            return;
        }

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M &&
                Build.MANUFACTURER.equals("Huawei")) {
            tagLock = "LocationManagerService";
            Log.d(TAG, "Device is Huawei manufacturer");
        }
        pmWakeLock = mgr.newWakeLock(1, tagLock);
        pmWakeLock.acquire();
        Log.d(TAG, "CameraService lockCPU()");
    }

    private void unlockCPU() {
        if (pmWakeLock != null && mWakeLock.isHeld()) {
            pmWakeLock.release();
            pmWakeLock = null;
            Log.d(TAG, "CameraService unlockCPU()");
        }
    }

    private void initOverlay(){
        LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rootView = (View) li.inflate(R.layout.overlay, null);
        textureView = (TextureView) rootView.findViewById(R.id.texPreview);
        cameraPreviewFragment.setTextureView(textureView);

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
    public void start(){
        shouldShowPreview = false;

        // Initialize view drawn over other apps
        Log.d(TAG, "start: Run Service with NO PREVIEW.");
        //TODO:SETUP PREVIEW SIZE FOR START();
        lockCPU();
        setUpCamera(620,860);

        runApp();

        //writeServiceStatePreference(1);
    }

    private void startWithPreview(){
        shouldShowPreview = true;
        Log.d(TAG, "startWithPreview: Run Service with PREVIEW");
        // Initialize view drawn over other apps
        initOverlay();

        //TODO: Ustawienie Kamery
        // Initialize camera here if texture view already initialized
        if (textureView.isAvailable()){
            setUpCamera(textureView.getWidth(), textureView.getHeight());
            openCamera();
            runApp();
        }
        else
        textureView.setSurfaceTextureListener(surfaceTextureListener);
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
                .setSmallIcon(R.drawable.ic_bell)
                .setContentIntent(pendingIntent)
                .setTicker(getText(R.string.app_name))
                .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    private void setUpCamera(int width, int height) {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == cameraFacing) {
                    this.cameraId = cameraId;

                    StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                    previewSize = chooseOptimalSize(streamConfigurationMap.getOutputSizes(SurfaceTexture.class), width, height);
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
        }
    };

    int readServiceStatePreference() {
        // reads picture delay from preferences, updates this.pictureDelay and delay button text
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        return prefs.getInt(SERVICE_STATE_KEY,0);
    }

    void writeServiceStatePreference(int serviceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(SERVICE_STATE_KEY, serviceState);
        editor.commit();
    }


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

    private void createPreviewSession() {
        try {
            //captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            captureRequestBuilder.addTarget(imageReader.getSurface());

            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(2));

            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            if (cameraDevice == null) {
                                return;
                            }
                            try {
                                CaptureRequest captureRequest = captureRequestBuilder.build();
                                CameraService.this.cameraCaptureSession = cameraCaptureSession;
                                //CameraService.this.cameraCaptureSession.capture();
                                CameraService.this.cameraCaptureSession.setRepeatingRequest(captureRequest /*mPreviewCaptureRequest*/,
                                        null , backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            System.out.println("FAILED CAPTURE SESSION");
                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

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
                executor.scheduleAtFixedRate(periodicTask, 0, pictureDelay + 3, TimeUnit.SECONDS);
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
        if (takePicture) {
            savePictureNow();
            //playTimerBeep();
        } else if (pictureTimer > 0) {

            System.out.println("ODLICZAM: "+pictureTimer);
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
        } else {

            savePictureAfterDelay(this.pictureDelay);
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
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
//        int value = prefs.getInt("icrementValue",0);
//        value++;
//        SharedPreferences.Editor editor = prefs.edit();
//        editor.putInt("icrementValue", value);
//        editor.commit();
//        Log.d(TAG, "VAAAAAAAAAAAAAAAALLLLLLLUUUUUEEEEEE: " + value);
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
            if(Intent.ACTION_SCREEN_ON.equals(action)){
                isON = true;
                runApp();
                Log.d(TAG, "onReceive: invoke runApp()");
            } else if(Intent.ACTION_SCREEN_OFF.equals(action)) {
                stopApp();
            }
        }

        private void stopApp() {

            if(executor != null && handler != null) {
                //handler.removeCallbacksAndMessages(null);
                executor.shutdownNow();
                Log.d(TAG, "stopApp(): Stoping countering");
            }
            isON = false;
        }
    }
}










//    private int getJpegOrientation(CameraCharacteristics c, int deviceOrientation) {
//        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return 0;
//        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);
//
//        // Round device orientation to a multiple of 90
//        deviceOrientation = (deviceOrientation + 45) / 90 * 90;
//
//        // Reverse device orientation for front-facing cameras
//        boolean facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
//        if (facingFront) deviceOrientation = -deviceOrientation;
//
//        // Calculate desired JPEG orientation relative to camera orientation to make
//        // the image upright relative to the device orientation
//        int jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360;
//
//        return jpegOrientation;
//    }
