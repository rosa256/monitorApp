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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
            File currentPhoto;
            currentPhoto = fileManager.createImageFile();
            System.out.println(currentPhoto.getAbsolutePath());
            outputPhoto = new FileOutputStream(currentPhoto);

            textureView.getBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, outputPhoto);
            Toast.makeText(getActivity().getApplicationContext(),"Picture Saved" ,Toast.LENGTH_LONG).show();

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

    @OnClick(R.id.fab_create_source_photo)
    public void onCreateSourcePhoto() {
        fileManager.initFileManager(this.getResources());
        //lock();

        FileOutputStream outputPhoto = null;
        try {
            //*currentTakenPhotoFile = fileManager.createSourceImageFile();
            //*outputPhoto = new FileOutputStream(currentTakenPhotoFile);
            //sourceFileInputStream = new FileInputStream(createImageFile(galleryFolder));

            //textureView.getBitmap()
            //        .compress(Bitmap.CompressFormat.PNG, 100, outputPhoto);
            //Toast.makeText(this,"Picture Source Saved" ,Toast.LENGTH_LONG).show();

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
    @OnClick(R.id.fab_select_photo)
    public void onSelectPhotoButtonClicked() {
        pickImage();
    }

    public void pickImage() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG);
    }

    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        FileOutputStream outputPhoto = null;
        if (resultCode == RESULT_OK) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getActivity().getContentResolver().openInputStream(imageUri);
                Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

                try {
                    fileManager.setCurrentTakenPhotoFile(fileManager.createSelectedImageFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                outputPhoto = new FileOutputStream(fileManager.getCurrentTakenPhotoFile());

                selectedImage.compress(Bitmap.CompressFormat.PNG, 100,outputPhoto);


            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(getActivity().getApplicationContext(), "Something went wrong", Toast.LENGTH_LONG).show();
            }

        }else {
            Toast.makeText(getActivity().getApplicationContext(), "You haven't picked Image",Toast.LENGTH_LONG).show();
        }
    }
}
