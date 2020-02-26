package com.example.damian.monitorapp.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler;
import com.example.damian.monitorapp.R;
import com.example.damian.monitorapp.utils.AppHelper;
import com.example.damian.monitorapp.utils.BusyIndicator;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RegisterConfirmation extends AppCompatActivity {

    private static final String TAG = "RegisterConfirmation";
    @Bind(R.id.confirmationButton) Button confirmationButton;
    private CognitoUser cognitoUser;
    private TextView confirmTextView;
    private String  username;

    private BusyIndicator busyIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_confirmation);
        ButterKnife.bind(this);

        confirmTextView = findViewById(R.id.registerConfrimEditText);
        username = getIntent().getStringExtra("username");

        busyIndicator = new BusyIndicator(this);
    }


    @OnClick(R.id.confirmationButton)
    public void confrimAccount(){
        busyIndicator.dimBackground();
        //TODO Walidacja
        cognitoUser = AppHelper.getPool().getUser(username);

        cognitoUser.confirmSignUpInBackground(confirmTextView.getText().toString(),false, confirmationCallback);
    }

    GenericHandler confirmationCallback = new GenericHandler() {
        @Override
        public void onSuccess() {
            Log.i(TAG, "confirmation user successfuly");
            Toast.makeText(RegisterConfirmation.this, "Successful confirmation!", Toast.LENGTH_SHORT).show();


            Intent intent = new Intent(RegisterConfirmation.this, SourcePhotoActivity.class);
            //Intent intent = new Intent(RegisterConfirmation.this, LoginActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);

            // User was successfully confirmed
            busyIndicator.unDimBackgorund();
        }
        @Override
        public void onFailure(Exception exception) {
            Log.i(TAG, "confirmation user failed:" + exception.getLocalizedMessage());
            Toast.makeText(RegisterConfirmation.this, "Failed to confirmed!", Toast.LENGTH_SHORT).show();
            // User confirmation failed. Check exception for the cause.
            busyIndicator.unDimBackgorund();
        }
    };
}
