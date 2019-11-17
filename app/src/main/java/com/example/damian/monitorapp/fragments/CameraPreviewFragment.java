package com.example.damian.monitorapp.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.damian.monitorapp.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CameraPreviewFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CameraPreviewFragment#} factory method to
 * create an instance of this fragment.
 */
public class CameraPreviewFragment extends Fragment {

    private static final String TAG = "CameraPreviewFragment";

    private int cameraFacing;
    private Handler backgroundHandler;
    private CameraDevice.StateCallback stateCallback;
    private CameraDevice cameraDevice;
    private CameraManager cameraManager;
    private HandlerThread backgroundThread;
    private TextureView.SurfaceTextureListener surfaceTextureListener;
    private CameraCaptureSession cameraCaptureSession;
    private OnFragmentInteractionListener mListener;


    private CaptureRequest.Builder captureRequestBuilder;
    private Size previewSize;

    private TextureView textureView;
    private ImageView imageViewSource;
    private ImageView imageViewTarget;

    private String cameraId;

    public CameraPreviewFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera_preview, container, false);

        cameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        cameraFacing = CameraCharacteristics.LENS_FACING_FRONT;
        textureView = view.findViewById(R.id.texture_view);
        imageViewSource = view.findViewById(R.id.image_view_source);
        imageViewTarget = view.findViewById(R.id.image_view_target);

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
                CameraPreviewFragment.this.cameraDevice = cameraDevice;
                createPreviewSession();
            }
            @Override
            public void onDisconnected(CameraDevice cameraDevice) {
                cameraDevice.close();
                CameraPreviewFragment.this.cameraDevice = null;
            }
            @Override
            public void onError(CameraDevice cameraDevice, int error) {
                cameraDevice.close();
                CameraPreviewFragment.this.cameraDevice = null;
            }
        };

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
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
                                CameraPreviewFragment.this.cameraCaptureSession = cameraCaptureSession;
                                CameraPreviewFragment.this.cameraCaptureSession.setRepeatingRequest(captureRequest,
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
                int deviceOrientation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
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
            if (ActivityCompat.checkSelfPermission(requireContext().getApplicationContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);
            }
        } catch (CameraAccessException e) {
            Log.i(TAG,":MissingPermission - CAMERA");
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
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
    public void onStop() {
        super.onStop();
        closeCamera();
        closeBackgroundThread();
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

    public static class CompareSizeByArea implements Comparator<Size> {

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

    public TextureView getTextureView() {
        return textureView;
    }
    public ImageView getImageViewSource() {
        return imageViewSource;
    }

    public ImageView getImageViewTarget() {
        return imageViewTarget;
    }

}


