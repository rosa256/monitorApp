package com.example.damian.monitorapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.net.Uri;
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
import android.widget.TextView;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.example.damian.monitorapp.Utils.ClientAWSFactory;
import com.example.damian.monitorapp.Utils.CognitoSettings;
import com.example.damian.monitorapp.Utils.Constants;
import com.example.damian.monitorapp.Utils.CustomPrivileges;
import com.example.damian.monitorapp.Utils.FileManager;
import com.example.damian.monitorapp.requester.RekognitionRequester;
import com.google.gson.Gson;
import com.michaldrabik.tapbarmenulib.TapBarMenu;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int RESULT_LOAD_IMG = 1;
    private CognitoCachingCredentialsProvider credentialsProvider;
    private TextureView.SurfaceTextureListener surfaceTextureListener;
    private FileManager fileManager;
    private Bitmap selectedImage;

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
    private TextView usernameEditText;
    private CognitoSettings cognitoSettings;

    private AmazonRekognitionClient rekognitionClient;

    @Bind(R.id.tapBarMenu)
    TapBarMenu tapBarMenu;
    private FileInputStream sourceFileInputStream;
    private String awsServiceOption = Constants.AWS_DETECT_FACES;

    public MainActivity() { }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        cognitoSettings = CognitoSettings.getInstance();
        cognitoSettings.initContext(MainActivity.this);

        ClientAWSFactory clientAWSFactory = new ClientAWSFactory();

        rekognitionClient = (AmazonRekognitionClient) clientAWSFactory.createRekognitionClient(getApplicationContext());
//        rekognitionRequester = new RekognitionRequester();

        //cameraSurfaceView = findViewById(R.id.cameraTextureView);

        CustomPrivileges.setUpPrivileges(this);

        fileManager = FileManager.getInstance();

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        cameraFacing = CameraCharacteristics.LENS_FACING_FRONT;
        textureView = findViewById(R.id.texture_view);

        usernameEditText = findViewById(R.id.usernameEditText);
        usernameEditText.setText(cognitoSettings.getUserPool().getCurrentUser().getUserId());


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

    @OnClick(R.id.fab_select_photo)
    public void onSelectPhotoButtonClicked() {
        pickImage();
    }

    public void pickImage() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG);
    }

    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        FileOutputStream outputPhoto = null;
        if (resultCode == RESULT_OK) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                selectedImage = BitmapFactory.decodeStream(imageStream);

                try {
                    currentTakenPhotoFile= fileManager.createSelectedImageFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                outputPhoto = new FileOutputStream(currentTakenPhotoFile);

                selectedImage.compress(Bitmap.CompressFormat.PNG, 100,outputPhoto);


            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
            }

        }else {
            Toast.makeText(MainActivity.this, "You haven't picked Image",Toast.LENGTH_LONG).show();
        }
    }


    @OnClick(R.id.fab_send_photo_aws)
    public void onSendPhotoToAWS() {
        fileManager.initFileManager(this.getResources());


        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                    new RekognitionRequester().doAwsService(rekognitionClient,currentTakenPhotoFile, awsServiceOption, getApplicationContext());
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
                }
                //TODO - ROTATION: START https://www.youtube.com/watch?v=z3LAbtDh1VE
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                // TODO: END


                //previewSize = chooseOptimalSize(streamConfigurationMap.getOutputSizes(SurfaceTexture.class), width, height);

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
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
        Intent intent;
        switch (item.getItemId()) {
            case R.id.registerItem:
                Toast.makeText(this,"Register",Toast.LENGTH_SHORT).show();
                intent = new Intent(this, RegisterActivity.class);
                startActivity(intent);
                return true;

            case R.id.loginItem:
                Toast.makeText(this,"Login",Toast.LENGTH_SHORT).show();
                intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                return true;

            case R.id.detectFaces:
                Toast.makeText(this,"Detect Faces Set",Toast.LENGTH_SHORT).show();
                awsServiceOption = Constants.AWS_DETECT_FACES;
                return true;

            case R.id.compareFaces:
                Toast.makeText(this,"Compare Faces Set",Toast.LENGTH_SHORT).show();
                awsServiceOption = Constants.AWS_COMPARE_FACES;
                return true;
            case R.id.logoutItem:
                Toast.makeText(this,"Logout",Toast.LENGTH_SHORT).show();

                intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
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
            setUpCamera(textureView.getWidth(), textureView.getHeight());
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

    public static class CompareSizeByArea implements Comparator<Size>{

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() /
                    (long) rhs.getWidth() * rhs .getHeight());
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
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else{
            return choices[0];
        }
    }
}
