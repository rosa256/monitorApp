package com.example.damian.monitorapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class AbouteMe extends AppCompatActivity {

    private Button doActionButton;
    public static final String TAG = "AbouteMe";

    //private CircleImageView circleImageView;  // imageview
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, ":onCreate");
        setContentView(R.layout.activity_aboute_me);
        doActionButton = (Button) findViewById(R.id.doActionButton);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.doActionButton)
    public void doActionButton(){
        Intent intent = new Intent(this, SourcePhotoActivity.class);
        startActivity(intent);
    }
}

