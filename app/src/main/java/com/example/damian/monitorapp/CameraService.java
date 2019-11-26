package com.example.damian.monitorapp;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
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

import com.example.damian.monitorapp.Utils.FileManager;
import com.example.damian.monitorapp.fragments.ActionMenu;
import com.example.damian.monitorapp.fragments.CameraPreviewFragment;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


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

    private CameraPreviewFragment cameraPreviewFragment;
    private TextureView.SurfaceTextureListener surfaceTextureListener;
    private CameraManager cameraManager;
    private String cameraId;
    private Size previewSize;
    private CameraDevice.StateCallback stateCallback;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private int cameraFacing;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSession;

    public static final int ACTIVITY_START_CAMERA_APP = 0;
    public static final int STATE_PREVIEW = 0;
    public static final int STATE__WAIT_LOCK = 1;
    private int mState;
    private ImageReader imageReader;

    private boolean onOff = false;
    int currentPictureID = 0;
    int pictureTimer = 0;
    ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor();
    Handler handler;
    static final int DEFAULT_DELAY = 15;
    int pictureDelay = DEFAULT_DELAY;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    //TODO: ZROBIENIE PODGLADU CAMERY SERWISU NAD INNYMI APLIKACJAMI,
    //TODO: ZROBIENIE PODGLADU CAMERY SERWISU NAD INNYMI APLIKACJAMI,
    //TODO: ZROBIENIE PODGLADU CAMERY SERWISU NAD INNYMI APLIKACJAMI,

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        Log.i(TAG, "onStartCommand(): action = " + action);
        System.out.println("onStartCommand(): action = " + action);
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
        closeBackgroundThread();

    }

    @Override
    public void onCreate() {
        super.onCreate();
        cameraFacing = CameraCharacteristics.LENS_FACING_FRONT;
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        handler = new Handler();
        cameraPreviewFragment = new CameraPreviewFragment();
        openBackgroundThread();
        surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                setUpCamera(width, height);
                openCamera();
            }
            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) { }
            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) { return false; }
            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) { }
        };
        stateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice cameraDevice) {
                CameraService.this.cameraDevice = cameraDevice;
                createPreviewSession();
            }
            @Override
            public void onDisconnected(CameraDevice cameraDevice) {
                cameraDevice.close();
                CameraService.this.cameraDevice = null;
            }
            @Override
            public void onError(CameraDevice cameraDevice, int error) {
                cameraDevice.close();
                CameraService.this.cameraDevice = null;
            }
        };

        startWithForeground();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        closeBackgroundThread();
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
    private void start(){
        shouldShowPreview = false;

        // Initialize view drawn over other apps
        System.out.println(TAG + ": start() ---------------");

        //TODO:SETUP PREVIEW SIZE FOR START();
        setUpCamera(620,860);
        openCamera();

        runApp();

    }

    private void startWithPreview(){
        shouldShowPreview = true;

        // Initialize view drawn over other apps
        initOverlay();
        System.out.println(TAG + ": startWithPreview() ---------------");

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
                .setSmallIcon(R.drawable.notification_template_icon_bg)
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
                    StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                    previewSize = chooseOptimalSize(streamConfigurationMap.getOutputSizes(SurfaceTexture.class), width, height);
                    this.cameraId = cameraId;
                    //previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];

                    //---(START)SETUP UP ORIENTATION;

                    //---(END)SETUP UP ORIENTATION;

                }
                //TODO - ROTATION: START https://www.youtube.com/watch?v=z3LAbtDh1VE
                //int deviceOrientation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
                // TODO: END
            }

            imageReader = ImageReader.newInstance(
                    previewSize.getWidth(),
                    previewSize.getHeight(),
                    ImageFormat.JPEG,
                    2);

            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    backgroundHandler.post(new ImageSaver(reader.acquireLatestImage()));
                }
            }, backgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);
            }
        } catch (CameraAccessException e) {
            Log.i(TAG,":MissingPermission - CAMERA");
            e.printStackTrace();
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

    private static class ImageSaver implements Runnable{
        private Image image;

        public ImageSaver(Image image) {
            this.image = image;
        }

        @Override
        public void run() {

            FileOutputStream outputPhoto = null;
            if (image == null)
                return;
            try {
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] ImageBytes = new byte[buffer.capacity()];
                buffer.get(ImageBytes);

                final Bitmap bmp= BitmapFactory.decodeByteArray(ImageBytes,0,ImageBytes.length);

                FileManager fileManager = FileManager.getInstance();
                fileManager.setCurrentTakenPhotoFile(fileManager.createImageFile());

                outputPhoto = new FileOutputStream(fileManager.getCurrentTakenPhotoFile());

                bmp.compress(Bitmap.CompressFormat.PNG,100,outputPhoto);
                image.close();


            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (image != null) {

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

    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {


        private void process(CaptureResult result) throws CameraAccessException {
            switch (mState) {
                case STATE_PREVIEW:
                    //NOTHINK
                    break;
                case STATE__WAIT_LOCK:
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED) {
                        //unLockFocus();
                        captureStillImage();
                        System.out.println("FOCUSE LOCK SUCCESFULY");
                    }
                    break;
            }
        }

        @Override
        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);

            try {
                process(result);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            System.out.println("FOCUSE LOCK FAILED!");
        }
    };

    public void takePhoto() throws CameraAccessException {
        lockFocus();
    }

    private void lockFocus() throws CameraAccessException {
        mState = STATE__WAIT_LOCK;
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_START);

        cameraCaptureSession.capture(captureRequestBuilder.build(), mSessionCaptureCallback,backgroundHandler);
    }

    //TODO:25.11.2019--PROBLEM JEST W TYM ZE IMAGERADER CIAGLE POBIERA ZDJECIA!! NIE CZEKA NA AKCJE
    private void unLockFocus() throws CameraAccessException {
        mState = STATE_PREVIEW;
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);

        cameraCaptureSession.capture(captureRequestBuilder.build(), mSessionCaptureCallback,null);
    }


    private void createPreviewSession() {
        try {
            List<Surface> targetSurfaces = new ArrayList<Surface>();

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            if(shouldShowPreview) {
                SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
                surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
                Surface previewSurface = new Surface(surfaceTexture);
                targetSurfaces.add(previewSurface);
                captureRequestBuilder.addTarget(previewSurface);
                //captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            }else {
                captureRequestBuilder.addTarget(imageReader.getSurface());
                targetSurfaces.add(imageReader.getSurface());
            }

            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(2));

            cameraDevice.createCaptureSession(targetSurfaces,
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            if (cameraDevice == null) {
                                return;
                            }
                            try {
                                CaptureRequest captureRequest = captureRequestBuilder.build();
                                CameraService.this.cameraCaptureSession = cameraCaptureSession;
                                CameraService.this.cameraCaptureSession.setRepeatingRequest(captureRequest /*mPreviewCaptureRequest*/,
                                        mSessionCaptureCallback, backgroundHandler);
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

    private void captureStillImage() throws CameraAccessException {
        CaptureRequest.Builder captureStillBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        captureStillBuilder.addTarget(imageReader.getSurface());
        captureStillBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(2));

        CameraCaptureSession.CaptureCallback captureCallback =
                new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                        System.out.println("IMAGE CAPTURED!");
                        try {
                            unLockFocus();
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                };
        cameraCaptureSession.capture(captureStillBuilder.build(), captureCallback,null);
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

    public void runApp() {
        if (!onOff) { //ON

            //---(START) CHECK INTERNET CONNECTION
            //Class for checking network conectivity.
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
            //---(END) CHECK INTERNET CONNECTION

            onOff = true;
            currentPictureID = 0;
            executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleAtFixedRate(periodicTask, 0, pictureDelay + 3, TimeUnit.SECONDS);

        } else { //OFF
            onOff = false;
            executor.shutdownNow();

            //Operations to end counting proccess.
            currentPictureID++;
            decrementTimer(-1);
            //Stoping handler postDelayed.
            handler.removeCallbacksAndMessages(null);
        }
    }

    public void decrementTimer(final int pictureID) {
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
            //if (pictureTimer<3) playTimerBeep();
        }
    }

    Runnable periodicTask = new Runnable() {
        public void run() {
            // Invoke method(s) to do the work
            savePicture();
        }
    };

    public void savePicture() {
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

    public void savePictureNow() {
        try {
            takePhoto();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        // ActionMenu cameraPreviewFragment = new ActionMenu();
        // cameraPreviewFragment.onTakePhoneButtonClicked();
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
