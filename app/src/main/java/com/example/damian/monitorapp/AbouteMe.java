package com.example.damian.monitorapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class AbouteMe extends AppCompatActivity {

    public static final String TAG = "AbouteMe";

    //private CircleImageView circleImageView;  // imageview
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, ":onCreate");
        setContentView(R.layout.activity_aboute_me);
        ButterKnife.bind(this);
    }
}

