package com.example.damian.monitorapp;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.damian.monitorapp.Utils.CustomPrivileges;
import com.example.damian.monitorapp.Utils.FileManager;
import com.example.damian.monitorapp.adapters.SourceStatePagerAdapter;
import com.example.damian.monitorapp.fragments.AcceptSourcePhotoFragment;
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

    private SourceStatePagerAdapter sourceStatePagerAdapter;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_source_photo);

        CustomPrivileges.setUpPrivileges(this);
        fileManager = FileManager.getInstance();

        sourceStatePagerAdapter = new SourceStatePagerAdapter(getSupportFragmentManager());
        viewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager();

        ButterKnife.bind(this);

    }

    private void setupViewPager(){
      sourceStatePagerAdapter.addFragment(new CameraPreviewFragment());
      sourceStatePagerAdapter.addFragment(new AcceptSourcePhotoFragment());
      viewPager.setAdapter(sourceStatePagerAdapter);
    }

    public void setViewPager(int fragmentNumber){
        viewPager.setCurrentItem(fragmentNumber);
    }

    @OnClick(R.id.fab_create_source_photo)
    public void onCreateSourcePhoto() {
        fileManager.initFileManager(this.getResources());
        cameraPreviewFragment = (CameraPreviewFragment) sourceStatePagerAdapter.getItem(0);
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
            setViewPager(1);

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
