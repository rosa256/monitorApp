package com.example.damian.monitorapp.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.example.damian.monitorapp.R;
import com.example.damian.monitorapp.utils.AppHelper;

public class SplashActivity extends Activity {

    private static final String TAG = "SplashActivity";
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        AppHelper.init(getApplicationContext());
        findCurrent();
    }

    private void findCurrent() {
        Log.e(TAG, "findCurrent: Invoked" );
        CognitoUser user = AppHelper.getPool().getCurrentUser();
        username = user.getUserId();
        Log.e(TAG, "findCurrent: username: "+ username );
        if(username != null) {
            AppHelper.setUser(username);
            user.getSessionInBackground(authenticationHandler);
        }else{
            Intent loginIntent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
        }
    }

    AuthenticationHandler authenticationHandler = new AuthenticationHandler() {
        @Override
        public void onSuccess(CognitoUserSession cognitoUserSession, CognitoDevice device) {
            Log.i(TAG, "onSuccess(): Invoked" );
            AppHelper.setCurrSession(cognitoUserSession);
            AppHelper.newDevice(device);
            goToMainActivity();
            finish();
        }

        @Override
        public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String username) {
            Log.i(TAG, "getAuthenticationDetails(): Invoked - Starting Login activity" );

            Intent loginIntent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
        }

        @Override
        public void getMFACode(MultiFactorAuthenticationContinuation multiFactorAuthenticationContinuation) {
            Log.i(TAG, "getMFACode(): Invoked" );
        }

        @Override
        public void authenticationChallenge(ChallengeContinuation continuation) { }

        @Override
        public void onFailure(Exception e) {
            Log.i(TAG, "onFailure(): Invoked - Starting Login activity" );
            Intent loginIntent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
        }
    };


    private void goToMainActivity() {
        Intent mainActivity = new Intent(this, MainActivity.class);
        mainActivity.putExtra("name", username);
        startActivityForResult(mainActivity, 4);
    }

    @Override
    protected void onPause() {
        finish();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
