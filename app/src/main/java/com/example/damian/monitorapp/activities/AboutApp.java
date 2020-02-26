package com.example.damian.monitorapp.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.example.damian.monitorapp.R;

import butterknife.ButterKnife;

public class AboutApp extends AppCompatActivity {

    public static final String TAG = "AboutApp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, ":onCreate");
        setContentView(R.layout.activity_aboute_app);
        ButterKnife.bind(this);
    }
}

