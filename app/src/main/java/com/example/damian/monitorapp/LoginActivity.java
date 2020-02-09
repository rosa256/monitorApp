package com.example.damian.monitorapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.auth.ui.AuthUIConfiguration;
import com.amazonaws.mobile.auth.ui.SignInUI;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.AWSStartupHandler;
import com.amazonaws.mobile.client.AWSStartupResult;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChooseMfaContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ForgotPasswordContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.NewPasswordContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.ForgotPasswordHandler;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentityClient;
import com.example.damian.monitorapp.Utils.AppHelper;
import com.example.damian.monitorapp.Utils.CognitoSettings;
import com.example.damian.monitorapp.Utils.Constants;
import com.example.damian.monitorapp.Utils.CustomPrivileges;
import com.example.damian.monitorapp.requester.RefreshAsyncTask;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private CognitoUser cognitoUser;
    @Bind(R.id.loginButton) Button loginButton;
    @Bind(R.id.goToRegistrationButton) Button registrationButton;
    private EditText passwordInput;
    private EditText usernameInput;
    private CognitoSettings cognitoSettings;
    private BusyIndicator busyIndicator;

    String username;
    String password;

    private AWSCredentialsProvider credentialsProvider;
    private AWSConfiguration configuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate: Invoked");
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        busyIndicator = new BusyIndicator(this);


        //cognitoSettings = CognitoSettings.getInstance();
        //cognitoSettings.initContext(LoginActivity.this);

        CustomPrivileges.setUpPrivileges(this);
        initInputs();

        AppHelper.init(getApplicationContext());

        findCurrent();
    }

    private void findCurrent() {
        Log.e(TAG, "findCurrent: Invoked" );
        CognitoUser user = AppHelper.getPool().getCurrentUser();
        username = user.getUserId();
        Log.e(TAG, "findCurrent: A" );

        if(username != null) {
        Log.e(TAG, "findCurrent: B" );
            AppHelper.setUser(username);
            usernameInput.setText(user.getUserId());
            user.getSessionInBackground(authenticationHandler);
        }
    }

    private void initInputs() {
        passwordInput = findViewById(R.id.inputLoginPassword);
        passwordInput.setText("");
        usernameInput = findViewById(R.id.inputLoginUsername);
        usernameInput.setText("maniek256");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "invoke onResume()");
    }


    @OnClick(R.id.goToRegistrationButton)
    public void goToRegister(){
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.loginButton)
    public void SignInUser(){
        //busyIndicator.dimBackground();
        Log.e(TAG, "SignInUser: Invoked" );
        username = usernameInput.getText().toString();
        if(username == null || username.length() < 1) {
            //TextView label = (TextView) findViewById(R.id.textViewUserIdMessage);
            //label.setText(usernameInput.getHint()+" cannot be empty");
            //return;
        }

        AppHelper.setUser(username);

        password = passwordInput.getText().toString();
        if(password == null || password.length() < 1) {
            //TextView label = (TextView) findViewById(R.id.textViewUserPasswordMessage);
            //label.setText(passwordInput.getHint()+" cannot be empty");
            //passwordInput.setBackground(getDrawable(R.drawable.text_border_error));
            //return;
        }

        showWaitDialog("Signing in...");
        AppHelper.getPool().getUser(username).getSessionInBackground(authenticationHandler);

       // busyIndicator.unDimBackgorund();
    }

    private void showWaitDialog(String message) {
        closeWaitDialog();
        //waitDialog = new ProgressDialog(this);
        //waitDialog.setTitle(message);
        //waitDialog.show();
    }
    private void closeWaitDialog() {
        try {
          //  waitDialog.dismiss();
        }
        catch (Exception e) {
            //
        }
    }

    // Callbacks
    ForgotPasswordHandler forgotPasswordHandler = new ForgotPasswordHandler() {
        @Override
        public void onSuccess() {
//            closeWaitDialog();
//            showDialogMessage("Password successfully changed!","");
//            inPassword.setText("");
//            inPassword.requestFocus();
        }

        @Override
        public void getResetCode(ForgotPasswordContinuation forgotPasswordContinuation) {
//            closeWaitDialog();
//            getForgotPasswordCode(forgotPasswordContinuation);
        }

        @Override
        public void onFailure(Exception e) {
//            closeWaitDialog();
//            showDialogMessage("Forgot password failed",AppHelper.formatException(e));
        }
    };

    //
    AuthenticationHandler authenticationHandler = new AuthenticationHandler() {
        @Override
        public void onSuccess(CognitoUserSession cognitoUserSession, CognitoDevice device) {
            Log.e(TAG, "onSuccess: 1" );
            Log.d(TAG, " -- Auth Success");
            AppHelper.setCurrSession(cognitoUserSession);
            AppHelper.newDevice(device);
            closeWaitDialog();
            goToMainActivity();
        }

        @Override
        public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String username) {
            Log.e(TAG, "onSuccess: 2" );
            closeWaitDialog();
            Locale.setDefault(Locale.US);
            getUserAuthentication(authenticationContinuation, username);
        }

        @Override
        public void getMFACode(MultiFactorAuthenticationContinuation multiFactorAuthenticationContinuation) {
            Log.e(TAG, "onSuccess: 3" );
            closeWaitDialog();
            //mfaAuth(multiFactorAuthenticationContinuation);
        }

        @Override
        public void authenticationChallenge(ChallengeContinuation continuation) { }

        @Override
        public void onFailure(Exception e) {
            Log.e(TAG, "onSuccess: 4" );
            closeWaitDialog();
            Toast.makeText(LoginActivity.this, "NIE UDALO SIE ZALOGOWAC", Toast.LENGTH_SHORT).show();
            //TextView label = (TextView) findViewById(R.id.textViewUserIdMessage);
            //label.setText("Sign-in failed");
            //inPassword.setBackground(getDrawable(R.drawable.text_border_error));

            //label = (TextView) findViewById(R.id.textViewUserIdMessage);
            //label.setText("Sign-in failed");
            //usernameInput.setBackground(getDrawable(R.drawable.text_border_error));

            //showDialogMessage("Sign-in failed", AppHelper.formatException(e));
        }
    };

    private void getUserAuthentication(AuthenticationContinuation continuation, String username) {
        if(username != null) {
            this.username = username;
            AppHelper.setUser(username);
        }
        if(this.password == null) {
            usernameInput.setText(username);
            password = passwordInput.getText().toString();
            if(password == null) {
                //TextView label = (TextView) findViewById(R.id.textViewUserPasswordMessage);
                //label.setText(passwordInput.getHint()+" enter password");
                //passwordInput.setBackground(getDrawable(R.drawable.text_border_error));
                return;
            }

            if(password.length() < 1) {
                //TextView label = (TextView) findViewById(R.id.textViewUserPasswordMessage);
                //label.setText(passwordInput.getHint()+" enter password");
                //passwordInput.setBackground(getDrawable(R.drawable.text_border_error));
                return;
            }
        }
        AuthenticationDetails authenticationDetails = new AuthenticationDetails(this.username, password, null);
        continuation.setAuthenticationDetails(authenticationDetails);
        continuation.continueTask();
    }

    private void goToMainActivity() {
        Intent mainActivity = new Intent(this, MainActivity.class);
        mainActivity.putExtra("name", username);
        startActivityForResult(mainActivity, 4);
    }
}

