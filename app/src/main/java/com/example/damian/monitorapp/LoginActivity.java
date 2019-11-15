package com.example.damian.monitorapp;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentityClient;
import com.example.damian.monitorapp.Utils.CognitoSettings;
import com.example.damian.monitorapp.Utils.Constants;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private CognitoUser cognitoUser;
    @Bind(R.id.loginButton) Button loginButton;
    @Bind(R.id.goToRegistrationButton) Button registrationButton;
    private EditText password;
    private EditText username;
    private CognitoSettings cognitoSettings;
    private CognitoUserSession userSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);


        password = findViewById(R.id.inputLoginPassword);
        password.setText("ABCabc!@#");
        username = findViewById(R.id.inputLoginUsername);
        username.setText("maniek256");
        cognitoSettings = CognitoSettings.getInstance();
        cognitoSettings.initContext(LoginActivity.this);

        if(checkCacheCredentials()){
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "invoke onResume()");
        if(userSession != null){
        if(!userSession.isValid()) {
            Intent intent = new Intent(this,LoginActivity.class);
            startActivity(intent);
            }
        }
    }


    @OnClick(R.id.goToRegistrationButton)
    public void goToRegister(){
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.loginButton)
    public void SignInUser(){
        cognitoUser = cognitoSettings.getUserPool().getUser(username.getText().toString());
        System.out.println(cognitoSettings.getUserPool().getCurrentUser());
        //Invoke Sign In process.
        cognitoUser.getSessionInBackground(authenticationHandler);
    }

    // Callback handler for the sign-in process
    AuthenticationHandler authenticationHandler = new AuthenticationHandler() {
        @Override
        public void onSuccess(CognitoUserSession cognitoUserSession, CognitoDevice newDevice) {
            userSession = cognitoUserSession;
            Log.i(TAG, "Sign-in user: " + cognitoUserSession.getUsername());


            //https://aws-amplify.github.io/aws-sdk-android/docs/reference/com/amazonaws/auth/CognitoCachingCredentialsProvider.html
            //https://docs.aws.amazon.com/cognito/latest/developerguide/amazon-cognito-integrating-user-pools-with-identity-pools.html
            Map<String, String> logins = new HashMap<String, String>();
            logins.put("cognito-idp."+ Constants.COGNITO_REGION +".amazonaws.com/" + Constants.USER_POOL_ID, cognitoUserSession.getIdToken().getJWTToken());
            cognitoSettings.getCredentialsProvider().setLogins(logins);
            AmazonCognitoIdentityClient amazonCognitoIdentityClient = new AmazonCognitoIdentityClient(cognitoSettings.getCredentialsProvider());


            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("userSession", new Gson().toJson(cognitoUserSession));
            startActivity(intent);
        }

        @Override
        public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String userId) {
            // The API needs user sign-in credentials to continue
            AuthenticationDetails authenticationDetails = new AuthenticationDetails(userId, password.getText().toString(), null);
            // Pass the user sign-in credentials to the continuation
            authenticationContinuation.setAuthenticationDetails(authenticationDetails);
            // Allow the sign-in to continue
            authenticationContinuation.continueTask();
        }

        @Override
        public void getMFACode(MultiFactorAuthenticationContinuation continuation) { }
        @Override
        public void authenticationChallenge(ChallengeContinuation continuation) { }
        @Override
        public void onFailure(Exception exception) {
            Log.i(TAG, "User faild to Sign-in: " + exception.getLocalizedMessage());
            Toast.makeText(LoginActivity.this,"User does not exist",Toast.LENGTH_SHORT).show();
        }
    };


    private boolean checkCacheCredentials() {
        CognitoUser currentUser = cognitoSettings.getUserPool().getCurrentUser();

        currentUser.getSessionInBackground(new AuthenticationHandler() {
            @Override
            public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {

                if (userSession.isValid()) {
                    Log.i(TAG, "user session valid, getting token...");
                    // Get id token from CognitoUserSession.
                    String idToken = userSession.getIdToken().getJWTToken();

                    if (idToken.length() > 0) {
                        // Set up as a credentials provider.
                        Log.i(TAG, "got id token - setting credentials using token");
                        //new RefreshAsyncTask().execute(1);

                    } else {
                        Log.i(TAG, "no token...");
                    }
                } else {
                    Log.i(TAG, "user session not valid - using identity pool credentials - guest user");
                }
                //performAction(action);
            }

            @Override
            public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String userId) {
                Log.i(TAG, " Not logged in! using identity pool credentials for guest user");
                //performAction(action);
            }
            @Override
            public void getMFACode(MultiFactorAuthenticationContinuation continuation) { }
            @Override
            public void authenticationChallenge(ChallengeContinuation continuation) { }
            @Override
            public void onFailure(Exception exception) {
                Log.i(TAG, "error getting session: " + exception.getLocalizedMessage());
                Log.i(TAG, "Session expired, forwarded to Login Activity.");
                //proceed using guest user credentials
                //performAction(action);
            }
        });


        String identityId = cognitoSettings.getUserPool().getCurrentUser().getUserId();
        System.out.println(TAG +": ---------------- IdentityId: " + identityId);
        System.out.println(TAG +": ---------------- SessionDuration: " + cognitoSettings.getCredentialsProvider().getSessionDuration());

        if(identityId != null && !identityId.isEmpty())
            return true;
        return false;
    }

}

