package com.example.damian.monitorapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amazonaws.mobile.auth.core.DefaultSignInResultHandler;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.core.IdentityProvider;
import com.amazonaws.mobile.auth.core.StartupAuthErrorDetails;
import com.amazonaws.mobile.auth.core.StartupAuthResult;
import com.amazonaws.mobile.auth.core.StartupAuthResultHandler;
import com.amazonaws.mobile.auth.core.signin.AuthException;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler;
import com.example.damian.monitorapp.AWSChangable.activity.SignInActivity;
import com.example.damian.monitorapp.Utils.AppHelper;
import com.example.damian.monitorapp.Utils.CustomPrivileges;

import java.lang.ref.WeakReference;

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
