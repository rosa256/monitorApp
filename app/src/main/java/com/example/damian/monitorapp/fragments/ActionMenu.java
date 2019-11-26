package com.example.damian.monitorapp.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.damian.monitorapp.R;
import com.example.damian.monitorapp.Utils.FileManager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.app.Activity.RESULT_OK;

public class ActionMenu extends Fragment{

    private FileManager fileManager;
    private TextureView textureView;
    private static final int RESULT_LOAD_IMG = 1;
    private CameraPreviewFragment cameraPreviewFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_action_menu, container,false);
        fileManager = FileManager.getInstance();
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @OnClick(R.id.fab_take_photo)
    public void onTakePhoneButtonClicked() {

        cameraPreviewFragment = (CameraPreviewFragment) getFragmentManager().findFragmentById(R.id.cameraPreviewFragment);
        textureView = cameraPreviewFragment.getTextureView();

        fileManager.initFileManager(this.getResources());
        //lock();
        FileOutputStream outputPhoto = null;
        try {
            fileManager.setCurrentTakenPhotoFile(fileManager.createImageFile());
            outputPhoto = new FileOutputStream(fileManager.getCurrentTakenPhotoFile());

            textureView.getBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, outputPhoto);
            Toast.makeText(getActivity().getApplicationContext(),"Picture Saved" ,Toast.LENGTH_LONG).show();

            //--To Set Up Hint Views
            Bitmap myBitmap = BitmapFactory.decodeFile(fileManager.getCurrentTakenPhotoFile().getAbsolutePath());
            cameraPreviewFragment.getImageViewTarget().setImageBitmap(myBitmap);
            //--To Set Up Hint Views

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

    //TODO: Very slow working.. Have to check this.
    //TODO: Fix loading photo from gallery (gallery path not initalize(null)).
    @OnClick(R.id.fab_select_photo)
    public void onSelectPhotoButtonClicked() {
        pickImage();
    }

    public void pickImage() {
        cameraPreviewFragment = (CameraPreviewFragment) getFragmentManager().findFragmentById(R.id.cameraPreviewFragment);
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG);
    }


    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        FileOutputStream outputPhoto = null;
        if (resultCode == RESULT_OK) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getActivity().getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

                try {
                    fileManager.setCurrentTakenPhotoFile(fileManager.createSelectedImageFile());
                    //--To Set Up Hint Views
                    cameraPreviewFragment.getImageViewTarget().setImageBitmap(selectedImage);
                    //--To Set Up Hint Views

                } catch (IOException e) {
                    e.printStackTrace();
                }
                File tempFile = fileManager.createImageFile();
                outputPhoto = new FileOutputStream(tempFile);

                selectedImage.compress(Bitmap.CompressFormat.PNG, 60 , outputPhoto);

                fileManager.setCurrentTakenPhotoFile(tempFile);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(getActivity().getApplicationContext(), "Something went wrong", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }else {
            Toast.makeText(getActivity().getApplicationContext(), "You haven't picked Image",Toast.LENGTH_LONG).show();
        }
    }
}

//    E/CameraCaptureSession: Session 0: Exception while stopping repeating:
//    android.hardware.camera2.CameraAccessException: CAMERA_DISCONNECTED (2): cancelRequest:458: Camera device no longer alive