package com.example.damian.monitorapp;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.damian.monitorapp.Utils.CustomPrivileges;
import com.example.damian.monitorapp.Utils.FileManager;
import com.example.damian.monitorapp.fragments.CameraPreviewFragment;

import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class SourcePhotoActivity extends AppCompatActivity implements CameraPreviewFragment.OnFragmentInteractionListener {
    private static final String TAG = "SourcePhotoActivity";
    private FileManager fileManager;
    private CameraPreviewFragment cameraPreviewFragment;
    private TextureView textureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_source_photo);

        CustomPrivileges.setUpPrivileges(this);

        fileManager = FileManager.getInstance();

        ButterKnife.bind(this);

    }

    @OnClick(R.id.fab_create_source_photo)
    public void onCreateSourcePhoto() {
        fileManager.initFileManager(this.getResources());

        cameraPreviewFragment = (CameraPreviewFragment) getSupportFragmentManager().findFragmentById(R.id.cameraPreviewFragment);
        textureView = cameraPreviewFragment.getTextureView();
        //lock();

        FileOutputStream outputPhoto = null;
        try {
            //TODO: Do Poprawienia setCurrentTakenPhotoFile...
            fileManager.setCurrentTakenPhotoFile(fileManager.createSourceImageFile());
            outputPhoto = new FileOutputStream(fileManager.getCurrentTakenPhotoFile());
            textureView.getBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, outputPhoto);
            Toast.makeText(SourcePhotoActivity.this,"Picture Source Saved" ,Toast.LENGTH_LONG).show();

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

    @Override
    public void onFragmentInteraction(Uri uri) { }
}
