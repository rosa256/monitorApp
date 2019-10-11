package com.example.damian.monitorapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.example.damian.monitorapp.Utils.ClientAWSFactory;
import com.example.damian.monitorapp.Utils.Constants;
import com.example.damian.monitorapp.Utils.CustomPrivileges;
import com.example.damian.monitorapp.Utils.FileManager;
import com.example.damian.monitorapp.requester.RekognitionRequester;
import com.michaldrabik.tapbarmenulib.TapBarMenu;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private CognitoCachingCredentialsProvider credentialsProvider;
    private TextureView.SurfaceTextureListener surfaceTextureListener;
    private FileManager fileManager;

    private Toolbar toolbar;
    private HandlerThread backgroundThread;
    private CameraManager cameraManager;
    private Handler backgroundHandler;
    private RekognitionRequester rekognitionRequester;

    private File currentTakenPhotoFile;
    private int cameraFacing;
    private String cameraId;

    private CameraDevice.StateCallback stateCallback;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size previewSize;
    private CameraCaptureSession cameraCaptureSession;
    private TextureView textureView;

    AmazonRekognitionClient rekognitionClient;

    @Bind(R.id.tapBarMenu)
    TapBarMenu tapBarMenu;
    private FileInputStream sourceFileInputStream;
    private String awsServiceOption = Constants.AWS_DETECT_FACES;

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        ClientAWSFactory awsFactory = new ClientAWSFactory();

        rekognitionClient = (AmazonRekognitionClient) awsFactory.createRekognitionClient();
        Log.i(TAG,"BBBBBBBBBB "+rekognitionClient.getRegions().getName());
        rekognitionRequester = new RekognitionRequester();

        //cameraSurfaceView = findViewById(R.id.cameraTextureView);

        CustomPrivileges.setUpPrivileges(this);

        fileManager = FileManager.getInstance();

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        cameraFacing = CameraCharacteristics.LENS_FACING_FRONT;
        textureView = findViewById(R.id.texture_view);

        surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                setUpCamera();
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
                MainActivity.this.cameraDevice = cameraDevice;
                createPreviewSession();
            }


            @Override
            public void onDisconnected(CameraDevice cameraDevice) {
                cameraDevice.close();
                MainActivity.this.cameraDevice = null;
            }

            @Override
            public void onError(CameraDevice cameraDevice, int error) {
                cameraDevice.close();
                MainActivity.this.cameraDevice = null;
            }
        };

        ButterKnife.bind(this);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }



    @OnClick(R.id.fab_take_photo)
    public void onTakePhoneButtonClicked() {
        fileManager.initFileManager(this.getResources());
        //lock();

        FileOutputStream outputPhoto = null;
        try {
            currentTakenPhotoFile= fileManager.createImageFile();
            outputPhoto = new FileOutputStream(currentTakenPhotoFile);
            //sourceFileInputStream = new FileInputStream(createImageFile(galleryFolder));

            textureView.getBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, outputPhoto);
            Toast.makeText(this,"Picture Saved" ,Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //unlock();
            try {
                if (outputPhoto != null) {
                    outputPhoto.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @OnClick(R.id.fab_send_photo_aws)
    public void onSendPhotoToAWS() {
        fileManager.initFileManager(this.getResources());


        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                    rekognitionRequester.doAwsService(rekognitionClient,currentTakenPhotoFile, awsServiceOption);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }


    @OnClick(R.id.fab_create_source_photo)

    public void onCreateSourcePhoto() {
        fileManager.initFileManager(this.getResources());
        //lock();

        FileOutputStream outputPhoto = null;
        try {
            currentTakenPhotoFile = fileManager.createSourceImageFile();
            outputPhoto = new FileOutputStream(currentTakenPhotoFile);
            //sourceFileInputStream = new FileInputStream(createImageFile(galleryFolder));

            textureView.getBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, outputPhoto);
            Toast.makeText(this,"Picture Source Saved" ,Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //unlock();
            try {
                if (outputPhoto != null) {
                    outputPhoto.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void lock() {
        try {
            cameraCaptureSession.capture(captureRequestBuilder.build(),
                    null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlock() {
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(),
                    null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);

            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            if (cameraDevice == null) {
                                return;
                            }
                            try {
                                CaptureRequest captureRequest = captureRequestBuilder.build();
                                MainActivity.this.cameraCaptureSession = cameraCaptureSession;
                                MainActivity.this.cameraCaptureSession.setRepeatingRequest(captureRequest,
                                        null, backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {

                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCamera() {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics =
                        cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        cameraFacing) {
                    StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
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
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);
            }
        } catch (CameraAccessException e) {
            Log.i(TAG,":MissingPermission - CAMERA");
            e.printStackTrace();
        }
    }

    private void openBackgroundThread() {
        backgroundThread = new HandlerThread("camera_background_thread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }



    @OnClick(R.id.tapBarMenu)
    public void onMenuButtonClick() {
        tapBarMenu.toggle();
    }

    public void aboutMe(View view){
        Intent intent = new Intent(this, AbouteMe.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.registerItem:
                Toast.makeText(this,"Register",Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, RegisterActivity.class);
                startActivity(intent);
                return true;

            case R.id.loginItem:
                Toast.makeText(this,"Login",Toast.LENGTH_SHORT).show();
                return true;

            case R.id.detectFaces:
                Toast.makeText(this,"Detect Faces Set",Toast.LENGTH_SHORT).show();
                awsServiceOption = Constants.AWS_DETECT_FACES;
                return true;

            case R.id.compareFaces:
                Toast.makeText(this,"Compare Faces Set",Toast.LENGTH_SHORT).show();
                awsServiceOption = Constants.AWS_COMPARE_FACES;
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
        return true;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        openBackgroundThread();
        if (textureView.isAvailable()) {
            setUpCamera();
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeCamera();
        closeBackgroundThread();
    }

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }


    private void closeBackgroundThread() {
        if (backgroundHandler != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }
}
