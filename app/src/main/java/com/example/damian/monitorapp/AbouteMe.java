package com.example.damian.monitorapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class AbouteMe extends AppCompatActivity {

    public static final String TAG = "AbouteMe";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,":onCreate");
        setContentView(R.layout.activity_aboute_me);
    }
}
