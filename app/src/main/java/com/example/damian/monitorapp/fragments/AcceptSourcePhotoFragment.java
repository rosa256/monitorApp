package com.example.damian.monitorapp.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.damian.monitorapp.LoginActivity;
import com.example.damian.monitorapp.R;
import com.example.damian.monitorapp.SourcePhotoActivity;
import com.example.damian.monitorapp.Utils.FileManager;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.OnClick;


public class AcceptSourcePhotoFragment extends Fragment {
    FileManager fileManager;
    ImageView myImage;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, @NonNull Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_accept_source_photo, container, false);

        fileManager = FileManager.getInstance();
        myImage = (ImageView) view.findViewById(R.id.source_photo_image_view);
        ButterKnife.bind(this, view);
        return view;
    }

    @OnClick
    (R.id.fab_accept_source_photo)
    public void acceptPhoto(){
        //TODO: Zrobić logowanie po akceptacji zdjęcia. o ile rejestracja sama tego już nie robi. :)
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
    }

    @OnClick
    (R.id.fab_discard_source_photo)
    public void discardPhoto(){
        ((SourcePhotoActivity)getActivity()).setViewPager(0);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            File imgFileToShow = fileManager.getSourcePhotoFile();
            if(imgFileToShow.exists()){
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFileToShow.getAbsolutePath());
                myImage.setImageBitmap(myBitmap);
            }
        }
        else {
        //TODO:Zrobić obsługę
        }
    }
}
