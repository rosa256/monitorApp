package com.example.damian.monitorapp;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
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

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final String USER_DO_LOGOUT = "user_do_logout";
    @Bind(R.id.loginButton) Button loginButton;
    @Bind(R.id.goToRegistrationButton) Button registrationButton;
    private EditText passwordInput;
    private EditText usernameInput;
    private BusyIndicator busyIndicator;
    private Boolean shouldAllowBack;

    String username;
    String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate: Invoked");
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        busyIndicator = new BusyIndicator(this);

        CustomPrivileges.setUpPrivileges(this);
        initInputs();

        AppHelper.init(getApplicationContext());
        shouldAllowBack = true;

        boolean doLogout = readLogoutState();
        Log.i(TAG, "onCreate(): Read logout: " + doLogout);
        if(doLogout){
            shouldAllowBack = false;
            Log.i(TAG, "onCreate(): Signing out");
            writeLogoutState(false);
        }
        findCurrent();
    }
    private void findCurrent() {
        Log.e(TAG, "findCurrent: Invoked" );
        CognitoUser user = AppHelper.getPool().getCurrentUser();
        username = user.getUserId();
        Log.e(TAG, "findCurrent: username: "+ username );
        if(username != null) {
            AppHelper.setUser(username);
            usernameInput.setText(user.getUserId());
            user.getSessionInBackground(authenticationHandler);
        }
    }

    private void initInputs() {
        usernameInput = findViewById(R.id.inputLoginUsername);
        usernameInput.setText("Maniek255");
        passwordInput = findViewById(R.id.inputLoginPassword);
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
        Log.e(TAG, "SignInUser: Invoked" );
        username = usernameInput.getText().toString();
        if(username == null || username.length() < 1) {
            usernameInput.setError("Incorrect username!\n*Cannot be empty!");
            return;
        }

        AppHelper.setUser(username);

        password = passwordInput.getText().toString();
        if(password == null || password.length() < 1) {
            passwordInput.setError("Incorrect password!\n*Cannot be empty!");
            return;
        }
        busyIndicator.dimBackground();

        showWaitDialog("Signing in...");
        AppHelper.getPool().getUser(username).getSessionInBackground(authenticationHandler);
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
            shouldAllowBack = true;
            Log.i(TAG, "onSuccess(): Invoked" );
            AppHelper.setCurrSession(cognitoUserSession);
            AppHelper.newDevice(device);
            goToMainActivity();
            clearInput();
            finish();
            busyIndicator.unDimBackgorund();
        }

        @Override
        public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String username) {
            Log.i(TAG, "getAuthenticationDetails(): Invoked" );
            getUserAuthentication(authenticationContinuation, username);
        }

        @Override
        public void getMFACode(MultiFactorAuthenticationContinuation multiFactorAuthenticationContinuation) {
            Log.i(TAG, "getMFACode(): Invoked" );
            //mfaAuth(multiFactorAuthenticationContinuation);
        }

        @Override
        public void authenticationChallenge(ChallengeContinuation continuation) { }

        @Override
        public void onFailure(Exception e) {
            Log.i(TAG, "onFailure(): 4" );
            closeWaitDialog();
            busyIndicator.unDimBackgorund();

            new MaterialDialog.Builder(LoginActivity.this).title("Wrong Credentials")
                    .content("User does not exist.")
                    .theme(Theme.LIGHT)
                    .positiveColor(Color.GRAY)
                    .positiveText("ok")
                    .show();
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

    private void clearInput() {
        if(usernameInput== null) {
            usernameInput = findViewById(R.id.inputLoginUsername);
        }

        if(passwordInput== null) {
            passwordInput = findViewById(R.id.inputLoginPassword);
        }

        usernameInput.setText("");
        usernameInput.requestFocus();
        passwordInput.setText("");
    }

    private void writeLogoutState(boolean doLogout) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(USER_DO_LOGOUT, doLogout);
        editor.commit();
    }

    private boolean readLogoutState() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean doLogout = prefs.getBoolean(USER_DO_LOGOUT, true);
        Log.i(TAG, "readLogoutState(): Do Logout: " + doLogout);
        return doLogout;
    }
}

